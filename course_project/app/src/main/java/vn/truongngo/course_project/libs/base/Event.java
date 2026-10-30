package vn.truongngo.course_project.libs.base;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * <p>Gói một sự kiện chỉ được xử lý một lần (hiện Snackbar, báo lỗi) khi đặt vào <code>LiveData</code>.</p>
 *
 * <p><code>LiveData</code> luôn gửi lại giá trị cuối cho observer mới, nên sau khi xoay màn hình một Snackbar lỗi sẽ hiện
 * lại. Gói giá trị vào <code>Event</code> thì lần nhận đầu {@link #consume()} lấy được nội dung, các lần sau chỉ nhận
 * <code>null</code>. Chỉ gọi từ main thread, giống <code>LiveData.setValue</code>.</p>
 *
 * @param <T> kiểu nội dung của sự kiện
 */
public final class Event<T> {

    /** Nội dung của sự kiện, không bao giờ <code>null</code>. */
    private final T content;

    /** Đã có ai lấy nội dung ra chưa. Không an toàn đa luồng, vì chỉ dùng ở main thread. */
    private boolean consumed;

    /**
     * Gói nội dung thành một sự kiện chưa được xử lý.
     *
     * @param content nội dung cần báo, không được <code>null</code> (để <code>null</code> từ {@link #consume()} chỉ có
     *                nghĩa là "đã lấy rồi")
     */
    public Event(@NonNull T content) {
        this.content = content;
    }

    /** Trả nội dung ở lần gọi đầu tiên, các lần sau trả <code>null</code>. */
    @Nullable
    public T consume() {
        if (consumed) {
            return null;
        }
        consumed = true;
        return content;
    }
}
