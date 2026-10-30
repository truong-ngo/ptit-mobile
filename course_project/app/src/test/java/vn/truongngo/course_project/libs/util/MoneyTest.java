package vn.truongngo.course_project.libs.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/**
 * Kiểm thử {@link Money} trên JVM. Cách ngăn cách hàng nghìn do dữ liệu locale của JDK; trên Android lấy từ ICU
 * và cũng là dấu chấm cho vi-VN.
 */
public class MoneyTest {

    @Test
    public void format_usesDotAsThousandsSeparator() {
        assertEquals("0", Money.format(0));
        assertEquals("999", Money.format(999));
        assertEquals("45.000", Money.format(45_000));
        assertEquals("1.234.567", Money.format(1_234_567));
        assertEquals("-45.000", Money.format(-45_000));
    }

    @Test
    public void formatVnd_addsUnit() {
        assertEquals("100.000 đ", Money.formatVnd(100_000));
    }

    @Test
    public void parse_ignoresSeparatorsAndUnit() {
        assertEquals(45_000L, Money.parse("45.000 đ"));
        assertEquals(45_000L, Money.parse("  45000 "));
        assertEquals(1_234_567L, Money.parse("1.234.567"));
        assertEquals(-45_000L, Money.parse("-45.000"));
    }

    @Test
    public void parse_roundTripsFormat() {
        for (long amount : new long[]{0, 7, 1_000, 99_999, 2_500_000, Long.MAX_VALUE}) {
            assertEquals(amount, Money.parse(Money.formatVnd(amount)));
        }
    }

    @Test
    public void parse_rejectsTextWithoutDigits() {
        assertThrows(NumberFormatException.class, () -> Money.parse(""));
        assertThrows(NumberFormatException.class, () -> Money.parse("abc"));
        assertThrows(NumberFormatException.class, () -> Money.parse("-"));
    }
}
