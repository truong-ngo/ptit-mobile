package vn.truongngo.course_project.libs.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Khai báo field là khóa ngoại trỏ tới khóa chính của một thực thể khác, để sinh
 * <code>FOREIGN KEY (...) REFERENCES ... ON DELETE ...</code> khi tạo bảng.</p>
 *
 * <p>Chỉ là ràng buộc ở tầng cơ sở dữ liệu: giá trị của field vẫn là khóa thường (ví dụ <code>Long categoryId</code>),
 * không tự nạp đối tượng được tham chiếu.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface References {

    /** Thực thể được tham chiếu; phải được đăng ký cùng <code>BaseDbHelper</code>. */
    Class<?> entity();

    /** Hành vi khi bản ghi cha bị xóa. */
    OnDelete onDelete() default OnDelete.RESTRICT;
}
