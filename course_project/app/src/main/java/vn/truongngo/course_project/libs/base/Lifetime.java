package vn.truongngo.course_project.libs.base;

import android.util.Log;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * <p>Khoảng đời của một thứ có vòng đời (Activity, View của Fragment, một lần attach của View, ViewModel), dùng để dọn
 * tài nguyên và bỏ các callback đến trễ khi thứ đó bị hủy.</p>
 *
 * <p>Chỉ dùng một lần và chỉ có một sự kiện kết thúc (hủy hẳn): bắt đầu chính là lúc tạo, nên không có hook cho
 * <code>onStart</code>/<code>onStop</code>. Việc cần chạy theo từng lần hiện/khuất không hợp với lớp này vì các
 * trạng thái đó lặp lại nhiều lần trong một vòng đời.</p>
 */
public final class Lifetime implements DefaultLifecycleObserver {

    private static final String TAG = "Lifetime";

    /** Chạy theo thứ tự ngược với lúc đăng ký (LIFO), giống đóng tài nguyên theo thứ tự ngược với lúc mở. */
    private final Deque<Runnable> endActions = new ArrayDeque<>();

    /** Đã kết thúc chưa; chỉ chuyển từ <code>false</code> sang <code>true</code> một lần. */
    private boolean ended;

    /** Chỉ tạo qua {@link #of(Lifecycle)} hoặc {@link #manual()}. */
    Lifetime() {
    }

    /**
     * Kết thúc khi {@code lifecycle} tới <code>ON_DESTROY</code>. Nếu nó đã <code>DESTROYED</code> thì khoảng đời kết thúc ngay
     * từ đầu, nên mọi {@link #onEnd} đăng ký sau đó chạy tức thì.
     */
    @NonNull
    public static Lifetime of(@NonNull Lifecycle lifecycle) {
        Lifetime lifetime = new Lifetime();
        if (lifecycle.getCurrentState() == Lifecycle.State.DESTROYED) {
            lifetime.ended = true;
        } else {
            lifecycle.addObserver(lifetime);
        }
        return lifetime;
    }

    /**
     * Khoảng đời không gắn Lifecycle nào, chỉ kết thúc khi {@link #end()} được gọi. Dành cho thứ không có lifecycle
     * riêng: {@link BaseLayout} (View kết thúc theo attach/detach) và {@link BaseViewModel} (kết thúc ở
     * <code>onCleared</code>).
     */
    @NonNull
    static Lifetime manual() {
        return new Lifetime();
    }

    /** Callback đến trễ (async, listener) phải kiểm tra trước khi đụng vào View. */
    public boolean isEnd() {
        return ended;
    }

    /**
     * Đăng ký việc dọn dẹp khi khoảng đời kết thúc (hủy task, đóng dialog, gỡ listener...). Chạy theo thứ tự ngược với
     * lúc đăng ký; nếu khoảng đời đã kết thúc thì chạy ngay.
     * Không an toàn đa luồng: chỉ gọi từ main thread.
     */
    public void onEnd(@NonNull Runnable action) {
        if (ended) {
            action.run();
            return;
        }
        endActions.push(action);
    }

    /**
     * Kết thúc khoảng đời và chạy các hành động đã đăng ký. Gọi nhiều lần chỉ chạy một lần; một hành động ném lỗi
     * chỉ bị ghi log, không chặn các hành động còn lại.
     */
    void end() {
        if (ended) {
            return;
        }
        ended = true;
        while (!endActions.isEmpty()) {
            try {
                endActions.pop().run();
            } catch (RuntimeException e) {
                Log.w(TAG, "onEnd action failed", e);
            }
        }
    }

    /** Gỡ chính nó khỏi lifecycle rồi kết thúc. Do <code>Lifecycle</code> gọi, không gọi tay. */
    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        owner.getLifecycle().removeObserver(this);
        end();
    }
}
