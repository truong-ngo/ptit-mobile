package vn.truongngo.course_project.bookstore.ui.book.model;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import vn.truongngo.course_project.bookstore.domain.BookCondition;
import vn.truongngo.course_project.libs.repository.specification.Criteria;
import vn.truongngo.course_project.libs.repository.specification.CriteriaBuilder;

/**
 * <p>Điều kiện lọc danh sách sách. Mỗi field <code>null</code> nghĩa là không lọc theo tiêu chí đó, và các điều kiện
 * còn lại nối bằng AND.</p>
 *
 * <p>{@link #equals} so sánh theo giá trị để ViewModel biết bộ lọc vừa nhận có khác bộ lọc đang áp dụng không (cùng giá
 * trị thì khỏi tải lại). Vì thế object chỉ nên coi là bất biến sau khi giao cho ViewModel: sửa field tại chỗ sẽ làm phép
 * so sánh sai.</p>
 */
public class BookCriteria {

    /** Mã thể loại, <code>null</code> là mọi thể loại. */
    public Long categoryId;

    /** Tình trạng, <code>null</code> là mọi tình trạng. */
    public BookCondition condition;

    /** Giá thấp nhất, tính cả mốc (<code>&gt;=</code>), <code>null</code> là không giới hạn dưới. */
    public Long minPrice;

    /** Giá cao nhất, tính cả mốc (<code>&lt;=</code>), <code>null</code> là không giới hạn trên. */
    public Long maxPrice;

    /** Không có tiêu chí nào, tức lọc ra mọi sách. */
    public boolean isEmpty() {
        return Objects.isNull(categoryId) &&
               Objects.isNull(condition) &&
               Objects.isNull(minPrice) &&
               Objects.isNull(maxPrice);
    }

    /** Chuyển các tiêu chí khác <code>null</code> thành danh sách điều kiện cho repository; rỗng nếu không lọc gì. */
    @NonNull
    public List<Criteria> toSqlCriteria() {
        if (isEmpty()) return Collections.emptyList();
        CriteriaBuilder cb = CriteriaBuilder.builder();
        if (Objects.nonNull(categoryId)) cb = cb.where("categoryId").isEqualTo(categoryId);
        if (Objects.nonNull(condition)) cb = cb.where("condition").isEqualTo(condition.name());
        if (Objects.nonNull(minPrice)) cb = cb.where("price").isGreaterThanOrEqualTo(minPrice);
        if (Objects.nonNull(maxPrice)) cb = cb.where("price").isLessThanOrEqualTo(maxPrice);
        return cb.build();
    }

    /** So sánh theo giá trị của bốn tiêu chí, xem lý do ở mô tả lớp. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BookCriteria other)) {
            return false;
        }
        return Objects.equals(categoryId, other.categoryId)
                && condition == other.condition
                && Objects.equals(minPrice, other.minPrice)
                && Objects.equals(maxPrice, other.maxPrice);
    }

    /** Nhất quán với {@link #equals}. */
    @Override
    public int hashCode() {
        return Objects.hash(categoryId, condition, minPrice, maxPrice);
    }
}
