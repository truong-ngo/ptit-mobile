package vn.truongngo.course_project.bookstore.ui.book.component;

import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import vn.truongngo.course_project.libs.util.ViewId;

/** Các view của màn hình danh sách sách (<code>fragment_book_list.xml</code>). */
public class BookListViews {
    /** Khối lọc phía trên danh sách. */
    @ViewId("bookFilter")
    public BookFilter bookFilter;

    /** Danh sách sách (phân trang, cuộn tới cuối thì tải thêm). */
    @ViewId("rvBooks")
    public RecyclerView listBooks;

    /** Dòng "chưa có sách" hoặc lỗi nạp, chỉ hiện khi không tải và danh sách rỗng. */
    @ViewId("tvEmpty")
    public TextView emptyListBook;

    /** Nút nổi mở form thêm sách. */
    @ViewId("fabAdd")
    public FloatingActionButton addBookFloatingButton;

    /** Vòng xoay giữa màn hình, hiện khi tải trang đầu. */
    @ViewId("progressCenter")
    public CircularProgressIndicator progressCenter;

    /** Vòng xoay cuối danh sách, hiện khi tải trang kế. */
    @ViewId("progressFooter")
    public CircularProgressIndicator progressFooter;
}
