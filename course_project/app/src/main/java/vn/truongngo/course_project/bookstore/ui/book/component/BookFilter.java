package vn.truongngo.course_project.bookstore.ui.book.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.BookCondition;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.bookstore.ui.book.model.BookCriteria;
import vn.truongngo.course_project.libs.base.BaseLayout;
import vn.truongngo.course_project.libs.util.Keyboard;
import vn.truongngo.course_project.libs.util.ViewBinder;

/**
 * <p>Khối lọc sách: nhận bộ lọc đang áp dụng qua {@link #render} và báo bộ lọc mới qua {@link #setOnApply}. Khối này
 * không biết ViewModel hay màn hình chứa nó, nên nơi dùng là bên nối hai phía.</p>
 *
 * <p>Thể loại do nơi dùng đưa vào qua {@link #setCategories}, khối này không tự nạp. Cho tới khi có danh sách, spinner
 * thể loại chỉ có mục "Tất cả" và {@link BookCriteria#categoryId} là <code>null</code>.</p>
 *
 * <p>Adapter của spinner được gán ngay trong constructor chứ không ở {@link #initViews()}, vì <code>initViews</code> chỉ
 * chạy khi View được gắn vào cửa sổ (sau <code>onViewCreated</code> của Fragment), còn Fragment có thể gọi
 * {@link #render} trước đó.</p>
 */
public class BookFilter extends BaseLayout<BookFilterViews> {

    /** Nơi nhận bộ lọc khi người dùng bấm Lọc hoặc Xóa lọc. Null khi chưa có ai nghe. */
    private Consumer<BookCriteria> onApply;

    /**
     * Thể loại đang có trong spinner. Vị trí <code>i + 1</code> của spinner ứng với <code>categories.get(i)</code>, vị
     * trí 0 là "Tất cả"; cần giữ lại vì từ chữ trên màn hình không suy ra được <code>categoryId</code>.
     */
    private List<Category> categories = Collections.emptyList();

    /** Constructor khi tạo bằng code. */
    public BookFilter(@NonNull Context context) {
        this(context, null);
    }

