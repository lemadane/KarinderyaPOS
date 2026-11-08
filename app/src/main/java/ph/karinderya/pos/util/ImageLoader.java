package ph.karinderya.pos.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import ph.karinderya.pos.R;

/** Minimal async thumbnail loader with an in-memory LRU cache. */
public final class ImageLoader {

     static final int THUMB_PX = 480;
     static final BitmapCache CACHE =
            new BitmapCache((int) Math.min(Runtime.getRuntime().maxMemory() / 1024 / 8, 32 * 1024));

     ImageLoader() {
    }

    public static void load(ImageView view, String path) {
        view.setTag(R.id.image_path_tag, path);
        if (path == null || path.isEmpty()) {
            showPlaceholder(view);
            return;
        }
        Bitmap cached = CACHE.get(path);
        if (cached != null) {
            showBitmap(view, cached);
            return;
        }
        showPlaceholder(view);
        Async.io(() -> {
            Bitmap bitmap = decodeSampled(path, THUMB_PX);
            if (bitmap == null) {
                return;
            }
            CACHE.put(path, bitmap);
            Async.main(() -> {
                if (path.equals(view.getTag(R.id.image_path_tag))) {
                    showBitmap(view, bitmap);
                }
            });
        });
    }

    public static void evict(String path) {
        if (path != null) {
            CACHE.remove(path);
        }
    }

    public static Bitmap decodeSampled(String path, int reqSize) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= reqSize && bounds.outHeight / (sample * 2) >= reqSize) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        return BitmapFactory.decodeFile(path, options);
    }

     static void showPlaceholder(ImageView view) {
        view.setScaleType(ImageView.ScaleType.CENTER);
        view.setImageResource(R.drawable.ic_restaurant_placeholder);
    }

     static void showBitmap(ImageView view, Bitmap bitmap) {
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setImageBitmap(bitmap);
    }
}
