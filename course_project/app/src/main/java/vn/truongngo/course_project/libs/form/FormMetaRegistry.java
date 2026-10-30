package vn.truongngo.course_project.libs.form;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>Kho siêu dữ liệu form của các lớp model: mỗi lớp chỉ được quét bằng reflection một lần rồi dùng lại.</p>
 *
 * <p>Cache theo lớp ở phạm vi toàn tiến trình là đúng bản chất, vì danh sách field của form chỉ phụ thuộc vào chính lớp và
 * annotation của nó, không phụ thuộc layout hay màn hình nào dùng nó.</p>
 */
final class FormMetaRegistry {

    /** Siêu dữ liệu đã quét theo từng lớp model. An toàn đa luồng vì form có thể được dựng ở nhiều màn hình. */
    private static final Map<Class<?>, List<FormMeta.FieldMeta>> CACHE = new ConcurrentHashMap<>();

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private FormMetaRegistry() {
    }

    /**
     * Các field của form theo thứ tự <code>order</code>, quét ở lần đầu rồi cache. Quét lỗi thì không cache gì.
     *
     * @throws IllegalArgumentException nếu lớp không có field nào gắn <code>@FormField</code>
     */
    @NonNull
    static List<FormMeta.FieldMeta> of(@NonNull Class<?> modelClass) {
        return CACHE.computeIfAbsent(modelClass, FormMeta::scan);
    }
}
