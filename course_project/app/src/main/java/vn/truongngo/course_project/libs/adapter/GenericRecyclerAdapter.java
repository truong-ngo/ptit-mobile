package vn.truongngo.course_project.libs.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import vn.truongngo.course_project.libs.util.ViewBinder;
import vn.truongngo.course_project.libs.util.ViewId;

/**
 * <p>Khái niệm liên quan:</p>
 * <ul>
 *   <li><code>RecyclerView</code> chỉ dựng đủ số View để lấp đầy màn hình rồi tái sử dụng: dòng trượt ra khỏi màn hình
 *       thì View của nó được dùng cho dòng mới, nên 10.000 dòng dữ liệu vẫn chỉ cần vài View. Nó quyết định khi nào cần
 *       View, giữ kho View đã dùng, và lo việc đặt vị trí, cuộn, animation.</li>
 *   <li>Adapter không quản lý vòng đời của View mà trả lời khi <code>RecyclerView</code> hỏi: có bao nhiêu dòng
 *       ({@link #getItemCount()}), dựng một ViewHolder mới ({@link #onCreateViewHolder}), đổ dữ liệu của dòng này vào
 *       ViewHolder này ({@link #onBindViewHolder}). <code>RecyclerView</code> giống object pool, adapter giống factory
 *       kèm nguồn dữ liệu của pool đó.</li>
 *   <li>ViewHolder ({@link BaseViewHolder}) giữ View gốc của một dòng và container các view con của nó, để đổ dữ liệu
 *       không phải tìm view lại.</li>
 * </ul>
 *
 * <p>Vấn đề lớp này giải quyết: dùng <code>RecyclerView.Adapter</code> trần thì mỗi danh sách phải viết một adapter và
 * một ViewHolder, phần lớn là mã lặp (giữ danh sách, inflate layout, <code>findViewById</code> từng view con, gắn click,
 * tính phần thay đổi giữa hai danh sách). Lớp này gom phần lặp đó, mỗi màn hình chỉ khai báo ba thứ khác nhau: layout
 * của một dòng, container giữ các view con, và cách đổ một item vào container.</p>
 *
 * <p>Cách dùng, từ gọn đến đầy đủ:</p>
 * <ul>
 *   <li><code>of(layout, Row.class, (row, item) -&gt; ...)</code> &mdash; container có <code>@ViewId</code>, {@link ViewBinder}
 *       tự điền view; thêm tham số <code>position</code> nếu cần (xem {@link ItemBinder})</li>
 *   <li>constructor với <code>viewsFactory</code> &mdash; tự chỉ ra cách tạo container, ví dụ container viết tay</li>
 *   <li>lớp con override {@link #bind} &mdash; khi muốn tự viết bind, hoặc dùng lại một dòng ở nhiều màn hình</li>
 * </ul>
 * <p>Danh sách được cập nhật bằng {@link #submit} (thay cả danh sách) hoặc {@link #append} (nối thêm, cho load more).</p>
 *
 * @param <T> kiểu dữ liệu của một dòng
 * @param <V> container giữ các view của một dòng, do người dùng viết
 */
public class GenericRecyclerAdapter<T, V> extends RecyclerView.Adapter<BaseViewHolder<V>> {

    /** Layout của một dòng. */
    private final int layoutResId;

    /** Tạo container giữ view từ root của dòng vừa inflate. */
    private final Function<View, V> viewsFactory;

    /** Null khi dùng lớp con override {@link #bind}. */
    private final ItemBinder<V, T> binder;

    /** Danh sách đang hiện; luôn là bản sao riêng của adapter, không dùng chung với người gọi. */
    private List<T> items = new ArrayList<>();

    /** Null thì {@link #submit} vẽ lại cả danh sách thay vì chỉ cập nhật phần thay đổi. */
    private Function<T, ?> idProvider;

    /** Nhận (item, position) khi bấm một dòng. Null khi chưa có ai nghe. */
    private BiConsumer<T, Integer> onItemClick;

    /** Nhận (item, position) khi nhấn giữ một dòng. Null khi chưa có ai nghe. */
    private BiConsumer<T, Integer> onItemLongClick;

