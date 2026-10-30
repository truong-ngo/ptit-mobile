package vn.truongngo.course_project.libs.form;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import vn.truongngo.course_project.libs.form.annotation.FormField;
import vn.truongngo.course_project.libs.form.validation.FormValidator;
import vn.truongngo.course_project.libs.form.validation.Max;
import vn.truongngo.course_project.libs.form.validation.Min;
import vn.truongngo.course_project.libs.form.validation.NotNull;
import vn.truongngo.course_project.libs.form.validation.Pattern;
import vn.truongngo.course_project.libs.form.validation.SelfValidating;
import vn.truongngo.course_project.libs.form.validation.Size;

/**
 * Kiểm thử phần lõi thuần Java của form (siêu dữ liệu, chuyển chữ ↔ kiểu, bộ kiểm tra) trên JVM.
 * {@code FormBinder} thao tác với View thật nên cần thiết bị, chưa được kiểm thử ở đây.
 */
public class FormCoreTest {

    enum Condition {TOT, KHA, CU}

    static class BookForm implements SelfValidating {
        @FormField(id = "etPrice", label = "Price", order = 3)
        @NotNull
        @Min(value = 0, inclusive = false, message = "Price must be greater than 0")
        @Max(10_000_000)
        long price;

        @FormField(id = "etTitle", label = "Title", order = 1)
        @NotNull
        @Size(min = 2, max = 10)
        String title;

        @FormField(label = "Code", order = 2, readOnly = true)
        String code;

        @FormField
        @Pattern(regexp = Pattern.EMAIL, message = "Invalid email")
        String email;

        @FormField
        Condition condition;

        String notAField;

        @Override
        public void validate() {
            if (title != null && title.equals(code)) {
                throw new IllegalArgumentException("Title must not equal the code");
            }
        }
    }

    static class NoFields {
        String a;
    }

    private static Field field(String name) throws NoSuchFieldException {
        return BookForm.class.getDeclaredField(name);
    }

    // ---- FormMeta ---------------------------------------------------------------------------------------

    @Test
    public void meta_sortedByOrderThenDeclaration_withDefaults() {
        List<FormMeta.FieldMeta> metas = FormMetaRegistry.of(BookForm.class);
        // order: title(1), code(2), price(3); email và condition order 0 nên đứng trước, giữ thứ tự khai báo
        assertEquals("email", metas.get(0).field().getName());
        assertEquals("condition", metas.get(1).field().getName());
        assertEquals("title", metas.get(2).field().getName());
        assertEquals("code", metas.get(3).field().getName());
        assertEquals("price", metas.get(4).field().getName());
        assertEquals(5, metas.size());

        FormMeta.FieldMeta title = metas.get(2);
        assertEquals("etTitle", title.idName());
        assertEquals("Title", title.label());
        assertFalse(title.readOnly());

        FormMeta.FieldMeta email = metas.get(0);
        assertEquals("id mặc định là tên field", "email", email.idName());
        assertEquals("nhãn mặc định là tên field", "email", email.label());
        assertTrue(metas.get(3).readOnly());
    }

    @Test
    public void registry_scansOncePerClassAndReusesTheResult() {
        assertSame(FormMetaRegistry.of(BookForm.class), FormMetaRegistry.of(BookForm.class));
        // scan không cache: mỗi lần gọi trực tiếp cho một danh sách mới
        assertNotSame(FormMeta.scan(BookForm.class), FormMetaRegistry.of(BookForm.class));
    }

    @Test
    public void meta_rejectsClassWithoutFormFields() {
        assertThrows(IllegalArgumentException.class, () -> FormMetaRegistry.of(NoFields.class));
    }

    @Test
    public void meta_getterSetter() {
        BookForm form = new BookForm();
        FormMeta.FieldMeta title = FormMetaRegistry.of(BookForm.class).get(2);
        title.setter().accept(form, "Số đỏ");
        assertEquals("Số đỏ", form.title);
        assertEquals("Số đỏ", title.getter().apply(form));
    }

    // ---- FormValues -------------------------------------------------------------------------------------

