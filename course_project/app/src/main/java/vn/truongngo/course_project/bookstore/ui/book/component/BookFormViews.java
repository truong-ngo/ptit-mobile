package vn.truongngo.course_project.bookstore.ui.book.component;

import android.widget.Button;

import vn.truongngo.course_project.bookstore.ui.book.model.BookForm;
import vn.truongngo.course_project.libs.form.FormBinder;
import vn.truongngo.course_project.libs.util.ViewId;

/**
 * <p>Các view của màn hình form sách mà <code>BookFormFragment</code> tự điều khiển: ba nút, cùng {@link FormBinder}
 * nối năm ô nhập với {@link BookForm}.</p>
 *
 * <p>Năm ô nhập (tên, tác giả, tình trạng, giá, thể loại) <b>không</b> nằm ở đây: {@link FormBinder} tự tìm chúng theo
 * id khai báo trong <code>@FormField</code> của {@link BookForm}, nên khai báo thêm ở đây chỉ tạo hai nguồn cho cùng một
 * view và dễ lệch nhau khi đổi id.</p>
 */
public class BookFormViews {

    /**
     * Không phải một view nhưng nằm ở đây có chủ ý: {@link FormBinder} giữ tham chiếu tới các ô nhập, nên để ở field của
     * Fragment thì cây View đã hủy bị giữ lại khi Fragment nằm trong back stack. Container bị nhả ở
     * <code>onDestroyView</code> nên nó chết cùng cây View. Không gắn <code>@ViewId</code> nên <code>ViewBinder</code> bỏ
     * qua; <code>BookFormFragment.bindViews</code> gán sau khi tạo container, vì cần <code>root</code>.
     */
    public FormBinder<BookForm> form;

    /** Lưu form (thêm hoặc sửa). */
    @ViewId("btnSave")
    public Button saveButton;

    /** Đóng form, quay về danh sách mà không ghi gì. */
    @ViewId("btnCancel")
    public Button cancelButton;

    /** Chỉ hiện khi sửa một cuốn sách có sẵn. */
    @ViewId("btnDelete")
    public Button deleteButton;
}