    /**
     * Dạng gọn nhất: container là lớp có {@link ViewId} nên {@link ViewBinder} tự điền view. Lambda không có
     * <code>position</code>; cần thì dùng bản nhận {@link ItemBinder}.
     * Kiểu <code>T</code> được suy ra từ kiểu của biến nhận kết quả, nên cần gán vào một biến có kiểu rõ.
     *
     * @param viewsType lớp container của dòng: constructor không tham số, field gắn <code>@ViewId</code>
     */
    @NonNull
    public static <T, V> GenericRecyclerAdapter<T, V> of(@LayoutRes int layoutResId,
                                                         @NonNull Class<V> viewsType,
                                                         @NonNull BiConsumer<V, T> binder) {
        return new GenericRecyclerAdapter<>(layoutResId, ViewBinder.factory(viewsType), binder);
    }

    /** Như bản trên, nhưng lambda nhận thêm <code>position</code>. */
    @NonNull
    public static <T, V> GenericRecyclerAdapter<T, V> of(@LayoutRes int layoutResId,
                                                         @NonNull Class<V> viewsType,
                                                         @NonNull ItemBinder<V, T> binder) {
        return new GenericRecyclerAdapter<>(layoutResId, ViewBinder.factory(viewsType), binder);
    }

    /**
     * @param viewsFactory nhận root của dòng vừa dựng, trả về container giữ các view, ví dụ <code>BookRow::new</code>
     * @param binder       đổ dữ liệu vào view, không có <code>position</code>
     */
    public GenericRecyclerAdapter(@LayoutRes int layoutResId,
                                  @NonNull Function<View, V> viewsFactory,
                                  @NonNull BiConsumer<V, T> binder) {
        this(layoutResId, viewsFactory, (views, item, position) -> binder.accept(views, item));
    }

    /**
     * Như constructor trên, nhưng binder nhận thêm <code>position</code>. Java chọn bản nào theo số tham số của lambda.
     */
    public GenericRecyclerAdapter(@LayoutRes int layoutResId,
                                  @NonNull Function<View, V> viewsFactory,
                                  @NonNull ItemBinder<V, T> binder) {
        this.layoutResId = layoutResId;
        this.viewsFactory = viewsFactory;
        this.binder = binder;
    }

    /**
     * Dành cho lớp con tự override {@link #bind}: không có binder, nên không override thì {@link #bind} ném lỗi.
     *
     * @param viewsFactory nhận root của dòng vừa dựng, trả về container giữ các view
     */
    protected GenericRecyclerAdapter(@LayoutRes int layoutResId, @NonNull Function<View, V> viewsFactory) {
        this.layoutResId = layoutResId;
        this.viewsFactory = viewsFactory;
        this.binder = null;
    }

    /**
     * Đổ dữ liệu của một dòng vào view. Mặc định gọi binder truyền vào constructor; lớp con override khi muốn tự viết.
     * <code>RecyclerView</code> tái sử dụng ViewHolder, nên phải gán mọi view, kể cả khi giá trị rỗng (có nhánh hiện
     * thì phải có nhánh ẩn), nếu không dòng mới còn dính dữ liệu của dòng cũ. <code>position</code> chỉ đúng tại lúc
     * bind, xem {@link ItemBinder}.
     *
     * @throws IllegalStateException nếu không có binder và lớp con không override
     */
    protected void bind(@NonNull V views, @NonNull T item, int position) {
        if (binder == null) {
            throw new IllegalStateException(getClass().getName()
                    + " has no binder: override bind(...) or pass a binder to the constructor");
        }
        binder.bind(views, item, position);
    }

    /**
     * Cách lấy định danh của một item để {@link #submit} so sánh danh sách cũ và mới.
     */
    public void setIdProvider(@Nullable Function<T, ?> idProvider) {
        this.idProvider = idProvider;
    }

    /**
     * Đặt nơi nhận khi bấm một dòng, với (item, position). Dùng <code>BiConsumer</code> của JDK nên item đứng trước,
     * position đứng sau. <code>null</code> để bỏ.
     */
    public void setOnItemClick(@Nullable BiConsumer<T, Integer> onItemClick) {
        this.onItemClick = onItemClick;
    }

