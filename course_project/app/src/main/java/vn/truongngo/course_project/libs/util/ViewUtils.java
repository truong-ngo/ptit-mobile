package vn.truongngo.course_project.libs.util;

import android.view.View;

import androidx.annotation.IdRes;

import org.jspecify.annotations.NonNull;

public final class ViewUtils {

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private ViewUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * <code>findViewById</code> nhưng không bao giờ trả null. Kiểu trả về suy ra từ chỗ gán, nên gán sai kiểu (ví dụ id
     * của <code>Button</code> vào biến <code>EditText</code>) chỉ lộ lúc chạy bằng <code>ClassCastException</code>.
     *
     * @throws IllegalStateException nếu không có view nào mang id đó trong <code>root</code>
     */
    @NonNull
    public static <T extends View> T require(@NonNull View root, @IdRes int id) {
        T view = root.findViewById(id);
        if (view == null) {
            throw new IllegalStateException("View '" + root.getResources().getResourceName(id) + "' not found");
        }
        return view;
    }

    /** Hiện view nếu <code>visible</code>, không thì ẩn hẳn (<code>GONE</code>, không chiếm chỗ). Luôn đặt cả hai nhánh. */
    public static void visibleIf(@NonNull View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
