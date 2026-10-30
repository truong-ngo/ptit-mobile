package vn.truongngo.course_project.libs.async;

import android.os.Handler;
import android.os.Looper;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import vn.truongngo.course_project.libs.base.Lifetime;

public final class Async {

    /** Handler của main thread, nơi mọi callback được post tới. */
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Số thread của pool mặc định {@link #io()}. */
    private static final int POOL_SIZE = 4;

    /** Việc nền không trả kết quả (ghi, xóa). Khác <code>Runnable</code> ở chỗ được ném checked exception. */
    @FunctionalInterface
    public interface VoidTask {

        /**
         * @throws Exception lỗi của việc nền, được giao cho <code>onError</code> của {@link Job}
         */
        void run() throws Exception;
    }

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private Async() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Pool dùng chung (4 thread), chỉ được tạo khi gọi lần đầu. Thread là daemon nên không giữ tiến trình sống. */
    @NonNull
    public static Executor io() {
        return Pool.INSTANCE;
    }

    /**
     * <p>Chạy <code>task</code> ngoài main thread rồi giao kết quả về main thread. Việc nền chỉ nên làm phần thuần: sửa
     * dữ liệu dùng chung (ví dụ <code>ViewModel</code>) thì làm trong <code>onSuccess</code>, trên main thread.</p>
     *
     * <p>Quy ước:</p>
     * <ul>
     *   <li>Callback luôn chạy ở main thread và luôn được post vào hàng đợi, kể cả khi gọi từ main thread.</li>
     *   <li><code>lifetime</code> kết thúc thì callback bị bỏ nhưng việc nền vẫn chạy xong, để lệnh ghi không mất khi
     *       người dùng thoát màn hình (xem {@link Job}).</li>
     *   <li>Lỗi mà không có <code>onError</code> thì bị ném lại ở main thread, thay vì bị nuốt.</li>
     * </ul>
     *
     * <p>Bản này chạy trên {@link #io()} và không có <code>onError</code>.</p>
     */
    @NonNull
    public static <T> Job<T> run(@NonNull Lifetime lifetime,
                                 @NonNull Callable<T> task,
                                 @NonNull Consumer<T> onSuccess) {
        return run(io(), lifetime, task, onSuccess, null);
    }

    /** Như bản trên, thêm <code>onError</code> (main thread) để nhận lỗi của <code>task</code>. */
    @NonNull
    public static <T> Job<T> run(@NonNull Lifetime lifetime,
                                 @NonNull Callable<T> task,
                                 @NonNull Consumer<T> onSuccess,
                                 @Nullable Consumer<Throwable> onError) {
        return run(io(), lifetime, task, onSuccess, onError);
    }

    /**
     * Như bản trên nhưng chạy trên <code>executor</code> do người dùng chọn, ví dụ một executor đơn luồng riêng cho
     * SQLite để các lệnh theo đúng thứ tự.
     */
    @NonNull
    public static <T> Job<T> run(@NonNull Executor executor,
                                 @NonNull Lifetime lifetime,
                                 @NonNull Callable<T> task,
                                 @NonNull Consumer<T> onSuccess,
                                 @Nullable Consumer<Throwable> onError) {
        Job<T> job = new Job<>(lifetime, onSuccess, onError);
        executor.execute(() -> {
            if (!job.shouldStart()) {
                return;
            }
            T result = null;
            Throwable error = null;
            try {
                result = task.call();
            } catch (Throwable t) {
                error = t;
            }
            T finalResult = result;
            Throwable finalError = error;
            MAIN.post(() -> job.deliver(finalResult, finalError));
        });
        return job;
    }

    /** Như {@link #run(Lifetime, Callable, Consumer)} cho việc không có kết quả; <code>onSuccess</code> chạy khi xong. */
    @NonNull
    public static Job<Void> runVoid(@NonNull Lifetime lifetime,
                                    @NonNull VoidTask task,
                                    @NonNull Runnable onSuccess) {
        return runVoid(io(), lifetime, task, onSuccess, null);
    }

    /** Như bản trên, thêm <code>onError</code> (main thread). */
    @NonNull
    public static Job<Void> runVoid(@NonNull Lifetime lifetime,
                                    @NonNull VoidTask task,
                                    @NonNull Runnable onSuccess,
                                    @Nullable Consumer<Throwable> onError) {
        return runVoid(io(), lifetime, task, onSuccess, onError);
    }

    /** Như bản trên nhưng chạy trên <code>executor</code> do người dùng chọn. */
    @NonNull
    public static Job<Void> runVoid(@NonNull Executor executor,
                                    @NonNull Lifetime lifetime,
                                    @NonNull VoidTask task,
                                    @NonNull Runnable onSuccess,
                                    @Nullable Consumer<Throwable> onError) {
        return run(executor, lifetime, () -> {
            task.run();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }

    /** Holder idiom: pool chỉ được tạo khi {@link #io()} được gọi lần đầu. */
    private static final class Pool {

        /** Pool dùng chung; thread đặt tên <code>async-N</code> để dễ nhận ra trong log và debugger. */
        static final ExecutorService INSTANCE = Executors.newFixedThreadPool(POOL_SIZE, new ThreadFactory() {

            /**
             * Số thứ tự đặt tên thread, bắt đầu từ 1.
             * */
            private final AtomicInteger count = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "async-" + count.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        });
    }
}
