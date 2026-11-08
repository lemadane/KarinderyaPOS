package ph.karinderya.pos.ui.more;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import ph.karinderya.pos.KarinderyaApp;
import ph.karinderya.pos.R;
import ph.karinderya.pos.data.model.BackupData;
import ph.karinderya.pos.data.model.CustomDiscount;
import ph.karinderya.pos.data.model.User;
import ph.karinderya.pos.databinding.DialogTextInputBinding;
import ph.karinderya.pos.databinding.DialogDiscountBinding;
import ph.karinderya.pos.databinding.FragmentMoreBinding;
import ph.karinderya.pos.ui.MainActivity;
import ph.karinderya.pos.ui.auth.LoginActivity;
import ph.karinderya.pos.ui.common.Ui;
import ph.karinderya.pos.util.Async;

public final class MoreFragment extends Fragment {

     FragmentMoreBinding binding;
     KarinderyaApp app;
     AlertDialog discountAuthenticationDialog;

     final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) {
                    exportTo(uri);
                }
            });

     final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    Ui.confirm(requireContext(), getString(R.string.import_confirm), () -> importFrom(uri));
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        app = KarinderyaApp.get(requireContext());
        binding.salesReportButton.setOnClickListener(v -> startActivity(new Intent(requireContext(),
                SalesReportActivity.class)));
        binding.changePasswordButton.setOnClickListener(v -> startActivity(new Intent(requireContext(),
                ChangePasswordActivity.class)));
        binding.storeNameButton.setOnClickListener(v -> editStoreName());
        binding.discountsButton.setOnClickListener(v -> authenticateDiscountManagement());
        binding.exportButton.setOnClickListener(v ->
                exportLauncher.launch("karinderya-backup-" + LocalDate.now() + ".json"));
        binding.importButton.setOnClickListener(v ->
                importLauncher.launch(new String[]{"application/json", "text/plain", "application/octet-stream"}));
        binding.logoutButton.setOnClickListener(v -> Ui.confirm(requireContext(), getString(R.string.logout_confirm),
                this::logout));
        binding.versionText.setText(getString(R.string.version_info, getString(R.string.app_name), versionName()));
        render();
    }

    @Override
    public void onDestroyView() {
        if (discountAuthenticationDialog != null) {
            discountAuthenticationDialog.dismiss();
        }
        binding = null;
        super.onDestroyView();
    }

     void render() {
        User user = app.session().requireUser();
        binding.userName.setText(user.fullName());
        binding.userUsername.setText(getString(R.string.username_handle, user.username()));
        binding.storeName.setText(app.settings().storeName());
    }

     void editStoreName() {
        DialogTextInputBinding dialog = DialogTextInputBinding.inflate(getLayoutInflater());
        dialog.inputLayout.setHint(R.string.store_name);
        dialog.input.setText(app.settings().storeName());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.store_name)
                .setView(dialog.getRoot())
                .setPositiveButton(R.string.save, (d, w) -> {
                    app.settings().setStoreName(String.valueOf(dialog.input.getText()));
                    render();
                    if (getActivity() instanceof MainActivity main) {
                        main.refreshChrome();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

     void authenticateDiscountManagement() {
        if (discountAuthenticationDialog != null) {
            return;
        }
        User user = app.session().requireUser();
        DialogTextInputBinding form = DialogTextInputBinding.inflate(getLayoutInflater());
        form.inputLayout.setHint(R.string.current_password);
        form.inputLayout.setEndIconMode(
                com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        form.input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.input.setFilters(new android.text.InputFilter[0]);
        form.input.setSaveEnabled(false);
        form.input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.manage_discounts)
                .setMessage(R.string.authenticate_discounts)
                .setView(form.getRoot())
                .setPositiveButton(R.string.verify_password, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        discountAuthenticationDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            form.input.setText("");
            discountAuthenticationDialog = null;
        });
        dialog.setOnShowListener(ignored -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String password = String.valueOf(form.input.getText());
                    if (password.isEmpty()) {
                        form.inputLayout.setError(getString(R.string.error_required));
                        return;
                    }
                    form.inputLayout.setError(null);
                    dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    form.input.setEnabled(false);
                    Async.run(() -> app.users().login(user.username(), password)
                            .filter(verified -> verified.id().equals(user.id())).isPresent(), verified -> {
                        if (!dialog.isShowing() || !isAdded() || binding == null) {
                            return;
                        }
                        if (!app.session().isLoggedIn()
                                || !app.session().requireUser().id().equals(user.id())) {
                            dialog.dismiss();
                            return;
                        }
                        if (verified) {
                            dialog.dismiss();
                            manageDiscounts();
                        } else {
                            form.input.setEnabled(true);
                            form.input.setText("");
                            form.inputLayout.setError(getString(R.string.error_wrong_password));
                            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        }
                    }, e -> {
                        if (!dialog.isShowing() || !isAdded() || binding == null) {
                            return;
                        }
                        form.input.setEnabled(true);
                        form.input.setText("");
                        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        Ui.showError(requireContext(), e);
                    });
                }));
        dialog.show();
    }

     void manageDiscounts() {
        Async.run(() -> app.discounts().list(), discounts -> {
            if (!isAdded() || binding == null) {
                return;
            }
            String[] labels = discounts.stream()
                    .map(discount -> getString(R.string.discount_option, discount.name(), discount.percent()))
                    .toArray(String[]::new);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.manage_discounts)
                    .setPositiveButton(R.string.add_discount, (d, w) -> editDiscount(null))
                    .setNegativeButton(R.string.cancel, null);
            if (discounts.isEmpty()) {
                builder.setMessage(R.string.no_custom_discounts);
            } else {
                builder.setItems(labels, (d, which) -> editDiscount(discounts.get(which)));
            }
            builder.show();
        }, e -> Ui.showError(requireContext(), e));
    }

     void editDiscount(CustomDiscount original) {
        DialogDiscountBinding form = DialogDiscountBinding.inflate(getLayoutInflater());
        List<Integer> rates = CustomDiscount.ALLOWED_PERCENTAGES;
        String[] labels = rates.stream().map(rate -> getString(R.string.discount_rate, rate))
                .toArray(String[]::new);
        int[] selectedRate = {original == null ? 0 : rates.indexOf(original.percent())};
        form.percentInput.setSimpleItems(labels);
        form.percentInput.setText(labels[selectedRate[0]], false);
        form.percentInput.setOnItemClickListener((parent, view, position, id) -> selectedRate[0] = position);
        if (original != null) {
            form.nameInput.setText(original.name());
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(original == null ? R.string.add_discount : R.string.edit_discount)
                .setView(form.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);
        if (original != null) {
            builder.setNeutralButton(R.string.delete, (d, w) ->
                    Ui.confirm(requireContext(), getString(R.string.delete_discount_confirm, original.name()),
                            () -> Async.run(() -> {
                                app.discounts().delete(original.id());
                                return true;
                            }, ok -> manageDiscounts(), e -> Ui.showError(requireContext(), e))));
        }
        var dialog = builder.create();
        dialog.setOnShowListener(ignored -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = String.valueOf(form.nameInput.getText()).trim();
                    if (name.isEmpty()) {
                        form.nameLayout.setError(getString(R.string.error_required));
                        return;
                    }
                    form.nameLayout.setError(null);
                    CustomDiscount discount = new CustomDiscount(original == null ? UUID.randomUUID() : original.id(),
                            name, rates.get(selectedRate[0]));
                    dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setEnabled(false);
                    if (original != null) {
                        dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).setEnabled(false);
                    }
                    dialog.setCancelable(false);
                    Async.run(() -> {
                        app.discounts().save(discount);
                        return true;
                    }, ok -> {
                        dialog.dismiss();
                        Ui.toast(requireContext(), getString(R.string.saved));
                    }, e -> {
                        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setEnabled(true);
                        if (original != null) {
                            dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
                        }
                        dialog.setCancelable(true);
                        Ui.showError(requireContext(), e);
                    });
                }));
        dialog.show();
    }

     void exportTo(Uri uri) {
        Async.run(() -> {
            try (OutputStream out = requireContext().getContentResolver().openOutputStream(uri, "wt")) {
                if (out == null) {
                    throw new IOException("Unable to open file");
                }
                return app.backup().export(out);
            }
        }, data -> Ui.toast(requireContext(), getString(R.string.export_done, data.menuItems().size(), data.orders().size())), e -> Ui.showError(requireContext(), e));
    }

     void importFrom(Uri uri) {
        Async.run(() -> {
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                if (in == null) {
                    throw new IOException("Unable to open file");
                }
                return app.backup().importFrom(in);
            }
        }, (BackupData data) -> {
            Ui.toast(requireContext(), getString(R.string.import_done, data.menuItems().size(),
                    data.orders().size()));
            if (getActivity() instanceof MainActivity main) {
                main.refreshChrome();
            }
        }, e -> Ui.showError(requireContext(), e));
    }

     void logout() {
        app.session().logout();
        app.cart().clear();
        startActivity(new Intent(requireContext(), LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        requireActivity().finish();
    }

     String versionName() {
        try {
            return requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }
}