    /** Constructor khi được inflate từ XML. */
    public BookFilter(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /** Constructor đầy đủ; mọi constructor đều về đây. Nạp spinner và tắt khôi phục state ngay, xem mô tả lớp. */
    public BookFilter(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupSpinners();
        setCategories(Collections.emptyList(), null);
        disableStateRestore();
    }

    @Override
    protected int layoutResId() {
        return R.layout.layout_book_filter;
    }

    @NonNull
    @Override
    protected BookFilterViews bindViews(@NonNull View root) {
        return ViewBinder.create(BookFilterViews.class, root);
    }

    /** Không còn gì để cấu hình lần đầu: spinner đã được nạp ở constructor. */
    @Override
    protected void initViews() {
    }

    @Override
    protected void initEvents() {
        views.submitButton.setOnClickListener(v -> apply());
        views.clearButton.setOnClickListener(v -> clear());
    }

    /** Đặt nơi nhận bộ lọc khi người dùng bấm Lọc, hoặc Xóa lọc (khi đó nhận bộ lọc rỗng). */
    public void setOnApply(@Nullable Consumer<BookCriteria> onApply) {
        this.onApply = onApply;
    }

    /**
     * Đổ bộ lọc đang áp dụng vào các ô, thay cho nội dung đang gõ. Gọi được bất cứ lúc nào sau constructor, kể cả
     * trước khi View được gắn vào cửa sổ.
     */
    public void render(@NonNull BookCriteria criteria) {
        selectCategory(criteria.categoryId);
        views.conditionFilter.setSelection(criteria.condition == null ? 0 : criteria.condition.ordinal() + 1);
        views.minPriceFilter.setText(criteria.minPrice == null ? "" : String.valueOf(criteria.minPrice));
        views.maxPriceFilter.setText(criteria.maxPrice == null ? "" : String.valueOf(criteria.maxPrice));
        views.maxPriceFilter.setError(null);
    }

    /**
     * <p>Tắt việc Android tự lưu và khôi phục nội dung bốn ô lọc, để các ô luôn lấy giá trị từ {@link #render} (tức từ
     * ViewModel). Nếu để Android khôi phục thì khi tiến trình bị hệ thống giết rồi dựng lại, ô lọc hiện bộ lọc cũ trong khi
     * ViewModel mới là bộ lọc rỗng, nên danh sách không lọc mà ô lại như đang lọc.</p>
     *
     * <p>Đổi lại, bản nháp chưa bấm Lọc không còn sống sót qua xoay màn hình; bộ lọc <b>đã áp dụng</b> thì vẫn được giữ
     * vì nằm trong ViewModel.</p>
     */
    private void disableStateRestore() {
        views.categoryFilter.setSaveEnabled(false);
        views.conditionFilter.setSaveEnabled(false);
        views.minPriceFilter.setSaveEnabled(false);
        views.maxPriceFilter.setSaveEnabled(false);
    }

    /**
     * Nạp lựa chọn cho spinner tình trạng. Mục đầu là "Tất cả" (vị trí 0), các mục sau theo thứ tự khai báo của
     * {@link BookCondition}, nên vị trí trừ một chính là <code>ordinal</code>. Spinner thể loại được nạp riêng ở
     * {@link #setCategories} vì danh sách của nó đến từ bên ngoài.
     */
    private void setupSpinners() {
        String all = getContext().getString(R.string.book_filter_all);
        List<String> conditions = new ArrayList<>();
        conditions.add(all);
        for (BookCondition condition : BookCondition.values()) {
            conditions.add(condition.getLabel());
        }
        setItems(views.conditionFilter, conditions);
    }

    /**
     * Thay danh sách thể loại của spinner và chọn lại thể loại <code>selectedId</code>, vì gán adapter mới đưa lựa chọn
     * về "Tất cả". Gọi lại được nhiều lần, ví dụ khi thể loại nạp xong hoặc khi View dựng lại. Gọi được bất cứ lúc nào
     * sau constructor, kể cả trước khi View được gắn vào cửa sổ.
     *
     * @param selectedId thể loại cần chọn, thường là bộ lọc đang áp dụng; <code>null</code> hoặc không có trong danh sách
     *                   thì chọn "Tất cả"
     */
    public void setCategories(@NonNull List<Category> categories, @Nullable Long selectedId) {
        this.categories = new ArrayList<>(categories);
        List<String> items = new ArrayList<>();
        items.add(getContext().getString(R.string.book_filter_all));
        for (Category category : categories) {
            items.add(category.getName());
        }
        setItems(views.categoryFilter, items);
        selectCategory(selectedId);
    }

    /** Chọn mục của thể loại có id này; không tìm thấy (hoặc <code>null</code>) thì chọn "Tất cả". */
    private void selectCategory(@Nullable Long categoryId) {
        int position = 0;
        for (int i = 0; i < categories.size(); i++) {
            if (Objects.equals(categories.get(i).getId(), categoryId)) {
                position = i + 1;
                break;
            }
        }
        views.categoryFilter.setSelection(position);
    }

    /** Id của thể loại đang chọn, hoặc <code>null</code> khi chọn "Tất cả". */
    @Nullable
    private Long selectedCategoryId() {
        int position = views.categoryFilter.getSelectedItemPosition();
        return position <= 0 ? null : categories.get(position - 1).getId();
    }

    /** Gán danh sách chữ cho một spinner. */
    private void setItems(Spinner spinner, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /** Đọc các ô thành bộ lọc rồi báo ra ngoài. Giá từ lớn hơn giá đến thì báo lỗi ở ô giá đến và không áp dụng. */
    private void apply() {
        Long min = parsePrice(views.minPriceFilter);
        Long max = parsePrice(views.maxPriceFilter);
        if (min != null && max != null && min > max) {
            views.maxPriceFilter.setError(getContext().getString(R.string.book_filter_price_range_error));
            views.maxPriceFilter.requestFocus();
            return;
        }

        BookCriteria criteria = new BookCriteria();
        criteria.categoryId = selectedCategoryId();
        criteria.condition = selectedCondition();
        criteria.minPrice = min;
        criteria.maxPrice = max;
        publish(criteria);
    }

    /** Đưa các ô về trạng thái rỗng và áp dụng ngay bộ lọc rỗng. */
    private void clear() {
        BookCriteria empty = new BookCriteria();
        render(empty);
        publish(empty);
    }

    /** Ẩn bàn phím rồi báo bộ lọc cho người nghe, nếu có. */
    private void publish(BookCriteria criteria) {
        Keyboard.hide(this);
        if (onApply != null) {
            onApply.accept(criteria);
        }
    }

    /** Tình trạng đang chọn, hoặc <code>null</code> khi chọn "Tất cả". */
    @Nullable
    private BookCondition selectedCondition() {
        int position = views.conditionFilter.getSelectedItemPosition();
        return position <= 0 ? null : BookCondition.values()[position - 1];
    }

    /**
     * Số trong ô giá, hoặc <code>null</code> khi để trống. Ô chỉ nhận chữ số và tối đa 12 ký tự (khai báo ở XML) nên
     * luôn vừa trong <code>long</code>.
     */
    @Nullable
    private static Long parsePrice(EditText field) {
        String text = field.getText().toString().trim();
        return text.isEmpty() ? null : Long.valueOf(text);
    }
}
