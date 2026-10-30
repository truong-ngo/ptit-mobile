package vn.truongngo.course_project.libs.repository;

/**
 * Bốn lớp lưu trữ của SQLite mà thư viện dùng. Tên hằng chính là tên kiểu trong câu <code>CREATE TABLE</code>.
 */
enum SqlType {
    /** Số nguyên 64 bit; cũng lưu boolean (0/1) và ngày (mili-giây). */
    INTEGER,

    /** Số thực 64 bit. */
    REAL,

    /** Chuỗi; cũng lưu <code>char</code>, <code>BigDecimal</code> và enum. */
    TEXT,

    /** Dãy byte nguyên dạng. */
    BLOB
}
