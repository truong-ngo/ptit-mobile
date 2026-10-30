package vn.truongngo.course_project.bookstore.ui.category.model;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.bookstore.repository.BookstoreDatabase;
import vn.truongngo.course_project.bookstore.repository.CategoryRepository;
import vn.truongngo.course_project.libs.base.BaseViewModel;
import vn.truongngo.course_project.libs.base.Event;

/**
 * <p>State và việc nền của màn hình form thể loại, dùng chung cho <b>thêm</b> và <b>sửa</b>: <code>id</code> rỗng là
 * thêm, có <code>id</code> là sửa (khi đó nạp thể loại từ DB để đổ vào form). Cũng lo việc <b>xóa</b>, vì xóa chỉ có ở
 * chế độ sửa.</p>
 *
 * <p>Phân công với giao diện: Fragment chỉ gọi các hàm công khai ({@link #ensureLoaded}, {@link #save},
 * {@link #delete}) rồi vẽ các <code>LiveData</code> chỉ đọc; không chạm repository. Mọi state chỉ được đổi ở main thread
 * (trong callback của {@link #run}), còn việc nền chỉ làm phần thuần (đọc, ghi DB) trên dữ liệu đã chụp sẵn.</p>
 *
 * <p>Nội dung các ô nhập <b>không</b> nằm ở đây: Android tự lưu và khôi phục chữ trong ô có id khi xoay màn hình. Chỉ
 * những gì phải sống qua xoay màn hình mà view không giữ được (kết quả nạp, cờ đang ghi, sự kiện chưa được xử lý) mới nằm
 * trong lớp này.</p>
 */
public class CategoryFormViewModel extends BaseViewModel {

    // Constant

    private static final String TAG = CategoryFormViewModel.class.getSimpleName();

    /** Giá trị của {@link #getDone()} khi vừa <b>thêm</b> một thể loại mới. */
    public static final String ACTION_CREATED = "created";

    /** Giá trị của {@link #getDone()} khi vừa <b>cập nhật</b> một thể loại có sẵn. */
    public static final String ACTION_UPDATED = "updated";

    /** Giá trị của {@link #getDone()} khi vừa <b>xóa</b> thể loại. */
    public static final String ACTION_DELETED = "deleted";

    // Dependency

    /** Nơi đọc và ghi thể loại. Dùng chung một thể hiện cho cả tiến trình (xem {@link BookstoreDatabase}). */
    private final CategoryRepository categoryRepository = BookstoreDatabase.categoryRepository(getApplication());

    // Identity and form data

    /** Mã thể loại đang mở; <code>null</code> khi thêm mới. Được đặt lại ở mỗi lần {@link #ensureLoaded}. */
    private Long id;

    /**
     * Form của thể loại đang sửa, mang <code>id</code>; <code>null</code> khi thêm mới hoặc chưa nạp xong. Ô nhập phải
     * được đọc <b>vào chính đối tượng này</b> (<code>FormBinder#readInto</code>) chứ không dùng <code>read()</code>:
     * <code>read()</code> tạo form mới và làm mất <code>id</code>, biến việc sửa thành thêm mới.
     */
    private CategoryForm editing;

    /** Đang nạp thể loại để sửa, để View dựng lại giữa chừng (xoay màn hình) không khởi động thêm một lần nạp trùng. */
    private boolean loadingForm;

    // UI state

    /**
     * Form cần đổ lên các ô, sau khi nạp xong thể loại cần sửa. Gói trong {@link Event} để chỉ giao <b>một lần</b>: nếu
     * không, xoay màn hình sẽ đổ lại và ghi đè chữ người dùng đã sửa dở.
     */
    private final MutableLiveData<Event<CategoryForm>> formToShow = new MutableLiveData<>();

    /** Việc ghi (thêm, sửa hoặc xóa) vừa xong; nội dung là một trong các <code>ACTION_*</code>. */
    private final MutableLiveData<Event<String>> done = new MutableLiveData<>();

