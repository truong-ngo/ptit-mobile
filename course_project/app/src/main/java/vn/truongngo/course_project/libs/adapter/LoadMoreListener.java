package vn.truongngo.course_project.libs.adapter;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;

import vn.truongngo.course_project.libs.base.Lifetime;

/**
 * <p>Tải trang kế tiếp khi người dùng cuộn gần tới cuối danh sách (load more). Lớp này chỉ lo <b>khi nào</b> tải; việc
 * lấy trang (repository, <code>Async</code>) và đưa dữ liệu vào danh sách (<code>adapter.append</code> hoặc
 * <code>submit</code>) do nơi gọi làm. Tách như vậy để cùng một listener dùng được với mọi nguồn dữ liệu (SQLite, mạng)
 * và mọi cách giữ state (Fragment, ViewModel).</p>
 *
 * <p><b>Vòng đời của một lần tải.</b> Listener có hai trạng thái, <i>rảnh</i> và <i>đang tải</i>:</p>
 * <ul>
 *   <li>Cuộn xuống gần cuối khi đang rảnh và còn trang sau: listener chuyển sang <i>đang tải</i> rồi gọi
 *       <code>onLoadMore</code>.</li>
 *   <li>Trong lúc <i>đang tải</i>, cuộn tiếp không gây tải trùng.</li>
 *   <li>Nơi gọi <b>phải</b> kết thúc mỗi lần tải bằng {@link #loaded} (thành công) hoặc {@link #failed} (lỗi) để listener
 *       quay về <i>rảnh</i>. Quên báo thì listener kẹt ở <i>đang tải</i> và không bao giờ tải nữa.</li>
 * </ul>
 *
 * <p><b>Trang đầu tiên</b> do nơi gọi tự tải rồi gọi <code>loaded(hasNext)</code>, để listener biết còn trang sau không;
 * listener không tự tải trang đầu.</p>
 *
 * <p><b>Thành công và lỗi khác nhau ở một điểm quan trọng.</b> {@link #loaded} tự kiểm tra lại ngay (vì trang vừa nhận có
 * thể chưa lấp đầy màn hình, khi đó người dùng không có gì để cuộn nên sẽ không có sự kiện cuộn nào kích hoạt lần tải
 * kế), còn {@link #failed} thì không. Nếu báo lỗi bằng <code>loaded</code>, listener sẽ tải lại ngay, lỗi lại, và lặp vô
 * hạn khi người dùng đang ở cuối danh sách. Với <code>failed</code>, lần thử lại chờ tới khi người dùng cuộn xuống thêm.</p>
 *
 * <p>Chỉ hỗ trợ <code>LinearLayoutManager</code> và <code>GridLayoutManager</code>, và chỉ gọi từ main thread. Listener tự
 * gỡ khỏi <code>RecyclerView</code> khi {@link Lifetime} kết thúc, nên truyền <code>Lifetime</code> của View (không phải
 * của ViewModel). Giữ tham chiếu listener trong ViewModel sẽ giữ cả <code>RecyclerView</code> sau khi View đã hủy.</p>
 *
 * <pre>{@code
 * loadMore = LoadMoreListener.attach(list, lifetime(), viewModel::loadNextPage);
 *
 * // khi một lần tải kết thúc (kể cả trang đầu):
 * if (failed) loadMore.failed();
 * else loadMore.loaded(page.hasNext());
 * }</pre>
 */
public final class LoadMoreListener extends RecyclerView.OnScrollListener {

    /** Số dòng còn lại phía dưới mà khi cuộn tới thì bắt đầu tải trang kế, để người dùng khỏi phải chờ. */
    public static final int DEFAULT_THRESHOLD = 3;

    /** Danh sách đang theo dõi; đọc adapter và LayoutManager tại thời điểm kiểm tra, không giữ bản sao. */
    private final RecyclerView list;

    /** Việc cần làm khi tới lúc tải trang kế; chạy ở main thread. */
    private final Runnable onLoadMore;

    /** Không âm. Xem {@link #DEFAULT_THRESHOLD}. */
    private final int threshold;

