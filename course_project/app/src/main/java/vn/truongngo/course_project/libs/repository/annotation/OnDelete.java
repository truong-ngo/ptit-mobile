package vn.truongngo.course_project.libs.repository.annotation;

/**
 * Hành vi của khóa ngoại khi bản ghi cha bị xóa, dùng trong {@link References#onDelete()}.
 */
public enum OnDelete {

    /** Cấm xóa bản ghi cha nếu còn bản ghi con; việc xóa ném <code>SQLiteConstraintException</code>. */
    RESTRICT("RESTRICT"),

    /** Đặt cột khóa ngoại của các bản ghi con về <code>NULL</code>; cột đó phải cho phép null. */
    SET_NULL("SET NULL"),

    /** Xóa luôn các bản ghi con. Nguy hiểm với dữ liệu thật: một lệnh xóa cha kéo theo mất cả nhánh con. */
    CASCADE("CASCADE"),

    /** Không làm gì ngay, việc kiểm tra khóa ngoại để tới cuối câu lệnh. */
    NO_ACTION("NO ACTION");

    /** Ký hiệu SQL của hành vi. */
    private final String sql;

    /** @param sql ký hiệu SQL dùng trong mệnh đề <code>ON DELETE</code> */
    OnDelete(String sql) {
        this.sql = sql;
    }

    /** Ký hiệu SQL tương ứng, dùng trong mệnh đề <code>ON DELETE</code>. */
    public String sql() {
        return sql;
    }
}