    /** Đặt listener này thì nhấn giữ luôn được coi là đã xử lý: click thường không chạy tiếp. */
    public void setOnItemLongClick(@Nullable BiConsumer<T, Integer> onItemLongClick) {
        this.onItemLongClick = onItemLongClick;
    }

    /**
     * <p>Thay toàn bộ danh sách bằng bản sao của <code>newItems</code> (sao chép để người gọi sửa danh sách gốc sau đó
     * không làm đổi adapter), rồi báo <code>RecyclerView</code> biết dòng nào thay đổi. Có hai nhánh:</p>
     * <ul>
     *   <li><b>Chưa có {@link #setIdProvider}:</b> adapter không biết dòng cũ và dòng mới tương ứng nhau thế nào, nên
     *       báo "mọi thứ đã đổi" (<code>notifyDataSetChanged</code>). Mọi dòng đang hiện bị bind lại, không có animation
     *       và vị trí cuộn dễ bị giật.</li>
     *   <li><b>Có {@link #setIdProvider}:</b> <code>DiffUtil</code> so danh sách cũ với mới để tìm dòng được thêm, xóa,
     *       di chuyển hoặc đổi nội dung, rồi gửi đúng các thông báo tương ứng (kèm animation), các dòng còn lại giữ
     *       nguyên. Nó dựa vào hai câu hỏi: hai dòng có phải <b>cùng một đối tượng</b> không (so id từ
     *       <code>idProvider</code>), và nếu cùng thì <b>nội dung</b> có giống nhau không (so bằng <code>equals</code> của
     *       <code>T</code>).</li>
     * </ul>
     *
     * <p>Ví dụ: sửa tên cuốn sách id 5 rồi nạp lại danh sách thì chỉ dòng id 5 được bind lại (cùng id, khác nội dung),
     * cuốn bị xóa biến mất kèm animation, các cuốn còn lại không đụng tới. Vì vậy <code>T</code> nên override
     * <code>equals</code>; nếu không, mọi dòng đều bị coi là đã đổi và bind lại như nhánh đầu.</p>
     *
     * <p>Chạy trên main thread; danh sách rất lớn thì tính diff ở nền sẽ hợp hơn.</p>
     */
    @SuppressLint("NotifyDataSetChanged")
    public void submit(@NonNull List<T> newItems) {
        List<T> next = new ArrayList<>(newItems);
        if (idProvider == null) {
            items = next;
            notifyDataSetChanged();
            return;
        }

        List<T> old = items;
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return old.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(idProvider.apply(old.get(oldPosition)), idProvider.apply(next.get(newPosition)));
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(old.get(oldPosition), next.get(newPosition));
            }
        });
        items = next;
        diff.dispatchUpdatesTo(this);
    }

    /** Nối thêm vào cuối (load more). Chỉ báo phần được thêm, các dòng cũ giữ nguyên. */
    public void append(@NonNull List<T> more) {
        if (more.isEmpty()) {
            return;
        }
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    /**
     * Item đang ở vị trí này.
     *
     * @throws IndexOutOfBoundsException nếu vị trí ngoài danh sách
     */
    @NonNull
    public T getItem(int position) {
        return items.get(position);
    }

    /**
     * Gắn click và long click một lần lúc tạo ViewHolder, nhưng đọc vị trí lúc bấm: ViewHolder được tái sử dụng cho dòng
     * khác nên vị trí lúc tạo sẽ sai. Vị trí là <code>NO_POSITION</code> khi dòng đang bị xóa dở, lúc đó bỏ qua.
     */
    @NonNull
    @Override
    public BaseViewHolder<V> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        BaseViewHolder<V> holder = new BaseViewHolder<>(root, viewsFactory.apply(root));

        holder.itemView.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && onItemClick != null) {
                onItemClick.accept(items.get(position), position);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION || onItemLongClick == null) {
                return false;
            }
            onItemLongClick.accept(items.get(position), position);
            return true;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<V> holder, int position) {
        bind(holder.views, items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
