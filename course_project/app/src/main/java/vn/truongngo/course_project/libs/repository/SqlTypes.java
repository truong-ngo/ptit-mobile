package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Date;

import vn.truongngo.course_project.libs.util.ClassUtils;

/**
 * <p>Ánh xạ kiểu Java với kiểu lưu trữ SQLite và chuyển giá trị qua lại. Không dùng lớp Android nào nên kiểm thử được
 * trên JVM.</p>
 *
 * <ul>
 *   <li><code>boolean</code>, <code>byte</code>, <code>short</code>, <code>int</code>, <code>long</code> &mdash;
 *       <code>INTEGER</code> (boolean lưu 0/1)</li>
 *   <li><code>float</code>, <code>double</code> &mdash; <code>REAL</code></li>
 *   <li><code>String</code>, <code>char</code>, <code>BigDecimal</code>, enum &mdash; <code>TEXT</code> (BigDecimal lưu chuỗi
 *       để không mất độ chính xác, enum lưu <b>tên hằng</b> chứ không phải <code>toString()</code>)</li>
 *   <li><code>java.util.Date</code> &mdash; <code>INTEGER</code> (số mili-giây)</li>
 *   <li><code>byte[]</code> &mdash; <code>BLOB</code></li>
 * </ul>
 *
 * <p><code>java.time</code> chưa được hỗ trợ vì chỉ có từ API 26 trong khi minSdk là 24.</p>
 */
final class SqlTypes {

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private SqlTypes() {
    }

    /** Kiểu lưu trữ cho kiểu Java, hoặc <code>null</code> nếu chưa hỗ trợ. */
    @Nullable
    static SqlType typeOf(@NonNull Class<?> javaType) {
        Class<?> type = ClassUtils.resolveWrapperIfNecessary(javaType);
        if (type == Boolean.class || type == Byte.class || type == Short.class || type == Integer.class || type == Long.class || type == Date.class) {
            return SqlType.INTEGER;
        }
        if (type == Float.class || type == Double.class) {
            return SqlType.REAL;
        }
        if (type == String.class || type == Character.class || type == BigDecimal.class || type.isEnum()) {
            return SqlType.TEXT;
        }
        if (type == byte[].class) {
            return SqlType.BLOB;
        }
        return null;
    }

    /**
     * Đổi giá trị Java sang giá trị lưu: <code>Long</code>, <code>Double</code>, <code>String</code>, <code>byte[]</code>
     * hoặc <code>null</code>.
     *
     * @throws IllegalArgumentException nếu kiểu của giá trị chưa được hỗ trợ
     */
    @Nullable
    static Object toDb(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        // BigDecimal phải kiểm tra trước nhóm Number thông thường.
        if (value instanceof Boolean) {
            return (Boolean) value ? 1L : 0L;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof Float || value instanceof Double) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof Character) {
            return String.valueOf(value);
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof String || value instanceof byte[]) {
            return value;
        }
        throw new IllegalArgumentException("Type " + value.getClass().getName() + " is not supported");
    }

    /**
     * Đổi giá trị Java sang chuỗi tham số cho câu truy vấn, vì <code>selectionArgs</code> của SQLite chỉ nhận chuỗi. So
     * sánh số với cột INTEGER/REAL vẫn đúng nhờ quy tắc affinity của SQLite.
     *
     * @throws IllegalArgumentException nếu là <code>byte[]</code> hoặc kiểu chưa hỗ trợ
     */
    @Nullable
    static String toArg(@Nullable Object value) {
        Object db = toDb(value);
        if (db == null) {
            return null;
        }
        if (db instanceof byte[]) {
            throw new IllegalArgumentException("byte[] cannot be used as a filter argument");
        }
        return db.toString();
    }

    /**
     * Đổi giá trị đọc từ cột (<code>Long</code>, <code>Double</code>, <code>String</code> hoặc <code>byte[]</code>) sang
     * kiểu của field Java.
     *
     * @throws IllegalArgumentException nếu kiểu field chưa được hỗ trợ
     * @throws IllegalStateException    nếu cột kiểu <code>char</code> không có đúng một ký tự
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object fromDb(@NonNull Object raw, @NonNull Class<?> javaType) {
        Class<?> type = ClassUtils.resolveWrapperIfNecessary(javaType);
        if (type == Boolean.class) {
            return ((Number) raw).longValue() != 0;
        }
        if (type == Byte.class) {
            return (byte) ((Number) raw).longValue();
        }
        if (type == Short.class) {
            return (short) ((Number) raw).longValue();
        }
        if (type == Integer.class) {
            return (int) ((Number) raw).longValue();
        }
        if (type == Long.class) {
            return ((Number) raw).longValue();
        }
        if (type == Float.class) {
            return ((Number) raw).floatValue();
        }
        if (type == Double.class) {
            return ((Number) raw).doubleValue();
        }
        if (type == Date.class) {
            return new Date(((Number) raw).longValue());
        }
        if (type == BigDecimal.class) {
            return new BigDecimal((String) raw);
        }
        if (type == Character.class) {
            String text = (String) raw;
            if (text.length() != 1) {
                throw new IllegalStateException("A char column must hold exactly one character, got: '" + text + "'");
            }
            return text.charAt(0);
        }
        if (type == String.class || type == byte[].class) {
            return raw;
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) type, (String) raw);
        }
        throw new IllegalArgumentException("Type " + javaType.getName() + " is not supported");
    }
}
