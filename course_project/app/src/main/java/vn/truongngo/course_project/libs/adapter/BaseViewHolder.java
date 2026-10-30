package vn.truongngo.course_project.libs.adapter;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;

/**
 * <p>ViewHolder giữ hai thứ của một dòng, cùng sinh ra từ một <code>root</code>: <code>itemView</code> là View gốc của
 * cả dòng, còn {@link #views} là container giữ các view <b>con</b> bên trong nó. Nhờ vậy <code>bind</code> không phải
 * tìm view lại mỗi lần cuộn.</p>
 *
 * <p><code>RecyclerView</code> tái sử dụng ViewHolder cho nhiều dòng dữ liệu, nên ViewHolder chỉ giữ view, không giữ dữ
 * liệu của dòng.</p>
 *
 * @param <V> container giữ các view con của một dòng, do người dùng viết
 */
public class BaseViewHolder<V> extends RecyclerView.ViewHolder {

    /**
     * Container giữ các view <b>con</b> của dòng (tên sách, giá...), tìm một lần lúc dựng ViewHolder. Khác
     * <code>itemView</code> có sẵn của <code>RecyclerView</code>: đó là View gốc của cả dòng, dùng để đặt vị trí và tái
     * sử dụng, còn đây là phần của ta để đổ dữ liệu vào view con. Cả hai cùng nằm trong một cây View.
     */
    public final V views;

    /**
     * @param root  View gốc của dòng, thường là kết quả inflate một file XML (nhưng có thể dựng bằng code); trở thành
     *              <code>itemView</code> của ViewHolder
     * @param views container giữ các view con của <code>root</code>, tạo từ chính <code>root</code> này
     */
    public BaseViewHolder(@NonNull View root, @NonNull V views) {
        super(root);
        this.views = views;
    }
}
