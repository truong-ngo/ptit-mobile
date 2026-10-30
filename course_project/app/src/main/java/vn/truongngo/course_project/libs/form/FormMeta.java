package vn.truongngo.course_project.libs.form;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import vn.truongngo.course_project.libs.form.annotation.FormField;
import vn.truongngo.course_project.libs.util.ReflectionUtils;

/**
 * Các field gắn <code>@FormField</code> của một lớp model, đã sắp theo <code>order</code>, quét bằng reflection một lần rồi
 * cache. Không dùng lớp Android nào nên kiểm thử được trên JVM.
 */
final class FormMeta {

    /**
     * Một field của form cùng id view, nhãn, và cặp hàm đọc/ghi field.
     *
     * @param field    field của model
     * @param idName   tên id của view trong layout (đã thay bằng tên field nếu annotation để trống)
     * @param label    nhãn dùng trong thông báo lỗi (đã thay bằng tên field nếu annotation để trống)
     * @param readOnly chỉ hiển thị: không kiểm tra, không ghi ngược vào model
     * @param getter   đọc giá trị của field từ một đối tượng model
     * @param setter   ghi giá trị vào field của một đối tượng model
     */
    record FieldMeta(
            Field field,
            String idName,
            String label,
            boolean readOnly,
            Function<Object, Object> getter,
            BiConsumer<Object, Object> setter
    ) {
    }

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private FormMeta() {
    }

    /**
     * Quét các field gắn <code>@FormField</code> của <code>type</code> theo thứ tự <code>order</code> (bằng nhau thì theo
     * thứ tự khai báo); id và nhãn rỗng thì lấy tên field. <b>Không cache</b>: gọi {@link FormMetaRegistry#of} để được kết
     * quả đã cache.
     *
     * @throws IllegalArgumentException nếu lớp không có field nào gắn <code>@FormField</code>
     */
    @NonNull
    @SuppressWarnings("all")
    static List<FieldMeta> scan(@NonNull Class<?> type) {
        Field[] fields = ReflectionUtils.findFields(type, f -> f.isAnnotationPresent(FormField.class));
        if (fields.length == 0) {
            throw new IllegalArgumentException("Class " + type.getName() + " must have at least one field annotated with @FormField");
        }

        List<Field> sorted = new ArrayList<>(Arrays.asList(fields));
        sorted.sort(Comparator.comparingInt(f -> f.getAnnotation(FormField.class).order()));

        List<FieldMeta> result = new ArrayList<>(sorted.size());
        for (Field field : sorted) {
            FormField formField = field.getAnnotation(FormField.class);
            String idName = formField.id().isEmpty() ? field.getName() : formField.id();
            String label = formField.label().isEmpty() ? field.getName() : formField.label();
            result.add(new FieldMeta(
                    field,
                    idName,
                    label,
                    formField.readOnly(),
                    ReflectionUtils.fieldGetter(type, field.getName()),
                    ReflectionUtils.fieldSetter(type, field.getName())));
        }
        return Collections.unmodifiableList(result);
    }
}
