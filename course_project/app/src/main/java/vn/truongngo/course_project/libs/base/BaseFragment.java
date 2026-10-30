package vn.truongngo.course_project.libs.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * <p>Khuôn chung cho một Fragment: dựng layout, gom view vào container do người dùng viết, tạo {@link Lifetime}
 * rồi gọi các hook theo thứ tự cố định.</p>
 *
 * <p>Fragment sống lâu hơn View của nó (còn trong back stack nhưng View đã bị hủy), nên mọi thứ gắn với giao diện
 * theo <b>view lifecycle</b> chứ không theo lifecycle của Fragment: <code>lifetime()</code> kết thúc ở
 * <code>onDestroyView</code>, và mọi hook chạy lại mỗi lần View được tạo lại, không chỉ lần đầu.</p>
 *
 * <p>Thứ tự hook ở <code>onViewCreated</code>: {@link #initViews()}, {@link #initEvents()}, {@link #observe()},
 * {@link #loadData()}. Phân công với ViewModel: Fragment chỉ gọi hàm của ViewModel và vẽ state nhận được, không chạm
 * repository; dữ liệu và việc nền thuộc {@link BaseViewModel}.</p>
 *
 * @param <V> container giữ các view của Fragment, do người dùng viết (không sinh tự động)
 */
public abstract class BaseFragment<V> extends Fragment {

    /** Null trước <code>onCreateView</code> và sau <code>onDestroyView</code>. */
    protected V views;

    /** Null trước <code>onViewCreated</code>. Sau <code>onDestroyView</code> vẫn còn nhưng đã kết thúc. */
    private Lifetime lifetime;

    /** Layout của Fragment, ví dụ <code>R.layout.fragment_order_list</code>. */
    @LayoutRes
    protected abstract int layoutResId();

    /** Gom các view cần dùng từ <code>root</code> vừa dựng vào container. */
    @NonNull
    protected abstract V bindViews(@NonNull View root);

    /** Cấu hình view lần đầu (adapter, chữ mặc định...). */
    protected abstract void initViews();

    /** Đăng ký sự kiện (click, thay đổi chữ...). */
    protected void initEvents() {
    }

    /**
     * Đăng ký nghe state của ViewModel để vẽ ra view (xem {@link #observe(LiveData, Consumer)}). Chạy sau
     * {@link #initEvents()} và trước {@link #loadData()}, để kết quả của lần tải đầu không bị lỡ. Chạy lại mỗi lần View
     * được tạo lại vì observer cũ đã gỡ theo View cũ; <code>LiveData</code> gửi lại giá trị cuối nên View mới tự được
     * vẽ lại mà không cần tải lại.
     */
    protected void observe() {
    }

    /**
     * Yêu cầu ViewModel nạp dữ liệu ban đầu. Chạy lại mỗi lần View được tạo lại (ví dụ quay về từ back stack hoặc xoay
     * màn hình), nên ViewModel phải tự bỏ qua khi đã có dữ liệu thay vì tải lại.
     */
    protected void loadData() {
    }

    /**
     * Dựng layout với <code>container</code> nhưng <code>attachToRoot = false</code> (Fragment tự gắn View vào), rồi
     * gom view vào container. Chạy lại mỗi lần View được tạo lại.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(layoutResId(), container, false);
        views = bindViews(root);
        return root;
    }

    /** Tạo {@link Lifetime} theo view lifecycle rồi chạy bốn hook theo thứ tự cố định (xem mô tả lớp). */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lifetime = Lifetime.of(getViewLifecycleOwner().getLifecycle());

        initViews();
        initEvents();
        observe();
        loadData();
    }

    /** Nhả container để cây View đã hủy không bị giữ lại trong bộ nhớ. */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        views = null;
    }

    /**
     * Truy cập container, chỉ trong khoảng từ <code>onViewCreated</code> đến <code>onDestroyView</code>. Callback đến
     * trễ (async, listener) phải kiểm tra {@link #lifetime()} còn sống trước khi gọi.
     *
     * @throws IllegalStateException nếu View chưa được tạo hoặc đã bị hủy
     */
    @NonNull
    protected final V views() {
        if (views == null) {
            throw new IllegalStateException("The Fragment view has not been created or has been destroyed, views are not accessible now");
        }
        return views;
    }

    /**
     * ViewModel gắn với Fragment này: sống lâu hơn View (còn khi Fragment nằm trong back stack) và qua xoay màn hình,
     * chỉ bị hủy khi Fragment kết thúc hẳn. Không giữ View, container hay Fragment trong ViewModel.
     */
    @NonNull
    protected final <T extends ViewModel> T viewModel(@NonNull Class<T> type) {
        return new ViewModelProvider(this).get(type);
    }

    /**
     * ViewModel gắn với Activity chứa Fragment: các Fragment cùng Activity gọi cùng một lớp nhận cùng một thể hiện, để
     * chia sẻ dữ liệu giữa các tab. Chỉ gọi khi Fragment đã gắn vào Activity.
     */
    @NonNull
    protected final <T extends ViewModel> T activityViewModel(@NonNull Class<T> type) {
        return new ViewModelProvider(requireActivity()).get(type);
    }

    /**
     * Nghe <code>data</code> theo vòng đời của <b>View</b> (<code>getViewLifecycleOwner</code>), không phải của
     * Fragment: View bị hủy thì observer tự gỡ, tránh vẽ vào View đã chết và tránh đăng ký chồng khi View được tạo lại.
     * Giá trị có thể là <code>null</code> nếu <code>LiveData</code> chưa được gán.
     */
    protected final <T> void observe(@NonNull LiveData<T> data, @NonNull Consumer<T> onChanged) {
        data.observe(getViewLifecycleOwner(), onChanged::accept);
    }

    /**
     * Khoảng đời của View. Sau <code>onDestroyView</code> nó đã kết thúc: <code>isEnd()</code> trả true và các hành
     * động đăng ký bằng <code>onEnd</code> chạy ngay.
     *
     * @throws IllegalStateException nếu View chưa được tạo
     */
    @NonNull
    protected final Lifetime lifetime() {
        if (lifetime == null) {
            throw new IllegalStateException("The Fragment view has not been created, no Lifetime yet");
        }
        return lifetime;
    }
}
