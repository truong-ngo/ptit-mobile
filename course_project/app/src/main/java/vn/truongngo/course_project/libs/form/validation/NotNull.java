package vn.truongngo.course_project.libs.form.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Ràng buộc trường dữ liệu không được {@code null} (và đối với chuỗi ký tự {@link CharSequence},
 * mặc định không được để trống hoặc chỉ chứa khoảng trắng).</p>
 *
 * <p>Gộp cả {@code @NotNull} và {@code @NotBlank} vào một annotation duy nhất để phù hợp với
 * đặc thù của {@code EditText} trên giao diện (vốn trả về chuỗi rỗng {@code ""} khi không nhập).</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNull {

    /**
     * Cho phép chuỗi ký tự chỉ chứa khoảng trắng hay không (mặc định là {@code false} &mdash; tức là bắt lỗi cả chuỗi trống).
     */
    boolean allowBlank() default false;

    /**
     * Thông điệp lỗi tùy chỉnh hiển thị cho người dùng (nếu để trống sẽ tự động sinh thông báo dựa trên {@code @FormField.label()}).
     */
    String message() default "";
}
