package vn.truongngo.course_project.libs.form.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Ràng buộc giá trị số ({@link Number}: {@code int}, {@code long}, {@code double}, {@link java.math.BigDecimal}...)
 * phải lớn hơn hoặc bằng ({@link #inclusive()} {@code = true}) hoặc lớn hơn hẳn ({@link #inclusive()} {@code = false})
 * ngưỡng {@link #value()}.</p>
 *
 * <p>Nhờ cờ {@link #inclusive()}, annotation này thay thế luôn cho cả {@code @Positive}
 * (khi viết {@code @Min(value = 0, inclusive = false)}) và {@code @PositiveOrZero} (khi viết {@code @Min(0)}).</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Min {

    /**
     * Giá trị cận dưới cần kiểm tra (hỗ trợ cả số nguyên lẫn số thực).
     */
    double value();

    /**
     * Nếu {@code true} (mặc định) thì kiểm tra {@code >= value}; nếu {@code false} thì kiểm tra nghiêm ngặt {@code > value}.
     */
    boolean inclusive() default true;

    /**
     * Thông điệp lỗi tùy chỉnh hiển thị cho người dùng.
     */
    String message() default "";
}
