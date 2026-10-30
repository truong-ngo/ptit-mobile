package vn.truongngo.course_project.libs.repository.specification;

/**
 * Bản ghi bất biến biểu diễn một điều kiện lọc đơn lẻ trên một trường của thực thể.
 *
 * @param field    tên trường Java trên lớp Entity (sẽ được {@link vn.truongngo.course_project.libs.repository.BaseSqliteRepository}
 *                 ánh xạ sang tên cột SQL tương ứng)
 * @param operator toán tử so sánh ({@link Operator})
 * @param value    giá trị đối chiếu (có thể là giá trị đơn, {@link java.util.Collection} cho {@link Operator#IN},
 *                 {@link java.util.List} 2 phần tử cho {@link Operator#BETWEEN}, hoặc {@code null} cho {@link Operator#IS_NULL})
 */
public record Criteria(String field, Operator operator, Object value) {
}
