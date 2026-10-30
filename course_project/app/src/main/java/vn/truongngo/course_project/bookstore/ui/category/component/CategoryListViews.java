package vn.truongngo.course_project.bookstore.ui.category.component;

import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import vn.truongngo.course_project.libs.util.ViewId;

/**
 * Các view của màn hình danh sách thể loại (<code>fragment_category_list.xml</code>). Danh sách thể loại nhỏ và không
 * phân trang nên chỉ có một vòng xoay ở giữa, khác màn hình danh sách sách.
 */
public class CategoryListViews {

    /** Tiêu đề phía trên danh sách, kèm tổng số thể loại sau khi dữ liệu về. */
    @ViewId("tvHeader")
    public TextView header;

    /** Danh sách thể loại. */
    @ViewId("rvCategories")
    public RecyclerView listCategories;

    /** Vòng xoay ở giữa, hiện khi đang nạp. */
    @ViewId("progress")
    public CircularProgressIndicator progress;

    /** Dòng "chưa có thể loại", chỉ hiện khi không nạp và danh sách rỗng. */
    @ViewId("tvEmpty")
    public TextView emptyListCategory;

    /** Nút nổi mở form thêm thể loại. */
    @ViewId("fabAdd")
    public FloatingActionButton addCategoryFloatingButton;
}
