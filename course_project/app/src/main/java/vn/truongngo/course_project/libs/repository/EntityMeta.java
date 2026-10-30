package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import vn.truongngo.course_project.libs.repository.annotation.Column;
import vn.truongngo.course_project.libs.repository.annotation.Id;
import vn.truongngo.course_project.libs.repository.annotation.OnDelete;
import vn.truongngo.course_project.libs.repository.annotation.References;
import vn.truongngo.course_project.libs.repository.annotation.Table;
import vn.truongngo.course_project.libs.util.Assert;
import vn.truongngo.course_project.libs.util.ClassUtils;
import vn.truongngo.course_project.libs.util.ReflectionUtils;

/**
 * <p>Siêu dữ liệu ánh xạ của một lớp thực thể, quét bằng reflection đúng một lần cho mỗi lớp.</p>
 *
 * <p>Quy ước giống JPA/Hibernate: mọi field không <code>static</code>, <code>final</code>, <code>transient</code> là
 * cột, đúng một field gắn <code>@Id</code>, tên bảng và cột mặc định đổi sang <code>snake_case</code>. Không dùng lớp
 * Android nào nên kiểm thử được trên JVM.</p>
 *
 * @param <E> lớp thực thể
 */
final class EntityMeta<E> {

    /**
     * Một cột của bảng cùng field Java tương ứng.
     *
     * @param fieldName        tên field Java
     * @param columnName       tên cột SQL
     * @param fieldType        kiểu Java của field
     * @param sqlType          kiểu lưu trữ SQLite
     * @param isId             có phải khóa chính không
     * @param autoIncrement    khóa chính tự tăng (chỉ có nghĩa khi isId)
     * @param nullable         cột có cho phép NULL không
     * @param unique           giá trị trong cột có phải duy nhất không
     * @param referencedEntity thực thể được tham chiếu nếu là khóa ngoại, không thì null
     * @param onDelete         hành vi khi bản ghi cha bị xóa (chỉ có nghĩa khi là khóa ngoại)
     * @param getter           đọc giá trị field từ một thực thể
     * @param setter           ghi giá trị vào field của một thực thể
     */
    record ColumnMeta(
            String fieldName,
            String columnName,
            Class<?> fieldType,
            SqlType sqlType,
            boolean isId,
            boolean autoIncrement,
            boolean nullable,
            boolean unique,
            @Nullable Class<?> referencedEntity,
            OnDelete onDelete,
            Function<Object, Object> getter,
            BiConsumer<Object, Object> setter
    ) {
    }

    /** Lớp thực thể. */
    final Class<E> type;

    /** Tên bảng SQL. */
    final String tableName;

    /** Cột khóa chính. */
    final ColumnMeta idColumn;

    /** Mọi cột theo thứ tự khai báo field, gồm cả khóa chính. */
    final List<ColumnMeta> columns;

    /** Các cột còn lại khi bỏ khóa chính. */
    final List<ColumnMeta> nonIdColumns;

    /** Tra cột theo tên field Java (không phải tên cột SQL). */
    final Map<String, ColumnMeta> byField;

    /**
     * Quét <code>type</code> bằng reflection thành siêu dữ liệu mới, <b>không cache</b>. Gọi {@link EntityMetaRegistry#of}
     * để được kết quả đã cache.
     *
     * @throws IllegalStateException nếu lớp không hợp lệ làm thực thể (thiếu hoặc thừa <code>@Id</code>, kiểu field chưa hỗ trợ)
     */
    @NonNull
    static <E> EntityMeta<E> scan(@NonNull Class<E> type) {
        Assert.notNull(type, "entityClass cannot be null");
        return new EntityMeta<>(type);
    }

