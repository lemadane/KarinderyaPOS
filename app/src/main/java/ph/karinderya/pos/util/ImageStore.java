package ph.karinderya.pos.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

/** Stores menu photos in app- storage, downscaled and EXIF-rotated. */
public final class ImageStore {

     static final int MAX_EDGE_PX = 1280;
     static final int JPEG_QUALITY = 85;

     final ContentResolver resolver;
     final File photoDir;
     final File cameraDir;

    public ImageStore(Context context) {
        this.resolver = context.getContentResolver();
        this.photoDir = new File(context.getFilesDir(), "menu_photos");
        this.cameraDir = new File(context.getCacheDir(), "camera");
        photoDir.mkdirs();
        cameraDir.mkdirs();
    }

    /** Temporary target file for the camera app; see {@code res/xml/file_paths.xml}. */
    public File newCameraFile() {
        return new File(cameraDir, "capture_" + System.currentTimeMillis() + ".jpg");
    }

    public String importFromUri(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("The selected file is not a supported image");
        }
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= MAX_EDGE_PX || bounds.outHeight / (sample * 2) >= MAX_EDGE_PX) {
            sample *= 2;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = open(uri)) {
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        if (bitmap == null) {
            throw new IOException("Unable to decode image");
        }
        bitmap = scaleAndRotate(bitmap, readRotation(uri));
        File out = newPhotoFile();
        try (OutputStream os = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, os);
        } finally {
            bitmap.recycle();
        }
        return out.getAbsolutePath();
    }

    public String importFromBytes(byte[] jpeg) throws IOException {
        File out = newPhotoFile();
        Files.write(out.toPath(), jpeg);
        return out.getAbsolutePath();
    }

    public byte[] readBytes(String path) throws IOException {
        return Files.readAllBytes(new File(path).toPath());
    }

    public boolean exists(String path) {
        return path != null && new File(path).isFile();
    }

    /** Deletes a photo, but only if it lives inside this app's photo directory. */
    public void delete(String path) {
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (photoDir.equals(file.getParentFile()) || cameraDir.equals(file.getParentFile())) {
            file.delete();
            ImageLoader.evict(path);
        }
    }

     File newPhotoFile() {
        return new File(photoDir, "item_" + UUID.randomUUID() + ".jpg");
    }

     InputStream open(Uri uri) throws IOException {
        InputStream in = resolver.openInputStream(uri);
        if (in == null) {
            throw new IOException("Unable to open image");
        }
        return in;
    }

     int readRotation(Uri uri) {
        try (InputStream in = open(uri)) {
            int orientation = new ExifInterface(in)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            return switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
                case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
                case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
                default -> 0;
            };
        } catch (IOException e) {
            return 0;
        }
    }

     static Bitmap scaleAndRotate(Bitmap source, int rotation) {
        int longest = Math.max(source.getWidth(), source.getHeight());
        float scale = longest > MAX_EDGE_PX ? (float) MAX_EDGE_PX / longest : 1f;
        if (scale == 1f && rotation == 0) {
            return source;
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        matrix.postRotate(rotation);
        Bitmap result = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        if (result != source) {
            source.recycle();
        }
        return result;
    }
}
