package vn.truongngo.course_project.libs.util;

import android.content.Context;
import android.view.View;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class ViewBinder {

    /** Các field gắn {@link ViewId} của mỗi lớp container, quét một lần rồi dùng lại. */
    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    /** Id số theo tên id. Khóa chỉ là tên vì ứng dụng chỉ có một package tài nguyên. */
    private static final Map<String, Integer> ID_CACHE = new ConcurrentHashMap<>();

     /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private ViewBinder() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * <p>Tạo container bằng constructor không tham số rồi điền view từ <code>root</code> vào các field gắn
     * {@link ViewId}.</p>
     *
     * <p>Container cần có constructor không tham số và field không <code>final</code> (reflection ghi vào chúng). Bản
     * release bật R8 phải giữ các field và constructor đó, xem <code>app/src/main/keepRules/rules.keep</code>.</p>
     *
     * @throws IllegalArgumentException nếu container không có constructor không tham số, thiếu id, view không nằm trong
     *                                  layout, hoặc field không giữ được kiểu của view
     */
    @NonNull
    public static <V> V create(@NonNull Class<V> type, @NonNull View root) {
        Assert.notNull(type, "Container class must not be null");
        V container = ReflectionUtils.instantiateClass(type);
        Assert.notNull(container, "Container must have a no-arg constructor");
        bind(container, root);
        return container;
    }

    /** Dạng <code>Function&lt;View, V&gt;</code> để truyền thẳng vào adapter, ví dụ <code>ViewBinder.factory(BookRow.class)</code>. */
    @NonNull
    public static <V> Function<View, V> factory(@NonNull Class<V> type) {
        return root -> create(type, root);
    }

    /**
     * Điền view vào một container có sẵn.
     *
     * @throws IllegalArgumentException nếu thiếu id, view không nằm trong layout, hoặc field không giữ được kiểu của view;
     *                                  thông báo nêu rõ field vi phạm
     */
    public static void bind(@NonNull Object container, @NonNull View root) {
        Assert.notNull(container, "Container must not be null");
        Assert.notNull(root, "Root view must not be null");

        Field[] fields = FIELD_CACHE.computeIfAbsent(container.getClass(),
                type -> ReflectionUtils.findFields(type, f -> f.isAnnotationPresent(ViewId.class)));
        for (Field field : fields) {
            bindField(container, root, field);
        }
    }

    /**
     * Tìm view theo tên id (phần sau <code>@+id/</code>) trong <code>root</code>.
     *
     * @param where mô tả nơi gọi (ví dụ <code>"Book.title"</code>) để thông báo lỗi dễ tìm
     * @throws IllegalArgumentException nếu id không có trong tài nguyên, hoặc view không nằm trong layout này
     */
    @NonNull
    public static View find(@NonNull View root, @NonNull String idName, @NonNull String where) {
        View view = root.findViewById(resolveId(root.getContext(), idName, where));
        Assert.notNull(view, () -> where + ": id '" + idName + "' exists in the app but is not part of this layout");
        return view;
    }

    /** Gán view cùng tên id vào một field. Tên id lấy từ {@link ViewId#value()}, rỗng thì dùng tên field. */
    private static void bindField(Object container, View root, Field field) {
        String where = field.getDeclaringClass().getSimpleName() + "." + field.getName();
        String idName = Objects.requireNonNull(field.getAnnotation(ViewId.class)).value();
        if (idName.isEmpty()) {
            idName = field.getName();
        }
        final String name = idName;

        View view = find(root, name, where);
        Assert.isTrue(ClassUtils.isAssignable(field.getType(), view.getClass()),
                () -> where + ": a field of type " + field.getType().getSimpleName() + " cannot hold view '" + name + "' of type " + view.getClass().getSimpleName());

        ReflectionUtils.setField(field, container, view);
    }

    /**
     * Đổi tên id sang id số bằng <code>getIdentifier</code> (chậm hơn <code>R.id.x</code>, nên kết quả được cache).
     *
     * @throws IllegalArgumentException nếu không có id tên đó trong tài nguyên
     */
    private static int resolveId(Context context, String name, String where) {
        Integer cached = ID_CACHE.get(name);
        if (cached != null) {
            return cached;
        }
        int id = context.getResources().getIdentifier(name, "id", context.getPackageName());
        Assert.isTrue(id != 0, () -> where + ": no resource id '" + name + "' (android:id=\"@+id/" + name + "\")");
        ID_CACHE.put(name, id);
        return id;
    }
}
