package vn.truongngo.course_project.libs.form.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Ràng buộc chuỗi ký tự phải khớp với biểu thức chính quy ({@link #regexp()}).</p>
 *
 * <p>Tích hợp sẵn các hằng số biểu thức chính quy thông dụng như {@link #EMAIL} và {@link #PHONE}
 * để người dùng có thể viết {@code @Pattern(regexp = Pattern.EMAIL)} mà không cần tạo thêm annotation riêng.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pattern {

    /** Biểu thức chính quy chuẩn dùng để kiểm tra định dạng địa chỉ Email. */
    String EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /** Biểu thức chính quy chuẩn dùng để kiểm tra số điện thoại (9 đến 15 chữ số, có thể bắt đầu bằng dấu {@code +}). */
    String PHONE = "^\\+?[0-9]{9,15}$";

    /**
     * Biểu thức chính quy (Regular Expression) dùng để đối chiếu với giá trị chuỗi của trường.
     */
    String regexp();

    /**
     * Thông điệp lỗi tùy chỉnh hiển thị cho người dùng.
     */
    String message() default "";
}
