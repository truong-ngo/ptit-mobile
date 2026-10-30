package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Một trang kết quả. Không chứa tổng số bản ghi vì việc đó cần thêm một truy vấn <code>COUNT</code>: dùng
 * <code>Repository#count</code> khi cần hiện "trang 1/5". Load more chỉ cần {@link #hasNext()}.
 *
 * @param <E>     kiểu thực thể
 * @param content các dòng của trang này, chỉ đọc
 * @param page    số trang, bắt đầu từ 0
 * @param size    kích thước trang đã yêu cầu (không phải số dòng thực có)
 * @param hasNext còn trang sau không
 */
public record Page<E>(List<E> content, int page, int size, boolean hasNext) {

    /** Gói <code>content</code> thành danh sách chỉ đọc, để người nhận không sửa được kết quả. */
    public Page(@NonNull List<E> content, int page, int size, boolean hasNext) {
        this.content = Collections.unmodifiableList(content);
        this.page = page;
        this.size = size;
        this.hasNext = hasNext;
    }

    /**
     * @return các dòng của trang này (tối đa <code>size</code> dòng), chỉ đọc
     */
    @Override
    @NonNull
    public List<E> content() {
        return content;
    }

    /** Trang không có dòng nào. */
    public boolean isEmpty() {
        return content.isEmpty();
    }
}
