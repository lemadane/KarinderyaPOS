package ph.karinderya.pos.data.repo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import android.util.Base64;
import android.util.Log;
import ph.karinderya.pos.data.db.DbHelper;
import ph.karinderya.pos.data.model.BackupData;
import ph.karinderya.pos.data.model.BackupMenuItem;
import ph.karinderya.pos.data.model.MenuItem;
import ph.karinderya.pos.data.model.Order;
import ph.karinderya.pos.util.ImageStore;
import ph.karinderya.pos.util.Json;

/**
 * JSON backup of menu (with embedded photos) and order history. User accounts are intentionally
 * excluded so password hashes never leave the device. Import merges by UUID.
 */
public record BackupService(
    DbHelper db,
    MenuRepository menu,
    OrderRepository orders,
    ImageStore images) {

  static final String TAG = "BackupService";

  public BackupData export(OutputStream out)
      throws IOException {
    List<BackupMenuItem> menuItems = new ArrayList<>();
    for (MenuItem item : menu.list(null, null, false)) {
      String photo = null;
      if (images.exists(item.photoPath())) {
        photo =
            Base64.encodeToString(
                images.readBytes(item.photoPath()),
                Base64.NO_WRAP);
      }
      menuItems.add(
          new BackupMenuItem(
              item.withPhotoPath(null),
              photo));
    }
    BackupData data =
        new BackupData(BackupData.CURRENT_VERSION, Instant.now(), menuItems,
        orders.listAllForBackup(), new DiscountRepository(db).list());
    Json.mapper(
    ).writerWithDefaultPrettyPrinter()
            .writeValue(
                out,
                data);
        return data;
      }

      public BackupData importFrom(InputStream in)
          throws IOException {
        BackupData data =
            Json.mapper()
            .readValue(
                in,
                BackupData.class);
        if (data.version() > BackupData.CURRENT_VERSION) {
          throw new IOException(
              "Backup was made by a newer app version");
        }
        db.getWritableDatabase()
            .beginTransaction();
        try {
          DiscountRepository discounts = new DiscountRepository(db);
          for (var discount : data.discounts()) {
            discounts.save(discount);
          }
          for (BackupMenuItem entry : data.menuItems()) {
            MenuItem item = entry.item();
            if (item == null || item.id() == null) {
              continue;
            }
            String existingPhoto =
                menu.findById(
                    item.id())
                .map(MenuItem::photoPath)
                .orElse(null);
            String photoPath = existingPhoto;
            if (entry.photoBase64() != null) {
              try {
                photoPath =
                    images.importFromBytes(
                        Base64.decode(
                            entry.photoBase64(),
                            Base64.DEFAULT));
                images.delete(existingPhoto);
              } catch (IllegalArgumentException | IOException e) {
                Log.w(
                    TAG,
                    "Skipping invalid photo for " + item.name(),
                    e);
              }
            }
            menu.upsert(item.withPhotoPath(photoPath));
          }
          for (Order order : data.orders()) {
            if (order.id() != null) {
              orders.upsert(order);
            }
          }
          db.getWritableDatabase()
              .setTransactionSuccessful();
        } finally {
          db.getWritableDatabase()
              .endTransaction();
        }
        return data;
      }
    }
