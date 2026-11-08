package ph.karinderya.pos.ui.menu;

import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import ph.karinderya.pos.R;
import ph.karinderya.pos.data.model.MenuItem;
import ph.karinderya.pos.databinding.ActivityMenuItemEditBinding;
import ph.karinderya.pos.ui.common.AuthedActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;
import ph.karinderya.pos.util.ImageLoader;
import ph.karinderya.pos.util.ImageStore;
import ph.karinderya.pos.util.Money;

/**
 * Add / edit a dish. Newly picked photos are written to app storage immediately; they are kept on
 * save and deleted on discard, while the previous photo is only deleted after a successful save.
 */
public final class MenuItemEditActivity extends AuthedActivity {

    public static final String EXTRA_ITEM_ID = "item_id";
     static final String STATE_PHOTO = "photo";
     static final String STATE_CAMERA_FILE = "camera_file";
     static final String STATE_LOADED = "loaded";

     ActivityMenuItemEditBinding binding;
     ImageStore images;
     MenuItem original;
     UUID itemId;
     String photoPath;
     File cameraFile;
     boolean loaded;
     boolean saving;

     final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    importPhoto(uri, null);
                }
            });

     final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                File file = cameraFile;
                cameraFile = null;
                if (file == null) {
                    return;
                }
                if (Boolean.TRUE.equals(success) && file.length() > 0) {
                    importPhoto(cameraUri(file), file);
                } else {
                    file.delete();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!sessionValid) {
            return;
        }
        binding = ActivityMenuItemEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Ui.applyInsets(binding.root, binding.toolbar);
        images = app.images();

        String idExtra = getIntent().getStringExtra(EXTRA_ITEM_ID);
        itemId = idExtra == null ? null : UUID.fromString(idExtra);
        setupToolbar(binding.toolbar, getString(itemId == null ? R.string.add_menu_item : R.string.edit_menu_item),
                true);

        if (savedInstanceState != null) {
            photoPath = savedInstanceState.getString(STATE_PHOTO);
            String camera = savedInstanceState.getString(STATE_CAMERA_FILE);
            cameraFile = camera == null ? null : new File(camera);
            loaded = savedInstanceState.getBoolean(STATE_LOADED);
        }

        binding.galleryButton.setOnClickListener(v -> pickPhoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        binding.cameraButton.setOnClickListener(v -> launchCamera());
        binding.removePhotoButton.setOnClickListener(v -> setPhoto(null));
        binding.saveButton.setOnClickListener(v -> save());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmDiscard();
            }
        });
        load();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_PHOTO, photoPath);
        outState.putString(STATE_CAMERA_FILE, cameraFile == null ? null : cameraFile.getAbsolutePath());
        outState.putBoolean(STATE_LOADED, loaded);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (itemId != null) {
            getMenuInflater().inflate(R.menu.menu_edit_item, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

     void load() {
        Async.run(() -> new MenuEditorData(app.menu().categories(),
                itemId == null ? null : app.menu().findById(itemId).orElse(null)), data -> {
            List<String> categories = data.categories();
            binding.categoryInput.setSimpleItems(categories.toArray(new String[0]));
            original = data.item();
            if (itemId != null && original == null) {
                finish();
                return;
            }
            if (!loaded) {
                loaded = true;
                if (original != null) {
                    binding.nameInput.setText(original.name());
                    binding.categoryInput.setText(original.category(), false);
                    binding.priceInput.setText(Money.toPlain(original.priceCents()));
                    binding.descriptionInput.setText(original.description());
                    binding.availableSwitch.setChecked(original.available());
                    binding.mealOfDaySwitch.setChecked(original.mealOfDay());
                    photoPath = original.photoPath();
                } else if (!categories.isEmpty()) {
                    binding.categoryInput.setText(categories.get(0), false);
                }
            }
            renderPhoto();
        }, e -> Ui.showError(this, e));
    }

     void launchCamera() {
        cameraFile = images.newCameraFile();
        try {
            takePhoto.launch(cameraUri(cameraFile));
        } catch (ActivityNotFoundException e) {
            cameraFile = null;
            Ui.toast(this, getString(R.string.error_no_camera));
        }
    }

     Uri cameraUri(File file) {
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
    }

     void importPhoto(Uri uri, File tempToDelete) {
        binding.saveButton.setEnabled(false);
        Async.run(() -> images.importFromUri(uri), path -> {
            if (tempToDelete != null) {
                tempToDelete.delete();
            }
            binding.saveButton.setEnabled(true);
            setPhoto(path);
        }, e -> {
            if (tempToDelete != null) {
                tempToDelete.delete();
            }
            binding.saveButton.setEnabled(true);
            Ui.toast(this, getString(R.string.error_photo));
        });
    }

    /** Replaces the working photo, deleting the previous one if it was a not-yet-saved import. */
     void setPhoto(String newPath) {
        if (photoPath != null && !isOriginalPhoto(photoPath)) {
            images.delete(photoPath);
        }
        photoPath = newPath;
        renderPhoto();
    }

     boolean isOriginalPhoto(String path) {
        return original != null && Objects.equals(original.photoPath(), path);
    }

     void renderPhoto() {
        ImageLoader.load(binding.photo, photoPath);
        binding.removePhotoButton.setVisibility(photoPath == null ? View.GONE : View.VISIBLE);
    }

     void save() {
        String name = text(binding.nameInput.getText()).trim();
        String category = text(binding.categoryInput.getText()).trim();
        String required = getString(R.string.error_required);
        binding.nameLayout.setError(name.isEmpty() ? required : null);
        binding.categoryLayout.setError(category.isEmpty() ? required : null);
        long price;
        try {
            price = Money.parse(text(binding.priceInput.getText()));
            binding.priceLayout.setError(null);
        } catch (NumberFormatException | ArithmeticException e) {
            binding.priceLayout.setError(getString(R.string.error_price));
            return;
        }
        if (name.isEmpty() || category.isEmpty()) {
            return;
        }
        Instant createdAt = original == null ? Instant.now() : original.createdAt();
        MenuItem item = new MenuItem(
                original == null ? null : original.id(), name, text(binding.descriptionInput.getText()), category,
                price, photoPath, binding.availableSwitch.isChecked(), binding.mealOfDaySwitch.isChecked(),
                createdAt, createdAt);
        String oldPhoto = original == null ? null : original.photoPath();
        saving = true;
        binding.saveButton.setEnabled(false);
        Async.run(() -> app.menu().save(item), saved -> {
            if (oldPhoto != null && !oldPhoto.equals(saved.photoPath())) {
                images.delete(oldPhoto);
            }
            Ui.toast(this, getString(R.string.saved));
            finish();
        }, e -> {
            saving = false;
            binding.saveButton.setEnabled(true);
            Ui.showError(this, e);
        });
    }

     void confirmDelete() {
        if (original == null) {
            return;
        }
        Ui.confirm(this, getString(R.string.delete_item_confirm, original.name()), () -> {
            MenuItem toDelete = original;
            Async.run(() -> {
                app.menu().delete(toDelete.id());
                return true;
            }, ok -> {
                app.cart().remove(toDelete.id());
                images.delete(toDelete.photoPath());
                if (photoPath != null && !photoPath.equals(toDelete.photoPath())) {
                    images.delete(photoPath);
                }
                finish();
            }, e -> Ui.showError(this, e));
        });
    }

     boolean isDirty() {
        if (!loaded) {
            return false;
        }
        if (original == null) {
            return photoPath != null || !text(binding.nameInput.getText()).isBlank()
                    || !text(binding.priceInput.getText()).isBlank()
                    || !text(binding.descriptionInput.getText()).isBlank()
                    || binding.mealOfDaySwitch.isChecked();
        }
        return !Objects.equals(photoPath, original.photoPath())
                || !text(binding.nameInput.getText()).trim().equals(original.name())
                || !text(binding.categoryInput.getText()).trim().equals(original.category())
                || Money.parseOrZero(text(binding.priceInput.getText())) != original.priceCents()
                || !text(binding.descriptionInput.getText()).trim().equals(Objects.toString(original.description(), ""))
                || binding.availableSwitch.isChecked() != original.available()
                || binding.mealOfDaySwitch.isChecked() != original.mealOfDay();
    }

     void confirmDiscard() {
        if (saving) {
            return;
        }
        if (!isDirty()) {
            discardAndFinish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.discard_changes)
                .setPositiveButton(R.string.discard, (d, w) -> discardAndFinish())
                .setNegativeButton(R.string.keep_editing, null)
                .show();
    }

     void discardAndFinish() {
        if (photoPath != null && !isOriginalPhoto(photoPath)) {
            images.delete(photoPath);
        }
        finish();
    }

     static String text(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
