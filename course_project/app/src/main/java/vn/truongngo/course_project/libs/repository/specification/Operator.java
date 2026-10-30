package vn.truongngo.course_project.libs.repository.specification;

/**
 * Tập hợp các toán tử so sánh SQL dùng để xây dựng mệnh đề {@code WHERE} động trong {@link Criteria}.
 */
public enum Operator {
    /** Bằng ({@code =}). */
    EQ("="),
    /** Khác ({@code !=}). */
    NE("!="),
    /** Lớn hơn ({@code >}). */
    GT(">"),
    /** Nhỏ hơn ({@code <}). */
    LT("<"),
    /** Lớn hơn hoặc bằng ({@code >=}). */
    GTE(">="),
    /** Nhỏ hơn hoặc bằng ({@code <=}). */
    LTE("<="),
    /** Tìm kiếm gần đúng theo mẫu chuỗi ({@code LIKE}). */
    LIKE("LIKE"),
    /** Thuộc tập hợp giá trị ({@code IN (...)}). */
    IN("IN"),
    /** Nằm trong đoạn hai giá trị ({@code BETWEEN ? AND ?}). */
    BETWEEN("BETWEEN"),
    /** Kiểm tra cột mang giá trị {@code NULL} ({@code IS NULL}). */
    IS_NULL("IS NULL"),
    /** Kiểm tra cột khác {@code NULL} ({@code IS NOT NULL}). */
    IS_NOT_NULL("IS NOT NULL");

    /** Ký hiệu toán tử SQL tương ứng. */
    private final String token;

    Operator(String token) {
        this.token = token;
    }

    /**
     * @return chuỗi ký hiệu SQL của toán tử
     */
    public String getToken() {
        return token;
    }
}
