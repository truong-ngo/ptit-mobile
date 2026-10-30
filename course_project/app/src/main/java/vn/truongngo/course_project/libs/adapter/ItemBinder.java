package vn.truongngo.course_project.libs.adapter;

import org.jspecify.annotations.NonNull;

/**
 * <p>Đổ dữ liệu của một dòng vào view, có thêm <code>position</code>. Dùng khi lambda cần vị trí; không cần thì dùng dạng
 * hai tham số (<code>BiConsumer</code> của JDK), gọn hơn.</p>
 *
 * <p><code>position</code> chỉ đúng <b>tại lúc bind</b>. Chèn hoặc xóa một dòng ở phía trên thì <code>RecyclerView</code>
 * không bind lại các dòng bên dưới (chúng chỉ trượt vị trí), nên giá trị chúng nhận lần trước có thể đã cũ. Cần số thứ
 * tự luôn đúng thì tính từ dữ liệu, hoặc báo adapter bind lại cả đoạn bị lệch.</p>
 *
 * @param <V> container giữ các view con của một dòng
 * @param <T> kiểu dữ liệu của một dòng
 */
@FunctionalInterface
public interface ItemBinder<V, T> {

    /**
     * Đổ <code>item</code> vào các view của <code>views</code>. Phải gán mọi view, kể cả khi giá trị rỗng, vì ViewHolder
     * được tái sử dụng.
     */
    void bind(@NonNull V views, @NonNull T item, int position);
}
