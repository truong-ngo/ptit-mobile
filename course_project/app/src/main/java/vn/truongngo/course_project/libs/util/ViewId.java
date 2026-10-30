package vn.truongngo.course_project.libs.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Đánh dấu một field của container để {@link ViewBinder} gán view có id tương ứng trong layout.</p>
 *
 * <p>Giá trị là <b>chuỗi</b>, không phải <code>R.id.x</code>: từ AGP 8 <code>R.id.*</code> không còn là hằng số nên không dùng
 * được làm giá trị annotation. Đổi lại, gõ sai tên chỉ lộ lúc chạy, khi container được tạo.</p>
 *
 * <pre>
 * &#64;ViewId public EditText etEmail;              // tên field cũng là tên id: android:id="@+id/etEmail"
 * &#64;ViewId("btnLogin") public Button submit;     // tên field tự do, chỉ ra tên id bằng chuỗi
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ViewId {

    /** Tên id trong XML (phần sau <code>@+id/</code>). Bỏ trống thì dùng tên field. */
    String value() default "";
}