    @Test
    public void values_parse() {
        assertNull(FormValues.parse("   ", String.class));
        assertNull(FormValues.parse(null, long.class));
        assertEquals("Số đỏ", FormValues.parse("  Số đỏ  ", String.class));
        assertEquals(45000L, FormValues.parse(" 45000 ", long.class));
        assertEquals(7, FormValues.parse("7", int.class));
        assertEquals(1.5, FormValues.parse("1.5", double.class));
        assertEquals(new BigDecimal("12.30"), FormValues.parse("12.30", BigDecimal.class));
        assertEquals('x', FormValues.parse("x", char.class));
        assertEquals(Condition.KHA, FormValues.parse("KHA", Condition.class));
    }

    @Test
    public void values_parseRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> FormValues.parse("12a", long.class));
        assertThrows(IllegalArgumentException.class, () -> FormValues.parse("1.5", int.class));
        assertThrows(IllegalArgumentException.class, () -> FormValues.parse("xy", char.class));
        assertThrows(IllegalArgumentException.class, () -> FormValues.parse("NOPE", Condition.class));
        assertThrows(IllegalArgumentException.class, () -> FormValues.parse("x", Object.class));
    }

    @Test
    public void values_format() {
        assertEquals("", FormValues.format(null));
        assertEquals("45000", FormValues.format(45000L));
        assertEquals("100000", FormValues.format(100000.0));
        assertEquals("1.5", FormValues.format(1.5));
        assertEquals("12.30", FormValues.format(new BigDecimal("12.30")));
        assertEquals("TOT", FormValues.format(Condition.TOT));
    }

    // ---- FormValidator ----------------------------------------------------------------------------------

    @Test
    public void validator_notNullAndBlank() throws Exception {
        Optional<String> nullError = FormValidator.checkField(field("title"), "Title", null);
        assertEquals("Field 'Title' must not be empty.", nullError.orElse(null));
        assertTrue(FormValidator.checkField(field("title"), "Title", "   ").isPresent());
        assertFalse(FormValidator.checkField(field("title"), "Title", "Số đỏ").isPresent());
    }

    @Test
    public void validator_size() throws Exception {
        assertEquals("Field 'Title' must be between 2 and 10 characters long.",
                FormValidator.checkField(field("title"), "Title", "a").orElse(null));
        assertTrue(FormValidator.checkField(field("title"), "Title", "12345678901").isPresent());
        assertFalse(FormValidator.checkField(field("title"), "Title", "ab").isPresent());
    }

    @Test
    public void validator_minMaxWithCustomMessage() throws Exception {
        assertEquals("Price must be greater than 0", FormValidator.checkField(field("price"), "Price", 0L).orElse(null));
        assertFalse(FormValidator.checkField(field("price"), "Price", 1L).isPresent());
        assertEquals("Field 'Price' must be less than or equal to 10000000.",
                FormValidator.checkField(field("price"), "Price", 10_000_001L).orElse(null));
        assertFalse(FormValidator.checkField(field("price"), "Price", 10_000_000L).isPresent());
    }

    @Test
    public void validator_pattern_andEmptyOptionalFieldSkipsConstraints() throws Exception {
        assertEquals("Invalid email", FormValidator.checkField(field("email"), "Email", "khong-phai-email").orElse(null));
        assertFalse(FormValidator.checkField(field("email"), "Email", "a@b.com").isPresent());
        assertFalse("email không bắt buộc nên để trống là hợp lệ",
                FormValidator.checkField(field("email"), "Email", "").isPresent());
    }

    @Test
    public void validator_validateBean_stopsAtFirstViolationThenRunsSelfValidation() {
        BookForm form = new BookForm();
        form.title = "Số đỏ";
        form.price = 5000;
        form.code = "Số đỏ";
        IllegalArgumentException cross = assertThrows(IllegalArgumentException.class, () -> FormValidator.validateBean(form));
        assertEquals("Title must not equal the code", cross.getMessage());

        form.code = "S001";
        FormValidator.validateBean(form);

        form.title = null;
        IllegalArgumentException required = assertThrows(IllegalArgumentException.class, () -> FormValidator.validateBean(form));
        assertEquals("Field 'Title' must not be empty.", required.getMessage());
    }
}
