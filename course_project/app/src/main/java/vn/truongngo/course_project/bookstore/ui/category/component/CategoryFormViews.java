package vn.truongngo.course_project.bookstore.ui.category.component;

import android.widget.Button;

import vn.truongngo.course_project.bookstore.ui.category.model.CategoryForm;
import vn.truongngo.course_project.libs.form.FormBinder;
import vn.truongngo.course_project.libs.util.ViewId;

/**
 * <p>Các view của màn hình form thể loại mà <code>CategoryFormFragment</code> tự điều khiển: ba nút, cùng
 * {@link FormBinder} nối ô nhập với {@link CategoryForm}.</p>
 *
 * <p>Ô nhập tên thể loại <b>không</b> nằm ở đây: {@link FormBinder} tự tìm nó theo id khai báo trong
 * <code>@FormField</code> của {@link CategoryForm}, nên khai báo thêm ở đây chỉ tạo hai nguồn cho cùng một view.</p>
 */
public class CategoryFormViews {

    /** Lưu form (thêm hoặc sửa). */
    @ViewId("btnSave")
    public Button saveButton;

    /** Đóng form, quay về danh sách mà không ghi gì. */
    @ViewId("btnCancel")
    public Button cancelButton;

    /** Chỉ hiện khi sửa một thể loại có sẵn. */
    @ViewId("btnDelete")
    public Button deleteButton;

    /**
     * Không phải một view nhưng nằm ở đây có chủ ý: {@link FormBinder} giữ tham chiếu tới ô nhập, nên để ở field của
     * Fragment thì cây View đã hủy bị giữ lại khi Fragment nằm trong back stack. Container bị nhả ở
     * <code>onDestroyView</code> nên nó chết cùng cây View. Không gắn <code>@ViewId</code> nên <code>ViewBinder</code> bỏ
     * qua; <code>CategoryFormFragment.bindViews</code> gán sau khi tạo container, vì cần <code>root</code>.
     */
    public FormBinder<CategoryForm> form;
}
