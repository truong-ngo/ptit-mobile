package vn.truongngo.course_project.libs.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Đánh dấu trường khóa chính (Primary Key) của thực thể cơ sở dữ liệu.</p>
 *
 * <p>Hỗ trợ cả khóa chính tự tăng ({@link #autoIncrement()} {@code = true}, mặc định cho {@code Long}/{@code Integer})
 * lẫn khóa chính do ứng dụng tự gán ({@link #autoIncrement()} {@code = false}, ví dụ mã chuỗi {@code "KH001"} hoặc {@code UUID}).</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {

    /**
     * Xác định khóa chính có được cơ sở dữ liệu tự động sinh giá trị ({@code AUTO_INCREMENT}) khi {@code INSERT} hay không.
     */
    boolean autoIncrement() default true;
}
