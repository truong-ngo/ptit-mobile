package vn.truongngo.course_project.libs.form.validation;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import vn.truongngo.course_project.libs.form.annotation.FormField;
import vn.truongngo.course_project.libs.util.Assert;
import vn.truongngo.course_project.libs.util.ReflectionUtils;
import vn.truongngo.course_project.libs.util.StringUtils;

public final class FormValidator {

    /** Biểu thức chính quy đã biên dịch, để không biên dịch lại ở mỗi lần bấm Lưu. */
    private static final Map<String, java.util.regex.Pattern> REGEX_CACHE = new ConcurrentHashMap<>();

    private FormValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Kiểm tra mọi field của một bean (rồi {@link SelfValidating#validate()} nếu bean cài đặt), nhãn trong thông báo lấy
     * từ {@link FormField#label()}.
     *
     * @throws IllegalArgumentException ngay tại field đầu tiên vi phạm, hoặc do {@link SelfValidating#validate()}
     */
    public static void validateBean(Object bean) {
        Assert.notNull(bean, "bean cannot be null");
        for (Field field : ReflectionUtils.findFields(bean.getClass(), f -> !Modifier.isStatic(f.getModifiers()))) {
            FormField formField = field.getAnnotation(FormField.class);
            String label = (formField != null && !formField.label().isEmpty()) ? formField.label() : field.getName();
            Object value = ReflectionUtils.getField(field, bean);
            checkField(field, label, value).ifPresent(message -> {
                throw new IllegalArgumentException(message);
            });
        }
        if (bean instanceof SelfValidating) {
            ((SelfValidating) bean).validate();
        }
    }

    /**
     * <p>Kiểm tra một field với giá trị <code>value</code> theo các annotation {@link NotNull}, {@link Size}, {@link Min},
     * {@link Max}, {@link Pattern} (theo thứ tự đó, dừng ở lỗi đầu tiên).</p>
     *
     * <p>Giá trị <code>null</code> hoặc chuỗi rỗng không bắt buộc thì bỏ qua mọi ràng buộc sau <code>@NotNull</code>. Khác bản ở
     * project server: không có <code>@Past</code>/<code>@Future</code> vì chúng dựa trên <code>java.time</code> (cần API 26, trong
     * khi minSdk là 24).</p>
     *
     * @param label nhãn dùng trong thông báo mặc định; rỗng thì dùng tên field
     * @return rỗng nếu hợp lệ, không thì thông báo lỗi (annotation có <code>message</code> riêng thì dùng nó)
     */
    public static Optional<String> checkField(Field field, String label, Object value) {
        Assert.notNull(field, "field cannot be null");
        String name = (label != null && !label.trim().isEmpty()) ? label : field.getName();

        // 1. @NotNull, kèm kiểm tra chuỗi chỉ toàn khoảng trắng
        NotNull notNull = field.getAnnotation(NotNull.class);
        if (notNull != null) {
            if (value == null) {
                return Optional.of(message(notNull.message(), "Field '" + name + "' must not be empty."));
            }
            if (!notNull.allowBlank() && value instanceof CharSequence && value.toString().trim().isEmpty()) {
                return Optional.of(message(notNull.message(), "Field '" + name + "' must not be empty."));
            }
        }

        // Giá trị null hoặc chuỗi rỗng không bắt buộc thì bỏ qua các ràng buộc phía sau.
        if (value == null || (value instanceof CharSequence sc && StringUtils.isEmpty(sc.toString()))) {
            return Optional.empty();
        }

        // 2. @Size
        Size size = field.getAnnotation(Size.class);
        if (size != null) {
            int length = sizeOf(value);
            if (length >= 0 && (length < size.min() || length > size.max())) {
                String fallback = (size.max() == Integer.MAX_VALUE)
                        ? "Field '" + name + "' must be at least " + size.min() + " characters long."
                        : "Field '" + name + "' must be between " + size.min() + " and " + size.max() + " characters long.";
                return Optional.of(message(size.message(), fallback));
            }
        }

        // 3. @Min
        Min min = field.getAnnotation(Min.class);
        if (min != null && value instanceof Number) {
            int cmp = toBigDecimal((Number) value).compareTo(BigDecimal.valueOf(min.value()));
            if (!(min.inclusive() ? cmp >= 0 : cmp > 0)) {
                String op = min.inclusive() ? "greater than or equal to " : "greater than ";
                return Optional.of(message(min.message(), "Field '" + name + "' must be " + op + formatNumber(min.value()) + "."));
            }
        }

        // 4. @Max
        Max max = field.getAnnotation(Max.class);
        if (max != null && value instanceof Number) {
            int cmp = toBigDecimal((Number) value).compareTo(BigDecimal.valueOf(max.value()));
            if (!(max.inclusive() ? cmp <= 0 : cmp < 0)) {
                String op = max.inclusive() ? "less than or equal to " : "less than ";
                return Optional.of(message(max.message(), "Field '" + name + "' must be " + op + formatNumber(max.value()) + "."));
            }
        }

        // 5. @Pattern
        Pattern pattern = field.getAnnotation(Pattern.class);
        if (pattern != null && value instanceof CharSequence) {
            java.util.regex.Pattern compiled = REGEX_CACHE.computeIfAbsent(pattern.regexp(), java.util.regex.Pattern::compile);
            if (!compiled.matcher((CharSequence) value).matches()) {
                return Optional.of(message(pattern.message(), "Field '" + name + "' does not match the required format."));
            }
        }

        return Optional.empty();
    }

    /** Độ dài của chuỗi, collection, map hoặc mảng; <code>-1</code> nếu kiểu giá trị không có khái niệm kích thước. */
    private static int sizeOf(Object value) {
        if (value instanceof CharSequence) {
            return ((CharSequence) value).length();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).size();
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        return -1;
    }

    /** Đổi mọi <code>Number</code> sang <code>BigDecimal</code> để so sánh không bị sai số dấu phẩy động. */
    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal) {
            return (BigDecimal) number;
        }
        if (number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long) {
            return BigDecimal.valueOf(number.longValue());
        }
        return BigDecimal.valueOf(number.doubleValue());
    }

    /** Bỏ đuôi <code>.0</code> của số nguyên khi hiển thị trong thông báo. */
    private static String formatNumber(double value) {
        return value == (long) value ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** Ưu tiên thông báo tùy chỉnh của annotation; để trống thì dùng thông báo mặc định. */
    private static String message(String custom, String fallback) {
        return (custom != null && !custom.trim().isEmpty()) ? custom : fallback;
    }
}
