package vn.truongngo.course_project.libs.base;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * <p>Khuôn chung cho một Activity: dựng layout, gom các view cần dùng vào một container do người dùng viết, tạo
 * {@link Lifetime} theo vòng đời Activity rồi gọi các hook theo thứ tự cố định.</p>
 *
 * <p>Thứ tự trong <code>onCreate</code>: <code>layoutResId</code>, <code>bindViews</code>, <code>initViews</code>,
 * <code>initEvents</code>, <code>loadData</code>. <code>loadData</code> chạy lại mỗi lần Activity được tạo lại
 * (xoay màn hình), nên dữ liệu cần sống qua việc đó phải để ở <code>ViewModel</code>.</p>
 *
 * @param <V> container giữ các view của màn hình, do người dùng viết (không sinh tự động)
 */
public abstract class BaseActivity<V> extends AppCompatActivity {

    /** Có sẵn từ khi các hook chạy; null trước <code>onCreate</code>. */
    protected V views;

    /** Gắn với vòng đời Activity, tạo ở <code>onCreate</code>. Truy cập qua {@link #lifetime()}. */
    private Lifetime lifetime;

    /** Layout của màn hình, ví dụ <code>R.layout.activity_login</code>. */
    @LayoutRes
    protected abstract int layoutResId();

    /**
     * Gom các view cần dùng từ <code>root</code> vừa dựng vào container.
     * Lúc này <code>setContentView</code> đã xong, nên <code>root</code> đã gắn vào cửa sổ.
     */
    @NonNull
    protected abstract V bindViews(@NonNull View root);

    /** Cấu hình view lần đầu (adapter, chữ mặc định...). */
    protected abstract void initViews();

    /** Đăng ký sự kiện (click, thay đổi chữ...). */
    protected void initEvents() {
    }

    /** Nạp dữ liệu ban đầu. Chạy lại mỗi lần Activity được tạo lại. */
    protected void loadData() {
    }

    /**
     * Bật edge-to-edge, dựng layout rồi chạy các hook theo thứ tự cố định (xem mô tả lớp). Lớp con không nên override;
     * cần làm gì ở giai đoạn nào thì dùng hook tương ứng.
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        lifetime = Lifetime.of(getLifecycle());

        setContentView(layoutResId());
        // Root của layout là con duy nhất của khung nội dung (android.R.id.content).
        View root = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        views = bindViews(root);
        applyWindowInsets(root);

        initViews();
        initEvents();
        loadData();
    }

    /**
     * Với targetSdk 35 trở lên, nội dung bị vẽ tràn dưới thanh trạng thái, thanh điều hướng và bàn phím. Mặc định cộng
     * các insets đó vào padding sẵn có của <code>root</code> (không ghi đè). Màn hình muốn tự xử lý thì override.
     */
    protected void applyWindowInsets(@NonNull View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() |
                             WindowInsetsCompat.Type.displayCutout() |
                             WindowInsetsCompat.Type.ime());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Gắn với vòng đời Activity: kết thúc ở <code>onDestroy</code>, kể cả khi Activity bị hủy để xoay màn hình. */
    @NonNull
    protected Lifetime lifetime() {
        return lifetime;
    }

    /**
     * ViewModel gắn với Activity này: sống qua xoay màn hình, chỉ bị hủy khi Activity kết thúc hẳn. Gọi nhiều lần với
     * cùng một lớp thì trả về cùng một thể hiện. Không giữ View, container hay Activity trong ViewModel.
     */
    @NonNull
    protected final <T extends ViewModel> T viewModel(@NonNull Class<T> type) {
        return new ViewModelProvider(this).get(type);
    }
}
