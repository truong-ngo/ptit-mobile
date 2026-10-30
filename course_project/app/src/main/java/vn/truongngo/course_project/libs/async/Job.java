package vn.truongngo.course_project.libs.async;

import android.os.Looper;
import android.util.Log;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

import vn.truongngo.course_project.libs.base.Lifetime;
import vn.truongngo.course_project.libs.util.ReflectionUtils;

/**
 * <p>Handle của một việc chạy nền do {@link Async#run} tạo ra: giữ nơi nhận kết quả và trạng thái hủy.</p>
 *
 * <p>Hai thứ dễ nhầm:</p>
 * <ul>
 *   <li><code>Lifetime</code> kết thúc chỉ làm kết quả bị <b>bỏ</b> (callback không chạy), việc nền vẫn chạy tới cùng.
 *       Nhờ vậy lệnh ghi dữ liệu không mất khi người dùng thoát màn hình.</li>
 *   <li>{@link #cancel()} gọi tường minh mới hủy: bỏ kết quả và không bắt đầu việc nếu nó còn trong hàng đợi. Việc đã
 *       đang chạy thì <b>không</b> bị ngắt (ngắt giữa chừng một lệnh SQLite không an toàn), chỉ bị bỏ kết quả.</li>
 * </ul>
 *
 * @param <T> kiểu kết quả của việc nền
 */
public final class Job<T> {

    /** Tag log chung với {@link Async}, vì hai lớp là một chức năng. */
    private static final String TAG = "Async";

    /** Khoảng đời của nơi tạo job; kết thúc thì kết quả bị bỏ (xem {@link #deliver}). */
    private final Lifetime lifetime;

    /** Nhận kết quả khi việc nền thành công. Chạy ở main thread. */
    private final Consumer<T> onSuccess;

    /** Null thì lỗi bị ném lại ở main thread. */
    private final Consumer<Throwable> onError;

    /** Ghi từ luồng bất kỳ (qua {@link #cancel()}) và đọc từ luồng nền, nên phải là <code>volatile</code>. */
    private volatile boolean cancelled;

    /** Chỉ đọc/ghi ở main thread. */
    private boolean done;

    /** Chỉ đọc/ghi ở main thread. */
    private Runnable onComplete;

    /** Chỉ {@link Async} tạo job. */
    Job(@NonNull Lifetime lifetime, @NonNull Consumer<T> onSuccess, @Nullable Consumer<Throwable> onError) {
        this.lifetime = lifetime;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    /**
     * <p>Việc làm dù thành công hay lỗi, giống <code>finally</code>: tắt vòng xoay "đang tải", bật lại nút Lưu... Chạy ở
     * main thread, sau <code>onSuccess</code> hoặc <code>onError</code> (kể cả khi chúng ném lỗi). Cũng như hai callback
     * đó, bị bỏ khi job đã {@link #cancel()} hoặc <code>Lifetime</code> đã kết thúc.</p>
     *
     * <p>Phải gọi ngay sau {@link Async#run} và chỉ ở main thread: lúc đó kết quả chưa được giao (nó đi qua hàng đợi của
     * main thread) nên không bị lỡ. Gọi nhiều lần thì các hành động chạy theo thứ tự đăng ký.</p>
     *
     * @throws IllegalStateException nếu không ở main thread, hoặc kết quả đã được giao
     */
    @NonNull
    public Job<T> onComplete(@NonNull Runnable action) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Job.onComplete must be called on the main thread");
        }
        if (done) {
            throw new IllegalStateException("The job has already delivered its result, register onComplete right after Async.run");
        }
        Runnable previous = onComplete;
        onComplete = previous == null ? action : () -> {
            previous.run();
            action.run();
        };
        return this;
    }

    /** Bỏ kết quả và không bắt đầu việc nếu nó chưa chạy. Gọi lúc nào, từ luồng nào cũng được. */
    public void cancel() {
        cancelled = true;
    }

    /** Đã gọi {@link #cancel()} chưa. Không phản ánh việc <code>Lifetime</code> kết thúc. */
    public boolean isCancelled() {
        return cancelled;
    }

    /** Đã giao kết quả (hoặc lỗi) cho callback. Chỉ đọc ở main thread. */
    public boolean isDone() {
        return done;
    }

    /** Chỉ chặn bởi {@link #cancel()}: <code>Lifetime</code> kết thúc <b>không</b> ngăn việc chạy. */
    boolean shouldStart() {
        return !cancelled;
    }

    /**
     * Giao kết quả cho callback; luôn được gọi ở main thread. Callback bị bỏ nếu job đã hủy hoặc <code>Lifetime</code>
     * đã kết thúc, và lỗi lúc đó chỉ được ghi log vì không còn ai nhận.
     */
    void deliver(@Nullable T result, @Nullable Throwable error) {
        done = true;
        if (cancelled || lifetime.isEnd()) {
            if (error != null) {
                Log.w(TAG, "Background task failed but its result was dropped (cancelled or lifetime ended)", error);
            }
            return;
        }
        try {
            if (error == null) {
                onSuccess.accept(result);
            } else if (onError != null) {
                onError.accept(error);
            } else {
                // Không có nơi xử lý: ném lại ở main thread để lỗi lộ ra thay vì bị nuốt.
                ReflectionUtils.rethrowRuntimeException(error);
            }
        } finally {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
}
