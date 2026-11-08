package ph.karinderya.pos.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Runs database / file work off the main thread and delivers results back on it. */
public final class Async {

     static final String TAG = "Async";
     static final ExecutorService IO = Executors.newFixedThreadPool(3);
     static final Handler MAIN = new Handler(Looper.getMainLooper());

     Async() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        IO.execute(() -> {
            try {
                T result = work.call();
                MAIN.post(() -> onSuccess.accept(result));
            } catch (Throwable t) {
                Log.e(TAG, "Background task failed", t);
                MAIN.post(() -> onError.accept(t));
            }
        });
    }

    public static void io(Runnable runnable) {
        IO.execute(runnable);
    }

    public static void main(Runnable runnable) {
        MAIN.post(runnable);
    }
}
