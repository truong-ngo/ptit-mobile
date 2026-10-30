package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.truongngo.course_project.libs.util.Assert;

/**
 * <p>Sinh câu lệnh tạo và xóa bảng từ annotation của các thực thể (giống <code>hbm2ddl</code> của Hibernate), và tính số
 * version của schema từ chính các câu lệnh đó.</p>
 *
 * <p>Version là mã băm của toàn bộ câu <code>CREATE TABLE</code>: schema đổi thì version đổi, nên
 * <code>SQLiteOpenHelper</code> tự gọi <code>onUpgrade</code>, không cần nhớ tăng số bằng tay. Không dùng lớp Android nào
 * nên kiểm thử được trên JVM.</p>
 */
final class SqlBuilder {

    private SqlBuilder() {
    }

    /** Câu <code>CREATE TABLE</code> cho một thực thể, kèm khóa ngoại nếu có <code>@References</code>. */
    @NonNull
    static String createTable(@NonNull EntityMeta<?> meta) {
        List<String> parts = new ArrayList<>();
        for (EntityMeta.ColumnMeta c : meta.columns) {
            StringBuilder column = new StringBuilder(c.columnName()).append(' ').append(c.sqlType());
            if (c.isId()) {
                column.append(" PRIMARY KEY");
                if (c.autoIncrement()) {
                    column.append(" AUTOINCREMENT");
                }
            } else {
                if (!c.nullable()) {
                    column.append(" NOT NULL");
                }
                if (c.unique()) {
                    column.append(" UNIQUE");
                }
            }
            parts.add(column.toString());
        }
        for (EntityMeta.ColumnMeta c : meta.columns) {
            if (c.referencedEntity() != null) {
                EntityMeta<?> parent = EntityMetaRegistry.of(c.referencedEntity());
                parts.add("FOREIGN KEY (" + c.columnName() + ") REFERENCES " + parent.tableName
                        + "(" + parent.idColumn.columnName() + ") ON DELETE " + c.onDelete().sql());
            }
        }
        return "CREATE TABLE " + meta.tableName + " (" + String.join(", ", parts) + ")";
    }

    /**
     * Các câu tạo bảng theo thứ tự đúng: bảng được tham chiếu (cha) trước bảng tham chiếu (con), bất kể thứ tự đăng ký.
     *
     * @throws IllegalStateException nếu khóa ngoại tham chiếu thực thể chưa đăng ký, hoặc tạo vòng tròn
     */
    @NonNull
    static List<String> createStatements(@NonNull Class<?>... entities) {
        List<String> statements = new ArrayList<>();
        for (Class<?> entity : sortParentsFirst(entities)) {
            statements.add(createTable(EntityMetaRegistry.of(entity)));
        }
        return statements;
    }

    /** Các câu xóa bảng theo thứ tự ngược với tạo: con trước cha, để khóa ngoại không chặn việc xóa. */
    @NonNull
    static List<String> dropStatements(@NonNull Class<?>... entities) {
        List<Class<?>> ordered = new ArrayList<>(sortParentsFirst(entities));
        Collections.reverse(ordered);
        List<String> statements = new ArrayList<>();
        for (Class<?> entity : ordered) {
            statements.add("DROP TABLE IF EXISTS " + EntityMetaRegistry.of(entity).tableName);
        }
        return statements;
    }

    /**
     * Số version của schema, luôn từ 1 trở lên (yêu cầu của <code>SQLiteOpenHelper</code>). <code>String.hashCode</code>
     * được quy định chính xác nên kết quả ổn định giữa các lần chạy.
     */
    static int versionOf(@NonNull Class<?>... entities) {
        int hash = String.join(";", createStatements(entities)).hashCode();
        return (hash & 0x7FFFFFFF) % 2147483646 + 1;
    }

    /**
     * Sắp các thực thể sao cho cha đứng trước con (duyệt sâu theo khóa ngoại).
     *
     * @throws IllegalArgumentException nếu không đăng ký thực thể nào
     */
    private static List<Class<?>> sortParentsFirst(Class<?>[] entities) {
        Assert.isTrue(entities != null && entities.length > 0, "At least one entity class must be registered");
        List<Class<?>> registered = Arrays.asList(entities);
        List<Class<?>> sorted = new ArrayList<>();
        Set<Class<?>> visiting = new HashSet<>();
        for (Class<?> entity : registered) {
            visit(entity, registered, visiting, sorted);
        }
        return sorted;
    }

    /**
     * Thêm <code>entity</code> vào <code>sorted</code> sau khi đã thêm các thực thể mà nó tham chiếu. Khóa ngoại trỏ về
     * chính nó được bỏ qua. <code>visiting</code> là đường đi hiện tại, dùng để phát hiện vòng tròn.
     */
    private static void visit(Class<?> entity, List<Class<?>> registered, Set<Class<?>> visiting, List<Class<?>> sorted) {
        if (sorted.contains(entity)) {
            return;
        }
        if (!visiting.add(entity)) {
            throw new IllegalStateException("Foreign keys form a cycle around entity " + entity.getName());
        }
        for (EntityMeta.ColumnMeta c : EntityMetaRegistry.of(entity).columns) {
            Class<?> parent = c.referencedEntity();
            if (parent == null || parent == entity) {
                continue;
            }
            if (!registered.contains(parent)) {
                throw new IllegalStateException(entity.getSimpleName() + "." + c.fieldName()
                        + " references " + parent.getName() + " but that entity is not registered");
            }
            visit(parent, registered, visiting, sorted);
        }
        visiting.remove(entity);
        sorted.add(entity);
    }
}
