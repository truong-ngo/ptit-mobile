package vn.truongngo.course_project.libs.util;

import org.jspecify.annotations.NonNull;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public final class Money {

    /** Locale cố định theo Việt Nam, không theo máy: số tiền luôn viết một kiểu dù máy đặt ngôn ngữ nào. */
    private static final Locale VIETNAM = Locale.forLanguageTag("vi-VN");

    /** Đơn vị tiền gắn sau số của {@link #formatVnd}. Là dữ liệu định dạng, không phải message nên viết tiếng Việt. */
    private static final String SUFFIX = " đ";

    /**
     * <code>NumberFormat</code> không an toàn đa luồng nên mỗi thread giữ một bản. Viết bằng lớp con thay vì
     * <code>ThreadLocal.withInitial</code> vì hàm đó cần API 26 (minSdk là 24).
     */
    private static final ThreadLocal<NumberFormat> FORMAT = new ThreadLocal<>() {
        @Override
        protected NumberFormat initialValue() {
            return NumberFormat.getIntegerInstance(VIETNAM);
        }
    };

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private Money() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Số có dấu chấm ngăn cách hàng nghìn theo cách viết của Việt Nam, ví dụ <code>45.000</code>. */
    @NonNull
    public static String format(long amount) {
        return Objects.requireNonNull(FORMAT.get()).format(amount);
    }

    /** Như {@link #format} kèm đơn vị, ví dụ <code>45.000 đ</code>. */
    @NonNull
    public static String formatVnd(long amount) {
        return format(amount) + SUFFIX;
    }

    /**
     * Đọc số tiền từ chuỗi người dùng nhập hoặc chuỗi đã định dạng: bỏ mọi ký tự không phải chữ số (dấu chấm, dấu cách,
     * đơn vị...), chỉ giữ dấu trừ ở đầu.
     *
     * @throws NumberFormatException nếu không có chữ số nào
     */
    public static long parse(@NonNull String text) {
        String trimmed = text.trim();
        StringBuilder digits = new StringBuilder();
        if (trimmed.startsWith("-")) {
            digits.append('-');
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }
        if (digits.toString().isEmpty() || (digits.length() == 1 && digits.charAt(0) == '-')) {
            throw new NumberFormatException("No digits in \"" + text + "\"");
        }
        return Long.parseLong(digits.toString());
    }
}
