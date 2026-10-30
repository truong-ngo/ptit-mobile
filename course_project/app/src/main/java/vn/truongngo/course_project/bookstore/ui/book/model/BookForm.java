package vn.truongngo.course_project.bookstore.ui.book.model;

import org.jspecify.annotations.NonNull;

import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.bookstore.domain.BookCondition;
import vn.truongngo.course_project.libs.form.annotation.FormField;
import vn.truongngo.course_project.libs.form.validation.Min;
import vn.truongngo.course_project.libs.form.validation.NotNull;
import vn.truongngo.course_project.libs.form.validation.Size;

/**
 * <p>Dữ liệu người dùng nhập ở form sách, ánh xạ với các ô trong <code>fragment_book_form.xml</code> bằng
 * <code>FormBinder</code>. Tách khỏi {@link Book} để thực thể không mang annotation và kiểu của giao diện.</p>
 *
 * <p>Điểm khác {@link Book} có lý do: <code>price</code> là <code>Long</code> (không phải <code>long</code>) để ô giá bỏ
 * trống ra <code>null</code> và <code>@NotNull</code> bắt được, trong khi kiểu nguyên thủy sẽ lặng lẽ thành 0; các field
 * văn bản được chuẩn hóa (cắt khoảng trắng, rỗng thành <code>null</code>) khi chuyển sang {@link Book}.</p>
 *
 * <p>Thông báo lỗi viết bằng tiếng Việt vì hiển thị thẳng cho người dùng; thông báo mặc định của thư viện là tiếng Anh.
 * Cần constructor không tham số để <code>FormBinder#read</code> tạo được đối tượng.</p>
 */
public class BookForm {

    /** Không có ô nhập: chỉ để biết đang sửa cuốn nào. Null khi tạo mới. */
    private Long id;

    /** Tên sách, bắt buộc và tối đa 200 ký tự. */
    @FormField(id = "etTitle", label = "Tên sách", order = 1)
    @NotNull(message = "Vui lòng nhập tên sách")
    @Size(max = 200, message = "Tên sách tối đa 200 ký tự")
    private String title;

    /** Tác giả, không bắt buộc, tối đa 100 ký tự. */
    @FormField(id = "etAuthor", label = "Tác giả", order = 2)
    @Size(max = 100, message = "Tên tác giả tối đa 100 ký tự")
    private String author;

    /** Tình trạng, bắt buộc; spinner tự có danh sách từ các hằng của {@link BookCondition}. */
    @FormField(id = "spCondition", label = "Tình trạng", order = 3)
    @NotNull(message = "Vui lòng chọn tình trạng")
    private BookCondition condition;

    /** Giá bán (đồng), bắt buộc và lớn hơn 0. */
    @FormField(id = "etPrice", label = "Giá bán", order = 4)
    @NotNull(message = "Vui lòng nhập giá bán")
    @Min(value = 0, inclusive = false, message = "Giá bán phải lớn hơn 0")
    private Long price;

    /** Null là chưa phân loại. */
    @FormField(id = "spCategory", label = "Thể loại", order = 5)
    private Long categoryId;

    /** Constructor không tham số cho <code>FormBinder#read</code> và cho {@link #empty()}. */
    public BookForm() {
    }

    /** Mã sách đang sửa, <code>null</code> khi thêm mới. */
    public Long getId() {
        return id;
    }

    /** Tên sách đang có trong form, dùng để nêu trong câu xác nhận xóa. */
    public String getTitle() {
        return title;
    }

    /** Tác giả đang có trong form. */
    public String getAuthor() {
        return author;
    }

    /** Tình trạng đang có trong form. */
    public BookCondition getCondition() {
        return condition;
    }

    /** Giá bán đang có trong form. */
    public Long getPrice() {
        return price;
    }

    /** Mã thể loại đang chọn, <code>null</code> là chưa phân loại. */
    public Long getCategoryId() {
        return categoryId;
    }

    /**
     * Form trống cho màn hình thêm mới. Luôn tạo <b>đối tượng mới</b> thay vì trả một hằng dùng chung: form là đối tượng
     * sửa được (<code>FormBinder#readInto</code> ghi thẳng vào nó), nên một thể hiện dùng chung sẽ mang dữ liệu của lần
     * nhập trước sang lần sau.
     */
    @NonNull
    public static BookForm empty() {
        return new BookForm();
    }

    /** Dựng form từ một cuốn sách có sẵn, để mở màn hình sửa. */
    @NonNull
    public static BookForm from(@NonNull Book book) {
        BookForm form = new BookForm();
        form.id = book.getId();
        form.title = book.getTitle();
        form.author = book.getAuthor();
        form.condition = book.getCondition();
        form.price = book.getPrice();
        form.categoryId = book.getCategoryId();
        return form;
    }

    /**
     * Chuyển thành {@link Book} để lưu. Chỉ gọi sau khi form đã qua kiểm tra: <code>title</code>, <code>condition</code>
     * và <code>price</code> được giả định khác <code>null</code>. Tên sách và tác giả được cắt khoảng trắng, tác giả để
     * trống thì lưu <code>null</code>.
     */
    @NonNull
    public Book toBook() {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title.trim());
        book.setAuthor(author == null || author.isBlank() ? null : author.trim());
        book.setCondition(condition);
        book.setPrice(price);
        book.setCategoryId(categoryId);
        return book;
    }
}
