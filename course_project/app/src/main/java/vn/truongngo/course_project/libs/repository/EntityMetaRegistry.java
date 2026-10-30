package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import vn.truongngo.course_project.libs.util.Assert;

/**
 * <p>Kho siêu dữ liệu của các lớp thực thể: mỗi lớp chỉ được quét bằng reflection một lần rồi dùng lại.</p>
 *
 * <p>Cache theo lớp ở phạm vi toàn tiến trình là đúng bản chất, vì siêu dữ liệu chỉ phụ thuộc vào chính lớp và annotation
 * của nó, không phụ thuộc <code>BaseDbHelper</code> nào dùng nó. Khóa giữ tham chiếu mạnh tới <code>Class</code>, chấp nhận
 * được vì lớp của ứng dụng Android không bị gỡ khỏi bộ nhớ khi đang chạy.</p>
 */
final class EntityMetaRegistry {

    /** Giá trị luôn là <code>EntityMeta</code> của đúng lớp làm khóa, nên phép ép kiểu trong {@link #of} an toàn. */
    private static final Map<Class<?>, EntityMeta<?>> CACHE = new ConcurrentHashMap<>();

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private EntityMetaRegistry() {
    }

    /**
     * Siêu dữ liệu của <code>type</code>, quét ở lần đầu rồi cache. Quét lỗi thì không cache gì, nên lần gọi sau cũng lỗi.
     *
     * @throws IllegalStateException nếu lớp không hợp lệ làm thực thể (thiếu hoặc thừa <code>@Id</code>, kiểu field chưa hỗ trợ)
     */
    @SuppressWarnings("unchecked")
    @NonNull
    static <E> EntityMeta<E> of(@NonNull Class<E> type) {
        Assert.notNull(type, "entityClass cannot be null");
        return (EntityMeta<E>) CACHE.computeIfAbsent(type, k -> EntityMeta.scan(type));
    }
}
