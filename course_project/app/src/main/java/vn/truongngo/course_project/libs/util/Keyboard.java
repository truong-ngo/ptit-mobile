package vn.truongngo.course_project.libs.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import org.jspecify.annotations.NonNull;

public final class Keyboard {

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private Keyboard() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Ẩn bàn phím đang gắn với cửa sổ của <code>view</code>; view phải đã được attach. */
    public static void hide(@NonNull View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Ẩn bàn phím của Activity qua ô đang có con trỏ, không có thì qua cửa sổ gốc. Hợp để gọi khi bấm Lưu. */
    public static void hide(@NonNull Activity activity) {
        View focused = activity.getCurrentFocus();
        hide(focused != null ? focused : activity.getWindow().getDecorView());
    }

    /**
     * Đặt con trỏ vào <code>field</code> và hiện bàn phím. Việc hiện bàn phím bị hoãn một nhịp qua <code>post</code> vì khi
     * màn hình vừa mở, ô nhập có thể chưa kịp gắn vào cửa sổ và hệ thống sẽ bỏ qua yêu cầu.
     */
    public static void show(@NonNull EditText field) {
        field.requestFocus();
        field.post(() -> {
            InputMethodManager imm = (InputMethodManager) field.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }
}