    /** Đang ghi (thêm, sửa hoặc xóa), để giao diện khóa nút và để {@link #save}, {@link #delete} chặn thao tác đôi. */
    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);

    /** Lỗi cần báo người dùng (nạp hay ghi lỗi); gói trong {@link Event} để xoay màn hình không hiện lại thông báo cũ. */
    private final MutableLiveData<Event<String>> error = new MutableLiveData<>();

    /**
     * Đang ở chế độ sửa mà không nạp được thể loại: form không có dữ liệu để sửa nên không được lưu hay xóa. Không có dấu
     * này thì {@link #formForSave()} trả form trống không có <code>id</code>, và bấm Lưu sẽ <b>tạo thể loại mới</b> thay
     * vì sửa.
     */
    private final MutableLiveData<Boolean> formLoadFailed = new MutableLiveData<>(false);

    /**
     * Phải là <code>public</code> với đúng chữ ký <code>(Application)</code> để <code>ViewModelProvider</code> tạo được
     * bằng reflection.
     */
    public CategoryFormViewModel(@NonNull Application application) {
        super(application);
    }

    // Getter

    /**
     * Form cần đổ lên các ô (chế độ sửa), giao đúng một lần qua {@link Event#consume()}. Chưa có giá trị cho tới khi nạp
     * xong thể loại.
     */
    @NonNull
    public LiveData<Event<CategoryForm>> getFormToShow() {
        return formToShow;
    }

    /**
     * Một lần ghi vừa xong, nội dung là {@link #ACTION_CREATED}, {@link #ACTION_UPDATED} hoặc {@link #ACTION_DELETED}.
     * Giao diện lấy ra bằng {@link Event#consume()} rồi báo màn hình trước và đóng form.
     */
    @NonNull
    public LiveData<Event<String>> getDone() {
        return done;
    }

    /** True khi đang ghi (thêm, sửa hoặc xóa). Luôn có giá trị, ban đầu là <code>false</code>. */
    @NonNull
    public LiveData<Boolean> getProcessing() {
        return processing;
    }

    /** Thông báo lỗi cho người dùng, mỗi lỗi lấy ra được một lần qua {@link Event#consume()}. */
    @NonNull
    public LiveData<Event<String>> getError() {
        return error;
    }

    /**
     * True khi đang sửa mà không nạp được thể loại; giao diện nên khóa nút Lưu và Xóa. Luôn có giá trị, ban đầu là
     * <code>false</code>.
     */
    @NonNull
    public LiveData<Boolean> getFormLoadFailed() {
        return formLoadFailed;
    }

    /**
     * Tên của thể loại đang sửa, để nêu trong câu xác nhận xóa. Rỗng nếu đang thêm mới hoặc chưa nạp xong. Trả chuỗi chứ
     * không trả {@link #editing} để giao diện không giữ được tham chiếu tới đối tượng sửa được.
     */
    @NonNull
    public String editingName() {
        return Optional.ofNullable(editing).map(CategoryForm::getName).orElse("");
    }

    /**
     * Form để đọc các ô nhập vào khi bấm Lưu: bản đã nạp (giữ <code>id</code>) khi sửa, bản trống khi thêm. Mỗi lần gọi ở
     * chế độ thêm cho một bản trống mới, nên không có dữ liệu của lần nhập trước dính sang lần sau.
     */
    @NonNull
    public CategoryForm formForSave() {
        return Optional.ofNullable(editing).orElseGet(CategoryForm::empty);
    }

    // Action

    /**
     * <p>Ghi nhớ thể loại đang mở và nạp nó từ DB nếu là sửa. Gọi ở <code>loadData()</code> của Fragment, tức mỗi lần View
     * được dựng lại, nên phải <b>idempotent</b>: thêm mới thì không có gì để nạp; sửa mà đã nạp rồi hoặc đang nạp thì bỏ
     * qua.</p>
     *
     * <p>Nạp lỗi thì báo người dùng và bật {@link #getFormLoadFailed()}; {@link #loadingForm} vẫn được hạ ở
     * <code>onComplete</code> nên lần gọi sau (View dựng lại) được thử lại.</p>
     *
     * @param id mã thể loại cần sửa, hoặc <code>null</code> khi thêm mới
     */
    public void ensureLoaded(@Nullable Long id) {
        this.id = id;
        if (id == null || editing != null || loadingForm) {
            return;
        }
        loadingForm = true;
        run(() -> CategoryForm.from(categoryRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalStateException("Category not found: " + id))),
                form -> {
                    editing = form;
                    formToShow.setValue(new Event<>(form));
                },
                e -> {
                    Log.e(TAG, "Failed to load category", e);
                    error.setValue(new Event<>(getApplication().getString(R.string.error_load)));
                    formLoadFailed.setValue(true);
                })
                .onComplete(() -> loadingForm = false);
    }

    /**
     * <p>Lưu form: thêm nếu form chưa có <code>id</code>, cập nhật nếu có. Xong thì phát {@link #getDone()}, lỗi thì phát
     * {@link #getError()}.</p>
     *
     * <p>Bỏ qua khi đang ghi. Với việc <b>ghi</b> không hủy job cũ rồi tạo job mới như việc đọc: hủy chỉ bỏ kết quả chứ
     * không dừng câu lệnh đang chạy, nên làm vậy sẽ ghi đôi. Chỉ từ chối lần gọi thứ hai mới an toàn.</p>
     *
     * @param form form đã qua kiểm tra (<code>FormBinder#readInto</code> trả <code>true</code>)
     */
    public void save(@NonNull CategoryForm form) {
        if (Boolean.TRUE.equals(processing.getValue())) {
            return;
        }
        Category category = form.toCategory();      // main thread: snapshot trước khi đẩy cho thread nền
        boolean creating = category.getId() == null;
        processing.setValue(true);
        run(() -> {
                    if (creating) categoryRepository.insert(category);
                    else categoryRepository.update(category);
                    return category;
                },
                saved -> done.setValue(new Event<>(creating ? ACTION_CREATED : ACTION_UPDATED)),
                this::onWriteError)
                .onComplete(() -> processing.setValue(false));
    }

    /**
     * <p>Xóa thể loại đang sửa. Các sách thuộc nó <b>không</b> bị xóa: khóa ngoại khai <code>SET_NULL</code> nên chúng
     * chuyển thành chưa phân loại. Chỉ có nghĩa ở chế độ sửa; thêm mới thì bỏ qua.</p>
     *
     * <p>Cũng chặn thao tác đôi bằng {@link #getProcessing()}, cùng lý do với {@link #save}.</p>
     */
    public void delete() {
        if (id == null || Boolean.TRUE.equals(processing.getValue())) {
            return;
        }
        Long target = id;                           // snapshot, việc nền không đọc field của ViewModel
        processing.setValue(true);
        run(() -> categoryRepository.deleteById(target),
                deleted -> done.setValue(new Event<>(ACTION_DELETED)),
                this::onWriteError)
                .onComplete(() -> processing.setValue(false));
    }

    // Utilities

    /**
     * Báo lỗi ghi cho người dùng. Tên thể loại trùng (cột <code>name</code> có <code>unique</code>) làm SQLite ném
     * {@link SQLiteConstraintException} nên có thông báo riêng; lỗi khác báo chung. Chạy ở main thread (callback
     * <code>onError</code> của {@link #run}).
     */
    private void onWriteError(Throwable e) {
        Log.e(TAG, "Failed to write category", e);
        int message = e instanceof SQLiteConstraintException ? R.string.category_name_taken : R.string.error_save;
        error.setValue(new Event<>(getApplication().getString(message)));
    }
}