    /** Quét field của <code>type</code> thành các cột; xem {@link #scan} về các lỗi có thể ném. */
    private EntityMeta(Class<E> type) {
        this.type = type;
        this.tableName = resolveTableName(type);

        Field[] persistent = ReflectionUtils.findFields(type,
                f -> !Modifier.isStatic(f.getModifiers())
                        && !Modifier.isFinal(f.getModifiers())
                        && !Modifier.isTransient(f.getModifiers()));

        List<ColumnMeta> all = new ArrayList<>();
        List<ColumnMeta> regular = new ArrayList<>();
        Map<String, ColumnMeta> fieldMap = new LinkedHashMap<>();
        ColumnMeta foundId = null;

        for (Field field : persistent) {
            ColumnMeta column = buildColumn(type, field);
            all.add(column);
            fieldMap.put(field.getName(), column);
            if (column.isId()) {
                if (foundId != null) {
                    throw new IllegalStateException("Entity " + type.getName() + " has more than one field annotated with @Id");
                }
                foundId = column;
            } else {
                regular.add(column);
            }
        }
        if (foundId == null) {
            throw new IllegalStateException("Entity " + type.getName() + " must have a field annotated with @Id");
        }

        this.idColumn = foundId;
        this.columns = Collections.unmodifiableList(all);
        this.nonIdColumns = Collections.unmodifiableList(regular);
        this.byField = Collections.unmodifiableMap(fieldMap);
    }

    /** Tạo đối tượng thực thể rỗng bằng constructor không tham số. */
    @Nullable
    E newInstance() {
        return ReflectionUtils.instantiateClass(type);
    }

    /**
     * Dựng siêu dữ liệu của một cột từ field và các annotation của nó.
     *
     * @throws IllegalStateException nếu kiểu field chưa hỗ trợ, <code>@Id</code> tự tăng không phải <code>Long</code>/<code>Integer</code>,
     *                               hoặc <code>onDelete = SET_NULL</code> đặt trên cột không cho phép null
     */
    private static ColumnMeta buildColumn(Class<?> entity, Field field) {
        String where = entity.getSimpleName() + "." + field.getName();

        SqlType sqlType = SqlTypes.typeOf(field.getType());
        if (sqlType == null) {
            throw new IllegalStateException(where + ": type " + field.getType().getName()
                    + " is not supported. Use boolean/number/String/char/BigDecimal/enum/Date/byte[], "
                    + "or declare the field transient to skip it.");
        }

        Id id = field.getAnnotation(Id.class);
        Column column = field.getAnnotation(Column.class);
        References ref = field.getAnnotation(References.class);

        boolean isId = id != null;
        boolean autoIncrement = isId && id.autoIncrement();
        if (autoIncrement) {
            Class<?> wrapper = ClassUtils.resolveWrapperIfNecessary(field.getType());
            if (wrapper != Long.class && wrapper != Integer.class) {
                throw new IllegalStateException(where + ": an auto-increment @Id only supports Long or Integer, "
                        + "use @Id(autoIncrement = false) if the key is assigned by the application");
            }
        }

        boolean nullable = !isId && (column == null || column.nullable());
        boolean unique = !isId && column != null && column.unique();
        OnDelete onDelete = ref != null ? ref.onDelete() : OnDelete.RESTRICT;
        if (ref != null && onDelete == OnDelete.SET_NULL && !nullable) {
            throw new IllegalStateException(where + ": onDelete = SET_NULL requires a nullable column (@Column(nullable = true))");
        }

        return new ColumnMeta(
                field.getName(),
                resolveColumnName(field, column),
                field.getType(),
                sqlType,
                isId,
                autoIncrement,
                nullable,
                unique,
                ref != null ? ref.entity() : null,
                onDelete,
                ReflectionUtils.fieldGetter(entity, field.getName()),
                ReflectionUtils.fieldSetter(entity, field.getName()));
    }

    /** Tên bảng từ {@link Table#name()}, rỗng thì đổi tên lớp sang <code>snake_case</code>. */
    private static String resolveTableName(Class<?> entity) {
        Table table = entity.getAnnotation(Table.class);
        if (table != null && !table.name().trim().isEmpty()) {
            return table.name().trim();
        }
        return toSnakeCase(entity.getSimpleName());
    }

    /** Tên cột từ {@link Column#name()}, rỗng hoặc không có annotation thì đổi tên field sang <code>snake_case</code>. */
    private static String resolveColumnName(Field field, @Nullable Column column) {
        if (column != null && !column.name().trim().isEmpty()) {
            return column.name().trim();
        }
        return toSnakeCase(field.getName());
    }

    /** Chuyển <code>camelCase</code> hoặc <code>PascalCase</code> sang <code>snake_case</code>. */
    @NonNull
    static String toSnakeCase(String text) {
        StringBuilder sb = new StringBuilder(text.length() + 4);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
