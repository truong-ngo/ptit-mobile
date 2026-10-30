package vn.truongngo.course_project.libs.base;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * <p>Khuôn chung cho một khối giao diện tái sử dụng có logic riêng (timer, listener, việc nền cần dọn). Khối chỉ
 * hiển thị tĩnh thì dùng <code>&lt;include&gt;</code>, không cần lớp này.</p>
 *
 * <p>View không có lifecycle riêng, nên {@link Lifetime} ở đây là <b>một lần attach</b>: bắt đầu khi View gắn vào cửa
 * sổ, kết thúc khi bị detach. Không cần hỏi <code>LifecycleOwner</code>: Activity hay view của Fragment bị hủy thì View
 * cũng bị gỡ khỏi cửa sổ và <code>onDetachedFromWindow</code> tự chạy.</p>
 *
 * <p>Root của layout XML nên là <code>&lt;merge&gt;</code> để các view con được gắn thẳng vào <code>BaseLayout</code>.
 * <code>initViews</code> và <code>initEvents</code> chỉ chạy ở lần attach đầu; <code>loadData</code> chạy lại mỗi lần attach
 * (ví dụ item của <code>RecyclerView</code> cuộn vào lại), vì mỗi lần attach là một <code>Lifetime</code> mới.</p>
 *
 * @param <V> container giữ các view của khối giao diện, do người dùng viết (không sinh tự động)
 */
public abstract class BaseLayout<V> extends FrameLayout {

    /** Có sẵn ngay sau constructor. */
    protected final V views;

    /** Null trước lần attach đầu tiên. */
    private Lifetime lifetime;

    /** Đã chạy <code>initViews</code> và <code>initEvents</code> chưa, để hai hook đó chỉ chạy ở lần attach đầu. */
    private boolean initialized;

    /** Constructor khi tạo bằng code. */
    public BaseLayout(@NonNull Context context) {
        this(context, null);
    }

    /** Constructor khi được inflate từ XML; <code>LayoutInflater</code> gọi bản này. */
    public BaseLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Gọi {@link #layoutResId()} và {@link #bindViews} ngay trong constructor (lúc field của lớp con chưa được khởi
     * tạo), nên hai hàm đó không được đọc hay ghi field nào của lớp con.
     */
    public BaseLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(layoutResId(), this, true);
        views = bindViews(this);
    }

    /** Layout của khối giao diện, ví dụ <code>R.layout.view_countdown</code>. Chỉ trả về hằng số. */
    @LayoutRes
    protected abstract int layoutResId();

    /** Gom các view cần dùng vào container. <code>root</code> chính là <code>BaseLayout</code> này. */
    @NonNull
    protected abstract V bindViews(@NonNull View root);

    /** Cấu hình view lần đầu (adapter, chữ mặc định...). Chỉ chạy ở lần attach đầu tiên. */
    protected abstract void initViews();

    /** Đăng ký sự kiện (click, thay đổi chữ...). Chỉ chạy ở lần attach đầu tiên. */
    protected void initEvents() {
    }

    /** Nạp dữ liệu và đăng ký việc dọn dẹp qua {@link #lifetime()}. Chạy lại mỗi lần attach. */
    protected void loadData() {
    }

    /**
     * View sắp bị gỡ khỏi cửa sổ. Chạy <b>trước</b> các hành động <code>onEnd</code>, lúc {@link #lifetime()} còn sống,
     * nên hợp để log hoặc ghi lại trạng thái giao diện. Dọn tài nguyên thì dùng <code>lifetime().onEnd(...)</code> ngay
     * chỗ đăng ký, không dọn ở đây. Detach không phải hủy: View có thể được attach lại.
     */
    protected void onDetach() {
    }

    /**
     * Tạo {@link Lifetime} mới cho lần attach này rồi chạy hook (xem mô tả lớp). Bỏ qua trong Layout Editor, vì không có
     * Activity và hook chứa logic ứng dụng có thể làm hỏng bản xem trước.
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (isInEditMode()) {
            return;
        }

        lifetime = Lifetime.manual();

        if (!initialized) {
            initialized = true;
            initViews();
            initEvents();
        }
        loadData();
    }

    /** Gọi {@link #onDetach()} rồi kết thúc {@link #lifetime()} của lần attach này. Không làm gì nếu chưa từng attach. */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (lifetime != null) {
            onDetach();
            lifetime.end();
        }
    }

    /**
     * Khoảng đời của lần attach hiện tại. Sau khi detach nó đã kết thúc: <code>isEnd()</code> trả true và các hành
     * động đăng ký bằng <code>onEnd</code> chạy ngay.
     *
     * @throws IllegalStateException nếu View chưa từng được attach
     */
    @NonNull
    protected final Lifetime lifetime() {
        if (lifetime == null) {
            throw new IllegalStateException("The view has not been attached yet, no Lifetime");
        }
        return lifetime;
    }
}
