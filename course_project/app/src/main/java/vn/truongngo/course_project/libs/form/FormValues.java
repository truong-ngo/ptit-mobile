package vn.truongngo.course_project.libs.form;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;

import vn.truongngo.course_project.libs.util.ClassUtils;

/**
 * Chuyển chữ người dùng gõ sang giá trị của field và ngược lại. Hỗ trợ <code>String</code>, số nguyên, số thực,
 * <code>BigDecimal</code>, <code>char</code> và enum (theo tên hằng). Không dùng lớp Android nào nên kiểm thử được trên JVM.
 */
final class FormValues {

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private FormValues() {
    }

    /**
     * Đổi chữ sang giá trị kiểu <code>type</code>. Chữ rỗng hoặc toàn khoảng trắng cho <code>null</code> để
     * <code>@NotNull</code> bắt lỗi "bắt buộc nhập"; chuỗi được cắt khoảng trắng hai đầu.
     *
     * @throws IllegalArgumentException nếu chữ không đúng định dạng của kiểu, hoặc kiểu chưa được hỗ trợ
     */
    @Nullable
    static Object parse(@Nullable String text, @NonNull Class<?> type) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String value = text.trim();
        Class<?> wrapper = ClassUtils.resolveWrapperIfNecessary(type);

        if (wrapper == String.class) {
            return value;
        }
        if (wrapper == Integer.class) {
            return Integer.valueOf(value);
        }
        if (wrapper == Long.class) {
            return Long.valueOf(value);
        }
        if (wrapper == Short.class) {
            return Short.valueOf(value);
        }
        if (wrapper == Byte.class) {
            return Byte.valueOf(value);
        }
        if (wrapper == Double.class) {
            return Double.valueOf(value);
        }
        if (wrapper == Float.class) {
            return Float.valueOf(value);
        }
        if (wrapper == BigDecimal.class) {
            return new BigDecimal(value);
        }
        if (wrapper == Character.class) {
            if (value.length() != 1) {
                throw new IllegalArgumentException("Exactly one character is required");
            }
            return value.charAt(0);
        }
        if (wrapper.isEnum()) {
            return parseEnum(wrapper, value);
        }
        throw new IllegalArgumentException("Type " + type.getName() + " is not supported for text input");
    }

    /** Đổi giá trị sang chữ đưa lên ô nhập: <code>null</code> thành chuỗi rỗng, số thực bỏ đuôi <code>.0</code> thừa. */
    @NonNull
    static String format(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof Double || value instanceof Float) {
            return BigDecimal.valueOf(((Number) value).doubleValue()).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    /** Mô tả ngắn kiểu dữ liệu, để ghép vào thông báo "sai định dạng". */
    @NonNull
    static String describe(@NonNull Class<?> type) {
        Class<?> wrapper = ClassUtils.resolveWrapperIfNecessary(type);
        if (wrapper == Integer.class || wrapper == Long.class || wrapper == Short.class || wrapper == Byte.class) {
            return "integer";
        }
        if (wrapper == Double.class || wrapper == Float.class || wrapper == BigDecimal.class) {
            return "number";
        }
        if (wrapper == Character.class) {
            return "single character";
        }
        return "value";
    }

    /** Tách riêng để gom cảnh báo ép kiểu: <code>Enum.valueOf</code> cần kiểu enum cụ thể mà ở đây chỉ có <code>Class&lt;?&gt;</code>. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Class<?> enumType, String value) {
        return Enum.valueOf((Class<? extends Enum>) enumType, value);
    }
}
