package vn.truongngo.course_project.libs.repository.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Tùy chỉnh cột bảng ánh xạ với field được đánh dấu. Field không gắn annotation này vẫn là cột (tên suy ra theo
 * <code>snake_case</code>); muốn loại một field thì khai báo <code>transient</code>.</p>
 *
 * <p><code>nullable</code> và <code>unique</code> dùng khi sinh câu <code>CREATE TABLE</code>
 * (<code>NOT NULL</code>, <code>UNIQUE</code>). Khóa chính luôn không null và bỏ qua hai thuộc tính này.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    /** Tên cột trong bảng SQL. Để trống thì suy ra từ tên field theo <code>snake_case</code>. */
    String name() default "";

    /** Cột có cho phép <code>NULL</code> không. */
    boolean nullable() default true;

    /** Giá trị trong cột có phải duy nhất trong bảng không (<code>UNIQUE</code>). */
    boolean unique() default false;
}
