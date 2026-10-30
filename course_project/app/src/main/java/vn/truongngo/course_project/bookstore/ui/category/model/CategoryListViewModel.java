package vn.truongngo.course_project.bookstore.ui.category.model;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.bookstore.repository.BookstoreDatabase;
import vn.truongngo.course_project.bookstore.repository.CategoryRepository;
import vn.truongngo.course_project.bookstore.ui.book.model.LoadState;
import vn.truongngo.course_project.libs.async.Job;
import vn.truongngo.course_project.libs.base.BaseViewModel;
import vn.truongngo.course_project.libs.base.Event;

/**
 * <p>State và việc nền của màn hình danh sách thể loại: nạp toàn bộ thể loại từ DB và giữ chúng qua xoay màn hình.
 * Danh sách thể loại nhỏ nên <b>không phân trang</b>: chỉ có một loại lần tải ({@link LoadState#REFRESHING}), khác màn
 * hình danh sách sách.</p>
 *
 * <p>Phân công với giao diện: Fragment chỉ gọi {@link #ensureLoaded()} và {@link #refresh()} rồi vẽ các
 * <code>LiveData</code> chỉ đọc; không chạm repository. Mọi state chỉ được đổi ở main thread (trong callback của
 * {@link #run}), còn việc nền chỉ làm phần thuần (đọc DB).</p>
 */
public class CategoryListViewModel extends BaseViewModel {

    private static final String TAG = CategoryListViewModel.class.getSimpleName();

    // Dependency

    /** Nơi đọc thể loại. Dùng chung một thể hiện cho cả tiến trình (xem {@link BookstoreDatabase}). */
    private final CategoryRepository categoryRepository = BookstoreDatabase.categoryRepository(getApplication());

    // UI state

    /**
     * Các thể loại đã nạp. Không có giá trị ban đầu: <code>null</code> nghĩa là <b>chưa nạp xong lần nào</b>, khác với
     * danh sách rỗng (đã nạp mà chưa có thể loại nào), để giao diện và {@link #ensureLoaded()} phân biệt được hai trường
     * hợp.
     */
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();

    /** Có đang nạp không: {@link LoadState#REFRESHING} trong lúc nạp, {@link LoadState#IDLE} khi không. */
    private final MutableLiveData<LoadState> loadState = new MutableLiveData<>(LoadState.IDLE);

    /** Lỗi cần báo người dùng; gói trong {@link Event} để xoay màn hình không hiện lại thông báo cũ. */
    private final MutableLiveData<Event<String>> error = new MutableLiveData<>();

    /**
     * Lần nạp đang chạy. Nạp thể loại chỉ đọc nên hủy được: {@link #refresh()} hủy lần cũ để kết quả cũ không về sau rồi
     * ghi đè kết quả mới.
     */
    private Job<?> currentJob;

    /**
     * Phải là <code>public</code> với đúng chữ ký <code>(Application)</code> để <code>ViewModelProvider</code> tạo được
     * bằng reflection.
     */
    public CategoryListViewModel(@NonNull Application application) {
        super(application);
    }

    // Getter

    /**
     * Danh sách thể loại đã nạp. Chưa có giá trị cho tới khi lần nạp đầu tiên xong; mỗi lần nạp xong chỉ phát đúng một
     * thay đổi.
     */
    @NonNull
    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    /** Trạng thái nạp. Luôn có giá trị, ban đầu là {@link LoadState#IDLE}. */
    @NonNull
    public LiveData<LoadState> getLoadState() {
        return loadState;
    }

    /** Thông báo lỗi cho người dùng, mỗi lỗi lấy ra được một lần qua {@link Event#consume()}. */
    @NonNull
    public LiveData<Event<String>> getError() {
        return error;
    }

    // Action

    /**
     * Nạp thể loại nếu chưa nạp lần nào. Gọi ở <code>loadData()</code> của Fragment, tức mỗi lần View được dựng lại, nên
     * phải <b>idempotent</b>: đã có dữ liệu thì bỏ qua (xoay màn hình không nạp lại), và đang nạp thì bỏ qua (xoay giữa
     * chừng không khởi động lần nạp trùng). Lần nạp đầu lỗi thì lần gọi sau (View dựng lại) được thử lại.
     */
    public void ensureLoaded() {
        if (categories.getValue() == null && !Objects.requireNonNull(loadState.getValue()).isLoading()) {
            load();
        }
    }

    /**
     * Nạp lại danh sách, dùng khi dữ liệu đã đổi từ nơi khác (ví dụ vừa thêm, sửa hoặc xóa thể loại ở màn form). Luôn nạp
     * lại, khác {@link #ensureLoaded()} ở chỗ không kiểm tra đã có dữ liệu; nếu đang có lần nạp thì hủy nó.
     */
    public void refresh() {
        load();
    }

    // Private

    /**
     * Hủy lần nạp đang chạy (nếu có) rồi nạp toàn bộ thể loại ở thread nền. Thành công thì thay danh sách; lỗi thì ghi
     * log và báo người dùng, danh sách cũ (nếu có) được giữ nguyên. Về {@link LoadState#IDLE} ở <code>onComplete</code>,
     * sau cả <code>onSuccess</code> lẫn <code>onError</code>.
     */
    private void load() {
        if (currentJob != null) {
            currentJob.cancel();
        }
        loadState.setValue(LoadState.REFRESHING);
        currentJob = run(
                categoryRepository::findAll,
                categories::setValue,
                e -> {
                    Log.e(TAG, "Failed to load categories", e);
                    error.setValue(new Event<>(getApplication().getString(R.string.error_load)));
                })
                .onComplete(() -> loadState.setValue(LoadState.IDLE));
    }
}