    /** Cờ chặn tải trùng: bật ngay trước khi gọi <code>onLoadMore</code>, tắt ở {@link #loaded}, {@link #failed}, {@link #reset}. */
    private boolean loading;

    /** Còn trang sau không, theo lần {@link #loaded} hoặc {@link #reset} gần nhất; ban đầu coi là còn. */
    private boolean hasMore = true;

    /** Chỉ tạo qua {@link #attach}, vì listener vô dụng nếu chưa được gắn vào <code>RecyclerView</code>. */
    private LoadMoreListener(RecyclerView list, int threshold, Runnable onLoadMore) {
        this.list = list;
        this.threshold = threshold;
        this.onLoadMore = onLoadMore;
    }

    /**
     * Gắn listener vào <code>list</code> với {@link #DEFAULT_THRESHOLD}. Xem {@link #attach(RecyclerView, Lifetime, int,
     * Runnable)}.
     *
     * @param onLoadMore chạy ở main thread khi cần tải trang kế; phải kết thúc bằng {@link #loaded} hoặc {@link #failed}
     */
    @NonNull
    public static LoadMoreListener attach(@NonNull RecyclerView list, @NonNull Lifetime lifetime, @NonNull Runnable onLoadMore) {
        return attach(list, lifetime, DEFAULT_THRESHOLD, onLoadMore);
    }

    /**
     * <p>Tạo listener, gắn vào <code>list</code> và đăng ký tự gỡ khi <code>lifetime</code> kết thúc. Giá trị trả về cần
     * giữ lại để gọi {@link #loaded} và {@link #failed}.</p>
     *
     * <p><code>list</code> chưa cần có adapter hay LayoutManager lúc gắn: chúng chỉ được đọc khi cuộn.</p>
     *
     * @param lifetime   khoảng đời của View chứa <code>list</code>; kết thúc thì listener bị gỡ
     * @param threshold  số dòng còn lại phía dưới để bắt đầu tải; 0 nghĩa là chỉ tải khi dòng cuối đã hiện, số âm được coi
     *                   là 0
     * @param onLoadMore chạy ở main thread khi cần tải trang kế; phải kết thúc bằng {@link #loaded} hoặc {@link #failed}
     */
    @NonNull
    public static LoadMoreListener attach(@NonNull RecyclerView list, @NonNull Lifetime lifetime,
                                          int threshold, @NonNull Runnable onLoadMore) {
        LoadMoreListener listener = new LoadMoreListener(list, Math.max(threshold, 0), onLoadMore);
        list.addOnScrollListener(listener);
        lifetime.onEnd(() -> list.removeOnScrollListener(listener));
        return listener;
    }

    /**
     * <p>Báo một lần tải đã xong thành công, kể cả trang đầu tiên: về trạng thái rảnh, lưu việc còn trang sau không rồi
     * kiểm tra lại ngay (đẩy vào hàng đợi của <code>list</code> để chạy sau khi adapter nhận dữ liệu mới và layout xong).</p>
     *
     * <p>Kiểm tra lại là để xử lý trang chưa lấp đầy màn hình: khi đó không còn gì để cuộn nên không có sự kiện cuộn nào
     * kích hoạt lần tải kế, listener phải tự tải tiếp. Vì vậy <b>không</b> gọi hàm này để báo lỗi, dùng {@link #failed}.</p>
     *
     * @param hasMore còn trang sau không, thường là <code>page.hasNext()</code>; <code>false</code> thì listener ngừng gọi
     *                <code>onLoadMore</code> cho tới khi có <code>loaded(true)</code> hoặc {@link #reset}
     */
    public void loaded(boolean hasMore) {
        this.loading = false;
        this.hasMore = hasMore;
        list.post(this::check);
    }

    /**
     * Báo lần tải thất bại: về trạng thái rảnh nhưng <b>không</b> tự thử lại (tránh vòng lặp lỗi khi mất kết nối). Lần thử
     * kế tiếp chờ người dùng cuộn xuống thêm. Giữ nguyên {@link #hasMore()} vì lỗi không cho biết gì về trang sau.
     */
    public void failed() {
        this.loading = false;
    }

