package vn.truongngo.course_project.bookstore.ui.book.component;

import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import vn.truongngo.course_project.libs.util.ViewId;

/** Các view của khối lọc sách (<code>layout_book_filter.xml</code>). */
public class BookFilterViews {
    /** Chọn thể loại; mục đầu là "Tất cả". */
    @ViewId("spCategoryFilter")
    public Spinner categoryFilter;

    /** Chọn tình trạng; mục đầu là "Tất cả". */
    @ViewId("spConditionFilter")
    public Spinner conditionFilter;

    /** Giá từ (tính cả mốc). */
    @ViewId("etMinPrice")
    public EditText minPriceFilter;

    /** Giá đến (tính cả mốc), nơi hiện lỗi khoảng giá sai. */
    @ViewId("etMaxPrice")
    public EditText maxPriceFilter;

    /** Áp dụng bộ lọc. */
    @ViewId("btnFilter")
    public Button submitButton;

    /** Xóa bộ lọc và áp dụng bộ lọc rỗng. */
    @ViewId("btnClearFilter")
    public Button clearButton;
}
