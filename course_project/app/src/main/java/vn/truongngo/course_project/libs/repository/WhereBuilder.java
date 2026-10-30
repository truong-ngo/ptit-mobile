package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import vn.truongngo.course_project.libs.repository.specification.Criteria;

/**
 * <p>Biến danh sách {@link Criteria} thành mệnh đề <code>WHERE</code> và mảng tham số. Đây là nơi duy nhất ghép chuỗi SQL
 * cho truy vấn lọc: người dùng chỉ khai báo (field, toán tử, giá trị).</p>
 *
 * <p>Tên field Java được đổi sang tên cột, và giá trị luôn đi qua tham số <code>?</code> nên không có chuyện chèn SQL.
 * Không dùng lớp Android nào nên kiểm thử được trên JVM.</p>
 */
final class WhereBuilder {

    /**
     * Kết quả dựng. Cả hai thành phần đều null khi không có điều kiện nào.
     *
     * @param sql  mệnh đề <code>WHERE</code> không kèm từ khóa <code>WHERE</code>, tham số viết là <code>?</code>
     * @param args giá trị của các <code>?</code> theo thứ tự xuất hiện
     */
    record Where(@Nullable String sql, @Nullable String[] args) {
    }

    /** Kết quả dùng chung cho trường hợp không có điều kiện. */
    private static final Where NONE = new Where(null, null);

    /** Lớp tiện ích chỉ có hàm tĩnh, không tạo thể hiện. */
    private WhereBuilder() {
    }

    /**
     * Dựng mệnh đề <code>WHERE</code> nối các điều kiện bằng <code>AND</code>.
     *
     * @throws IllegalArgumentException nếu có field không thuộc thực thể, hoặc <code>BETWEEN</code> không đúng hai cận
     */
    @NonNull
    static Where build(@NonNull EntityMeta<?> meta, @Nullable List<Criteria> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return NONE;
        }
        List<String> predicates = new ArrayList<>(criteria.size());
        List<String> args = new ArrayList<>();

        for (Criteria c : criteria) {
            EntityMeta.ColumnMeta column = meta.byField.get(c.field());
            if (column == null) {
                throw new IllegalArgumentException(
                        "Unknown field '" + c.field() + "' on entity " + meta.type.getSimpleName());
            }
            String name = column.columnName();

            switch (c.operator()) {
                case IS_NULL, IS_NOT_NULL -> predicates.add(name + " " + c.operator().getToken());
                case IN -> {
                    Collection<?> values = (Collection<?>) c.value();
                    if (values == null || values.isEmpty()) {
                        // IN () không hợp lệ trong SQL, trả predicate false
                        predicates.add("1 = 0");
                    } else {
                        predicates.add(name + " IN (" + String.join(", ", Collections.nCopies(values.size(), "?")) + ")");
                        for (Object value : values) {
                            args.add(SqlTypes.toArg(value));
                        }
                    }
                }
                case BETWEEN -> {
                    List<?> range = (List<?>) c.value();
                    if (range == null || range.size() != 2) {
                        throw new IllegalArgumentException("BETWEEN criteria requires exactly 2 bounds for field " + c.field());
                    }
                    predicates.add(name + " BETWEEN ? AND ?");
                    args.add(SqlTypes.toArg(range.get(0)));
                    args.add(SqlTypes.toArg(range.get(1)));
                }
                default -> {
                    predicates.add(name + " " + c.operator().getToken() + " ?");
                    args.add(SqlTypes.toArg(c.value()));
                }
            }
        }
        return new Where(String.join(" AND ", predicates), args.toArray(new String[0]));
    }
}
