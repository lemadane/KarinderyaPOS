package ph.karinderya.pos.util;

import android.graphics.Bitmap;
import android.util.LruCache;

public final class BitmapCache extends LruCache<String, Bitmap> {

    public BitmapCache(int maxKilobytes) {
        super(maxKilobytes);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return Math.max(1, value.getByteCount() / 1024);
    }
}
