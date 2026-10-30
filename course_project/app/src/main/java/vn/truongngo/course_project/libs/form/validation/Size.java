package vn.truongngo.course_project.libs.form.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Ràng buộc độ dài của chuỗi ký tự ({@link CharSequence}) hoặc số lượng phần tử của
 * {@link java.util.Collection}, {@link java.util.Map} và mảng phải nằm trong đoạn {@code [min, max]}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Size {

    /**
     * Độ dài / số lượng phần tử tối thiểu (mặc định là {@code 0}).
     */
    int min() default 0;

    /**
     * Độ dài / số lượng phần tử tối đa (mặc định là {@link Integer#MAX_VALUE}).
     */
    int max() default Integer.MAX_VALUE;

    /**
     * Thông điệp lỗi tùy chỉnh hiển thị cho người dùng (nếu để trống sẽ tự động sinh thông báo chuẩn).
     */
    String message() default "";
}
