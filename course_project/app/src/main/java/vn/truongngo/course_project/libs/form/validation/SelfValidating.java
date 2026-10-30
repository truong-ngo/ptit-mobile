package vn.truongngo.course_project.libs.form.validation;

/**
 * <p>Hợp đồng tùy chọn cho phép lớp mô hình giao diện (UI Row Model) tự thực hiện các kiểm tra chéo
 * giữa nhiều trường (cross-field validation) sau khi toàn bộ các annotation đơn lẻ
 * ({@link NotNull}, {@link Size}, {@link Min}, {@link Max}, {@link Pattern})
 * đã vượt qua thành công.</p>
 *
 * <p>Ví dụ: kiểm tra {@code endDate} phải lớn hơn hoặc bằng {@code startDate}, hoặc {@code maxParticipants >= minParticipants}.</p>
 */
@FunctionalInterface
public interface SelfValidating {

    /**
     * Thực hiện kiểm tra tính hợp lệ nội bộ của đối tượng.
     *
     * @throws IllegalArgumentException nếu dữ liệu vi phạm quy tắc kiểm tra (thông điệp ngoại lệ sẽ được hiển thị trực tiếp lên UI)
     */
    void validate();
}
