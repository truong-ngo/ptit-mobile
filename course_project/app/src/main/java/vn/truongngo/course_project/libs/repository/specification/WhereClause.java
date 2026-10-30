package vn.truongngo.course_project.libs.repository.specification;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>Đối tượng trung gian cung cấp giao diện Fluent API để định nghĩa điều kiện so sánh cho một trường cụ thể
 * khi gọi {@link CriteriaBuilder#where(String)}.</p>
 *
 * <p>Hỗ trợ các phép toán so sánh lớn/nhỏ ({@link #isGreaterThan}, {@link #isLessThan}...)
 * nhận kiểu {@link Comparable}, cho phép lọc trực tiếp theo cả số lẫn ngày tháng
 * ({@link java.time.LocalDate}, {@link java.time.LocalDateTime}, {@link java.util.Date}).</p>
 */
public final class WhereClause {

    /** Bộ xây dựng danh sách điều kiện gốc. */
    private final CriteriaBuilder builder;

    /** Tên trường trên lớp Entity đang được áp dụng điều kiện. */
    private final String field;

    WhereClause(CriteriaBuilder builder, String field) {
        this.builder = builder;
        this.field = field;
    }

    /**
     * Điều kiện bằng ({@code field = value}). Tự động bỏ qua nếu {@code value} là {@code null} hoặc chuỗi rỗng.
     */
    public CriteriaBuilder isEqualTo(Object value) {
        return builder.add(this.field, Operator.EQ, value);
    }

    /**
     * Điều kiện khác ({@code field != value}). Tự động bỏ qua nếu {@code value} là {@code null} hoặc chuỗi rỗng.
     */
    public CriteriaBuilder isNotEqualTo(Object value) {
        return builder.add(this.field, Operator.NE, value);
    }

    /**
     * Điều kiện chứa chuỗi con ({@code field LIKE '%value%'}). Tự động bỏ qua nếu {@code value} là {@code null} hoặc trống.
     */
    public CriteriaBuilder contains(String value) {
        if (value != null && !value.trim().isEmpty()) {
            return builder.add(this.field, Operator.LIKE, "%" + value.trim() + "%");
        }
        return builder;
    }

    /**
     * Điều kiện bắt đầu bằng tiền tố ({@code field LIKE 'prefix%'}).
     */
    public CriteriaBuilder startsWith(String prefix) {
        if (prefix != null && !prefix.trim().isEmpty()) {
            return builder.add(this.field, Operator.LIKE, prefix.trim() + "%");
        }
        return builder;
    }

    /**
     * Điều kiện kết thúc bằng hậu tố ({@code field LIKE '%suffix'}).
     */
    public CriteriaBuilder endsWith(String suffix) {
        if (suffix != null && !suffix.trim().isEmpty()) {
            return builder.add(this.field, Operator.LIKE, "%" + suffix.trim());
        }
        return builder;
    }

    /**
     * Điều kiện lớn hơn ({@code field > value}), hỗ trợ mọi kiểu {@link Comparable} (Số, Ngày giờ, Chuỗi).
     */
    public CriteriaBuilder isGreaterThan(Comparable<?> value) {
        return builder.add(this.field, Operator.GT, value);
    }

    /**
     * Điều kiện lớn hơn hoặc bằng ({@code field >= value}), hỗ trợ mọi kiểu {@link Comparable} (Số, Ngày giờ, Chuỗi).
     */
    public CriteriaBuilder isGreaterThanOrEqualTo(Comparable<?> value) {
        return builder.add(this.field, Operator.GTE, value);
    }

    /**
     * Điều kiện nhỏ hơn ({@code field < value}), hỗ trợ mọi kiểu {@link Comparable} (Số, Ngày giờ, Chuỗi).
     */
    public CriteriaBuilder isLessThan(Comparable<?> value) {
        return builder.add(this.field, Operator.LT, value);
    }

    /**
     * Điều kiện nhỏ hơn hoặc bằng ({@code field <= value}), hỗ trợ mọi kiểu {@link Comparable} (Số, Ngày giờ, Chuỗi).
     */
    public CriteriaBuilder isLessThanOrEqualTo(Comparable<?> value) {
        return builder.add(this.field, Operator.LTE, value);
    }

    /**
     * Điều kiện nằm trong tập hợp ({@code field IN (...)}).
     */
    public CriteriaBuilder in(Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            return builder.add(this.field, Operator.IN, values);
        }
        return builder;
    }

    /**
     * <p>Điều kiện nằm trong đoạn {@code [startValue, endValue]}.</p>
     *
     * <p>Linh hoạt cho các bộ lọc tìm kiếm trên UI (Từ - Đến): nếu người dùng chỉ nhập {@code startValue} thì tự động
     * hạ xuống {@code >= startValue}; nếu chỉ nhập {@code endValue} thì hạ xuống {@code <= endValue};
     * nếu nhập cả hai thì sinh {@code BETWEEN startValue AND endValue}.</p>
     */
    public CriteriaBuilder isBetween(Comparable<?> startValue, Comparable<?> endValue) {
        if (startValue != null && endValue != null) {
            return builder.add(this.field, Operator.BETWEEN, Arrays.asList(startValue, endValue));
        }
        if (startValue != null) {
            return isGreaterThanOrEqualTo(startValue);
        }
        if (endValue != null) {
            return isLessThanOrEqualTo(endValue);
        }
        return builder;
    }

    /**
     * Điều kiện kiểm tra cột mang giá trị {@code NULL} ({@code field IS NULL}).
     */
    public CriteriaBuilder isNull() {
        return builder.add(this.field, Operator.IS_NULL, null);
    }

    /**
     * Điều kiện kiểm tra cột khác {@code NULL} ({@code field IS NOT NULL}).
     */
    public CriteriaBuilder isNotNull() {
        return builder.add(this.field, Operator.IS_NOT_NULL, null);
    }
}
