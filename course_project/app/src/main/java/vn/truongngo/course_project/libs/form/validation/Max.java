package vn.truongngo.course_project.libs.form.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ràng buộc giá trị số ({@link Number}: {@code int}, {@code long}, {@code double}, {@link java.math.BigDecimal}...)
 * phải nhỏ hơn hoặc bằng ({@link #inclusive()} {@code = true}) hoặc nhỏ hơn hẳn ({@link #inclusive()} {@code = false})
 * ngưỡng {@link #value()}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Max {

    /**
     * Giá trị cận trên cần kiểm tra (hỗ trợ cả số nguyên lẫn số thực).
     */
    double value();

    /**
     * Nếu {@code true} (mặc định) thì kiểm tra {@code <= value}; nếu {@code false} thì kiểm tra nghiêm ngặt {@code < value}.
     */
    boolean inclusive() default true;

    /**
     * Thông điệp lỗi tùy chỉnh hiển thị cho người dùng.
     */
    String message() default "";
}
