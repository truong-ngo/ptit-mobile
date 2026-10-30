package vn.truongngo.course_project.bookstore.ui.book.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.truongngo.course_project.bookstore.domain.Category;

/**
 * <p>State của khối lọc sách: bộ lọc đang áp dụng và danh sách thể loại để chọn. Là một lớp thường nằm trong
 * <code>BookListViewModel</code> (không phải một ViewModel thứ hai), nên sống và chết cùng nó và qua được xoay màn hình.</p>
 *
 * <p>Lớp này chỉ giữ dữ liệu và logic thuần, <b>không</b> chạy việc nền: ViewModel gốc nạp thể loại bằng
 * <code>run(...)</code> rồi gọi {@link #setCategories}. Nhờ vậy lớp không phải biết thread hay <code>Lifetime</code>, và
 * mọi thay đổi chỉ xảy ra ở main thread.</p>
 */
public class BookFilterModel {

    // State

    /** Bộ lọc đang áp dụng. Không bao giờ <code>null</code>; ban đầu là bộ lọc rỗng (lọc ra mọi sách). */
    private BookCriteria criteria = new BookCriteria();

    /**
     * Không có giá trị ban đầu, để phân biệt "chưa nạp" (<code>null</code>) với "đã nạp mà không có thể loại nào" (danh
     * sách rỗng).
     */
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();

    // Getter

    /**
     * Bộ lọc đang áp dụng, để giao diện dựng lại (xoay màn hình) đổ lại vào các ô lọc. Chỉ đọc: object này được dùng để
     * so sánh với bộ lọc mới ở {@link #apply}, nên sửa field tại chỗ sẽ làm phép so sánh sai.
     */
    @NonNull
    public BookCriteria getCriteria() {
        return criteria;
    }

    /**
     * Thể loại để chọn trong bộ lọc. Chưa có giá trị cho tới khi {@link #setCategories} được gọi lần đầu; giá trị là danh
     * sách không sửa được.
     */
    @NonNull
    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    /** Đã nạp thể loại ít nhất một lần thành công (kể cả khi kết quả rỗng). */
    public boolean hasCategories() {
        return categories.getValue() != null;
    }

    // Action

    /**
     * Lưu danh sách thể loại và báo cho người nghe. Chỉ gọi ở main thread, như <code>LiveData.setValue</code>. Danh sách
     * được sao chép để người gọi sửa danh sách gốc về sau không làm đổi state.
     */
    public void setCategories(@NonNull List<Category> list) {
        categories.setValue(List.copyOf(list));
    }

    /**
     * Đặt bộ lọc mới và cho biết nó có khác bộ lọc đang áp dụng không, để ViewModel quyết định có tải lại danh sách hay
     * không. <code>newCriteria</code> được giữ nguyên tham chiếu, nên người gọi không được sửa nó sau đó.
     *
     * @return <code>true</code> nếu <code>newCriteria</code> khác bộ lọc cũ theo {@link BookCriteria#equals}
     */
    public boolean apply(@NonNull BookCriteria newCriteria) {
        boolean changed = !newCriteria.equals(criteria);
        criteria = newCriteria;
        return changed;
    }
}
