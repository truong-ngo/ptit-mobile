package vn.truongngo.course_project.bookstore.ui.book.component;

import android.widget.TextView;

import vn.truongngo.course_project.libs.util.ViewId;

/** Các view của một dòng sách (<code>item_book.xml</code>). */
public class BookItemViews {
    /** Tên sách. */
    @ViewId("tvTitle")
    public TextView title;

    /** Giá bán, định dạng tiền VND. */
    @ViewId("tvPrice")
    public TextView price;

    /** Tác giả. */
    @ViewId("tvAuthor")
    public TextView author;

    /** Tình trạng sách. */
    @ViewId("tvCondition")
    public TextView condition;
}
