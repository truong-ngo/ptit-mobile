package vn.truongngo.course_project.libs.util;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jspecify.annotations.NonNull;

import vn.truongngo.course_project.libs.base.Lifetime;

public final class Dialogs {

    /** Chữ nút hủy của {@link #confirm}. Chữ mặc định trong thư viện nên viết tiếng Anh. */
    private static final String CANCEL = "Cancel";

    /** Chữ nút đóng của {@link #message}. Chữ mặc định trong thư viện nên viết tiếng Anh. */
    private static final String CLOSE = "Close";

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private Dialogs() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * <p>Hỏi xác nhận trước một việc không hoàn tác được (ví dụ xóa). Hộp thoại gắn với <code>lifetime</code> và tự đóng
     * khi nó kết thúc, để không rò rỉ cửa sổ khi Activity bị hủy lúc hộp thoại còn mở (xoay màn hình).</p>
     *
     * <p>Nếu <code>lifetime</code> đã kết thúc thì không hiện gì, vì hiện hộp thoại lên Activity đã hủy sẽ ném
     * <code>BadTokenException</code>. Gọi từ main thread.</p>
     *
     * @param confirmText chữ trên nút đồng ý (ví dụ "Delete"); nút còn lại là "Cancel"
     * @param onConfirm   chạy ở main thread khi người dùng đồng ý; bấm Cancel hoặc chạm ra ngoài thì không chạy
     */
    public static void confirm(@NonNull Context context, @NonNull Lifetime lifetime, @NonNull String title,
                               @NonNull String message, @NonNull String confirmText, @NonNull Runnable onConfirm) {
        if (lifetime.isEnd()) {
            return;
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(CANCEL, null)
                .setPositiveButton(confirmText, (d, which) -> onConfirm.run())
                .show();
        lifetime.onEnd(dialog::dismiss);
    }

    /** Hộp thoại thông báo một nút "Close" (ví dụ báo lỗi cần người dùng đọc). Cùng quy ước vòng đời với {@link #confirm}. */
    public static void message(@NonNull Context context, @NonNull Lifetime lifetime,
                               @NonNull String title, @NonNull String message) {
        if (lifetime.isEnd()) {
            return;
        }
        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(CLOSE, null)
                .show();
        lifetime.onEnd(dialog::dismiss);
    }
}