    /**
     * Về trạng thái ban đầu (còn trang sau, không đang tải), dùng khi làm mới danh sách hoặc đổi bộ lọc mà không có lần
     * tải nào sẽ tự báo {@link #loaded}. Việc đang tải tới trang mấy do nơi gọi giữ, listener không biết. Không tự kiểm tra
     * lại như {@link #loaded}.
     */
    public void reset() {
        this.loading = false;
        this.hasMore = true;
    }

    /** Đang có một lần tải chưa báo xong ({@link #loaded}, {@link #failed} hoặc {@link #reset} chưa được gọi). */
    public boolean isLoading() {
        return loading;
    }

    /** Còn trang sau không, theo lần {@link #loaded} hoặc {@link #reset} gần nhất. */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * Chỉ kiểm tra khi cuộn xuống (<code>dy &gt; 0</code>). <code>RecyclerView</code> cũng gọi hàm này với <code>dy == 0</code>
     * sau mỗi lần layout; kiểm tra ở đó sẽ khiến một lần tải lỗi bị thử lại liên tục, phá quy ước "lỗi thì chờ người dùng
     * cuộn lại". Cuộn lên (<code>dy &lt; 0</code>) cũng bị bỏ qua vì không thể đưa người dùng gần tới cuối hơn.
     */
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (dy > 0) {
            check();
        }
    }

    /**
     * Gọi <code>onLoadMore</code> nếu {@link #shouldLoad} cho phép. Cờ đang tải được bật <b>trước</b> khi gọi, để một lần
     * cuộn (hoặc một lần {@link #loaded}) không kích hoạt hai lần tải nếu <code>onLoadMore</code> chạy lại chính nó.
     * Bỏ qua im lặng khi <code>list</code> chưa có adapter hoặc LayoutManager, vì chưa có gì để đo.
     *
     * @throws IllegalStateException nếu LayoutManager không phải loại được hỗ trợ
     */
    private void check() {
        RecyclerView.Adapter<?> adapter = list.getAdapter();
        RecyclerView.LayoutManager layoutManager = list.getLayoutManager();
        if (adapter == null || layoutManager == null) {
            return;
        }
        // GridLayoutManager extends LinearLayoutManager nên sẽ qua check Layout.
        if (!(layoutManager instanceof LinearLayoutManager)) {
            throw new IllegalStateException("LoadMoreListener only supports LinearLayoutManager and GridLayoutManager, got "
                    + layoutManager.getClass().getSimpleName());
        }
        int lastVisible = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        if (shouldLoad(lastVisible, adapter.getItemCount(), threshold, loading, hasMore)) {
            loading = true;
            onLoadMore.run();
        }
    }

    /**
     * <p>Có nên tải trang kế không. Hàm thuần, không đụng View nên kiểm thử được trên JVM. Chỉ tải khi đồng thời:</p>
     * <ul>
     *   <li>còn trang sau (<code>hasMore</code>);</li>
     *   <li>không đang tải (<code>!loading</code>), để khỏi tải trùng;</li>
     *   <li>danh sách có dòng (<code>itemCount &gt; 0</code>), vì danh sách rỗng thì chưa có "gần cuối";</li>
     *   <li>đã có dòng hiện ra (<code>lastVisible &gt;= 0</code>);</li>
     *   <li>dòng cuối đang hiện nằm trong <code>threshold</code> dòng cuối:
     *       <code>lastVisible &gt;= itemCount - 1 - threshold</code>.</li>
     * </ul>
     * <p>Ví dụ 20 dòng, <code>threshold = 3</code>: tải khi dòng cuối đang hiện có chỉ số từ 16 trở lên.</p>
     *
     * @param lastVisible vị trí dòng cuối đang hiện, âm nếu chưa có dòng nào
     * @param itemCount   tổng số dòng của adapter
     * @param threshold   số dòng còn lại phía dưới để bắt đầu tải
     * @param loading     đang có một lần tải chưa báo xong
     * @param hasMore     còn trang sau
     */
    static boolean shouldLoad(int lastVisible, int itemCount, int threshold, boolean loading, boolean hasMore) {
        return hasMore && !loading && itemCount > 0 && lastVisible >= 0
                && lastVisible >= itemCount - 1 - threshold;
    }
}
