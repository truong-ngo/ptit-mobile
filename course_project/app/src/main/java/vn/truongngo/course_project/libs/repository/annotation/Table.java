package vn.truongngo.course_project.libs.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Đánh dấu một lớp Java là một thực thể cơ sở dữ liệu (Entity) và ánh xạ nó với tên bảng SQL tương ứng.</p>
 *
 * <p>Nếu thuộc tính {@link #name()} bị bỏ trống, {@link vn.truongngo.course_project.libs.repository.BaseSqliteRepository}
 * sẽ tự động suy luận tên bảng bằng cách chuyển tên lớp từ {@code PascalCase} sang {@code snake_case}.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

    /**
     * Tên bảng trong cơ sở dữ liệu (nếu để trống sẽ tự động chuyển tên lớp sang {@code snake_case}).
     */
    String name() default "";
}
