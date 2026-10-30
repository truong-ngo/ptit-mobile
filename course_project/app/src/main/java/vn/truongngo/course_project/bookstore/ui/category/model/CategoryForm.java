package vn.truongngo.course_project.bookstore.ui.category.model;

import org.jspecify.annotations.NonNull;

import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.libs.form.annotation.FormField;
import vn.truongngo.course_project.libs.form.validation.NotNull;
import vn.truongngo.course_project.libs.form.validation.Size;

/**
 * <p>Dữ liệu người dùng nhập ở form thể loại, ánh xạ với ô trong <code>fragment_category_form.xml</code> bằng
 * <code>FormBinder</code>. Tách khỏi {@link Category} để thực thể không mang annotation của giao diện, cùng cách với
 * <code>BookForm</code>.</p>
 *
 * <p>Thông báo lỗi viết bằng tiếng Việt vì hiển thị thẳng cho người dùng. Cần constructor không tham số để
 * <code>FormBinder#read</code> tạo được đối tượng.</p>
 */
public class CategoryForm {

    /** Không có ô nhập: chỉ để biết đang sửa thể loại nào. Null khi tạo mới. */
    private Long id;

    /** Tên thể loại, bắt buộc và tối đa 100 ký tự. Cột <code>name</code> của bảng còn <code>unique</code>, kiểm tra ở DB. */
    @FormField(id = "etName", label = "Tên thể loại", order = 1)
    @NotNull(message = "Vui lòng nhập tên thể loại")
    @Size(max = 100, message = "Tên thể loại tối đa 100 ký tự")
    private String name;

    /** Constructor không tham số cho <code>FormBinder#read</code> và cho {@link #empty()}. */
    public CategoryForm() {
    }

    /** Form trống để thêm mới. Mỗi lần gọi trả một đối tượng mới vì form sửa được, không dùng chung một hằng. */
    @NonNull
    public static CategoryForm empty() {
        return new CategoryForm();
    }

    /** Dựng form từ một thể loại có sẵn, để mở màn hình sửa. */
    @NonNull
    public static CategoryForm from(@NonNull Category category) {
        CategoryForm form = new CategoryForm();
        form.id = category.getId();
        form.name = category.getName();
        return form;
    }

    /** Tên đang có trong form, dùng để nêu tên thể loại trong câu xác nhận xóa. */
    public String getName() {
        return name;
    }

    /**
     * Chuyển thành {@link Category} để lưu. Chỉ gọi sau khi form đã qua kiểm tra: <code>name</code> được giả định khác
     * <code>null</code>. Tên được cắt khoảng trắng hai đầu.
     */
    @NonNull
    public Category toCategory() {
        Category category = new Category();
        category.setId(id);
        category.setName(name.trim());
        return category;
    }
}
