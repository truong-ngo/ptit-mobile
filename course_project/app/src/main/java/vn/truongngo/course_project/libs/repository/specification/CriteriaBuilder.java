package vn.truongngo.course_project.libs.repository.specification;

import vn.truongngo.course_project.libs.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Bộ xây dựng danh sách điều kiện lọc động ({@link Criteria}) theo phong cách Fluent API.</p>
 *
 * <p>Tự động bỏ qua các điều kiện có giá trị {@code null} hoặc chuỗi rỗng (trừ {@link Operator#IS_NULL} và
 * {@link Operator#IS_NOT_NULL}), giúp tầng giao diện có thể truyền trực tiếp giá trị từ các ô tìm kiếm
 * mà không cần viết hàng loạt câu lệnh {@code if (keyword != null)}.</p>
 *
 * <p>Ví dụ sử dụng:</p>
 * <pre>{@code
 * List<Criteria> criteria = CriteriaBuilder.builder()
 *     .where("name").contains(txtName.getText())
 *     .where("fee").isBetween(minFee, maxFee)
 *     .build();
 * }</pre>
 */
public final class CriteriaBuilder {

    /** Danh sách các điều kiện lọc đã thu thập. */
    private final List<Criteria> criteriaList = new ArrayList<>();

    private CriteriaBuilder() {
    }

    /**
     * Khởi tạo một {@link CriteriaBuilder} mới.
     */
    public static CriteriaBuilder builder() {
        return new CriteriaBuilder();
    }

    /**
     * Bắt đầu khai báo điều kiện lọc cho trường {@code field}.
     *
     * @param field tên trường trên lớp Entity (không được rỗng)
     * @return đối tượng {@link WhereClause} để chọn toán tử so sánh
     */
    public WhereClause where(String field) {
        Assert.hasText(field, "field cannot be blank");
        return new WhereClause(this, field.trim());
    }

    /**
     * Thêm một điều kiện lọc vào danh sách nếu giá trị hợp lệ (hoặc toán tử là {@code IS_NULL} / {@code IS_NOT_NULL}).
     *
     * @param field    tên trường
     * @param operator toán tử so sánh
     * @param value    giá trị so sánh
     * @return chính đối tượng {@link CriteriaBuilder} hiện tại
     */
    public CriteriaBuilder add(String field, Operator operator, Object value) {
        Assert.hasText(field, "field cannot be blank");
        Assert.notNull(operator, "operator cannot be null");

        if (operator == Operator.IS_NULL || operator == Operator.IS_NOT_NULL) {
            criteriaList.add(new Criteria(field, operator, null));
            return this;
        }

        if (value != null && !(value instanceof CharSequence cs && cs.toString().trim().isEmpty())) {
            criteriaList.add(new Criteria(field, operator, value));
        }
        return this;
    }

    /**
     * Hoàn tất và trả về danh sách bất biến chứa các {@link Criteria} đã xây dựng.
     */
    public List<Criteria> build() {
        return Collections.unmodifiableList(criteriaList);
    }
}
