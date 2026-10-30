package vn.truongngo.course_project.libs.form.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Gắn một field của model với một ô nhập trong layout, để <code>FormBinder</code> đổ dữ liệu model lên view và đọc
 * ngược lại (có kiểm tra). View nằm sẵn trong XML nên annotation chỉ ra <b>id của view</b> chứ không sinh ô nhập.</p>
 *
 * <p>View và kiểu field được hỗ trợ: <code>EditText</code> (chuỗi, số, <code>BigDecimal</code>, <code>char</code>),
 * <code>CheckBox</code>/<code>Switch</code> (boolean), <code>Spinner</code> (enum, hoặc danh sách tự cung cấp qua
 * <code>FormBinder#choices</code>). Ràng buộc dữ liệu khai báo kèm bằng các annotation trong gói <code>form.validation</code>.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FormField {

    /** Tên id của view trong layout (phần sau <code>@+id/</code>). Để trống thì dùng tên field. */
    String id() default "";

    /**
     * Nhãn dùng trong thông báo lỗi, ví dụ "Field 'Title' must not be empty". Để trống thì dùng tên field. Chữ hiển thị
     * cạnh ô nhập do layout lo, không phải thuộc tính này.
     */
    String label() default "";

    /** Thứ tự xử lý, cũng là thứ tự chọn ô để đặt con trỏ khi nhiều ô lỗi. Số nhỏ trước; bằng nhau thì theo thứ tự khai báo. */
    int order() default 0;

    /** Chỉ hiển thị: không cho sửa, không kiểm tra và không ghi ngược vào model (ví dụ mã khi mở ở chế độ sửa). */
    boolean readOnly() default false;
}
