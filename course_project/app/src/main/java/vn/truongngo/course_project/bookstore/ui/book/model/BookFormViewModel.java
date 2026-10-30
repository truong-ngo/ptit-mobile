package vn.truongngo.course_project.bookstore.ui.book.model;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.bookstore.repository.BookRepository;
import vn.truongngo.course_project.bookstore.repository.BookstoreDatabase;
import vn.truongngo.course_project.bookstore.repository.CategoryRepository;
import vn.truongngo.course_project.libs.base.BaseViewModel;
import vn.truongngo.course_project.libs.base.Event;

/**
 * <p>State và việc nền của màn hình form sách, dùng chung cho <b>thêm</b> và <b>sửa</b>: <code>id</code> rỗng là thêm, có
 * <code>id</code> là sửa (khi đó nạp sách từ DB để đổ vào form). Cũng lo việc <b>xóa</b>, vì xóa chỉ có ở chế độ sửa. Khác
 * form thể loại ở chỗ còn phải nạp <b>danh sách thể loại</b> cho spinner, ở cả hai chế độ.</p>
 *
 * <p>Phân công với giao diện: Fragment chỉ gọi các hàm công khai ({@link #ensureLoaded}, {@link #save}, {@link #delete})
 * rồi vẽ các <code>LiveData</code> chỉ đọc; không chạm repository. Mọi state chỉ được đổi ở main thread (trong callback
 * của {@link #run}), còn việc nền chỉ làm phần thuần (đọc, ghi DB) trên dữ liệu đã chụp sẵn.</p>
 *
 * <p>Nội dung các ô nhập <b>không</b> nằm ở đây: Android tự lưu và khôi phục chữ trong ô có id khi xoay màn hình. Chỉ
 * những gì phải sống qua xoay màn hình mà view không giữ được (kết quả nạp, cờ đang ghi, sự kiện chưa được xử lý) mới nằm
 * trong lớp này.</p>
 */
public class BookFormViewModel extends BaseViewModel {

    // Constant

    private static final String TAG = BookFormViewModel.class.getSimpleName();

    /** Giá trị của {@link #getDone()} khi vừa <b>thêm</b> một sách mới. */
    public static final String ACTION_CREATED = "created";

    /** Giá trị của {@link #getDone()} khi vừa <b>cập nhật</b> một sách có sẵn. */
    public static final String ACTION_UPDATED = "updated";

    /** Giá trị của {@link #getDone()} khi vừa <b>xóa</b> sách. */
    public static final String ACTION_DELETED = "deleted";

    // Dependency

    /** Nơi đọc thể loại cho spinner. Dùng chung một thể hiện cho cả tiến trình (xem {@link BookstoreDatabase}). */
    private final CategoryRepository categoryRepository = BookstoreDatabase.categoryRepository(getApplication());

    /** Nơi đọc và ghi sách. Dùng chung một thể hiện cho cả tiến trình (xem {@link BookstoreDatabase}). */
    private final BookRepository bookRepository = BookstoreDatabase.bookRepository(getApplication());

    // Identity and form data

    /** Mã sách đang mở; <code>null</code> khi thêm mới. Được đặt lại ở mỗi lần {@link #ensureLoaded}. */
    private Long id;

    /**
     * Form của sách đang sửa, mang <code>id</code>; <code>null</code> khi thêm mới hoặc chưa nạp xong. Ô nhập phải được
     * đọc <b>vào chính đối tượng này</b> (<code>FormBinder#readInto</code>) chứ không dùng <code>read()</code>:
     * <code>read()</code> tạo form mới và làm mất <code>id</code>, biến việc sửa thành thêm mới.
     */
    private BookForm editing;

    /**
     * Đang nạp dữ liệu cho form (thể loại, và sách nếu là sửa), để View dựng lại giữa chừng (xoay màn hình) không khởi
     * động thêm một lần nạp trùng.
     */
    private boolean loadingForm;

    // UI state

    /**
     * Form cần đổ lên các ô, sau khi nạp xong sách cần sửa. Gói trong {@link Event} để chỉ giao <b>một lần</b>: nếu không,
     * xoay màn hình sẽ đổ lại và ghi đè chữ người dùng đã sửa dở.
     */
    private final MutableLiveData<Event<BookForm>> formToShow = new MutableLiveData<>();

    /**
     * Các thể loại để chọn trong spinner. Không có giá trị ban đầu: <code>null</code> nghĩa là chưa nạp xong lần nào, để
     * {@link #ensureLoaded} biết còn phải nạp.
     */
    private final MutableLiveData<List<Category>> categories = new MutableLiveData<>();

    /** Đang ghi (thêm, sửa hoặc xóa), để giao diện khóa nút và để {@link #save}, {@link #delete} chặn thao tác đôi. */
    private final MutableLiveData<Boolean> processing = new MutableLiveData<>(false);

    /** Lỗi cần báo người dùng (nạp hay ghi lỗi); gói trong {@link Event} để xoay màn hình không hiện lại thông báo cũ. */
    private final MutableLiveData<Event<String>> error = new MutableLiveData<>();

    /** Việc ghi (thêm, sửa hoặc xóa) vừa xong; nội dung là một trong các <code>ACTION_*</code>. */
    private final MutableLiveData<Event<String>> done = new MutableLiveData<>();

    /**
     * Đang ở chế độ sửa mà không nạp được sách hoặc thể loại: form không có dữ liệu để sửa nên không được lưu hay xóa.
     * Không có dấu này thì {@link #formForSave()} trả form trống không có <code>id</code>, và bấm Lưu sẽ <b>tạo sách
     * mới</b> thay vì sửa.
     */
    private final MutableLiveData<Boolean> formLoadFailed = new MutableLiveData<>(false);

    /**
     * Phải là <code>public</code> với đúng chữ ký <code>(Application)</code> để <code>ViewModelProvider</code> tạo được
     * bằng reflection.
     */
    public BookFormViewModel(@NonNull Application application) {
        super(application);
    }

    // Getter

    /**
     * Form cần đổ lên các ô (chế độ sửa), giao đúng một lần qua {@link Event#consume()}. Chưa có giá trị cho tới khi nạp
     * xong sách.
     */
    @NonNull
    public LiveData<Event<BookForm>> getFormToShow() {
        return formToShow;
    }

    /** Các thể loại để chọn trong spinner. Chưa có giá trị cho tới khi nạp xong; giao diện nạp lại spinner mỗi khi đổi. */
    @NonNull
    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    /** True khi đang ghi (thêm, sửa hoặc xóa). Luôn có giá trị, ban đầu là <code>false</code>. */
    @NonNull
    public LiveData<Boolean> getProcessing() {
        return processing;
    }

    /**
     * Một lần ghi vừa xong, nội dung là {@link #ACTION_CREATED}, {@link #ACTION_UPDATED} hoặc {@link #ACTION_DELETED}.
     * Giao diện lấy ra bằng {@link Event#consume()} rồi báo màn hình trước và đóng form.
     */
    @NonNull
    public LiveData<Event<String>> getDone() {
        return done;
    }

    /** Thông báo lỗi cho người dùng, mỗi lỗi lấy ra được một lần qua {@link Event#consume()}. */
    @NonNull
    public LiveData<Event<String>> getError() {
        return error;
    }

    /**
     * True khi đang sửa mà không nạp được sách hoặc thể loại; giao diện nên khóa nút Lưu và Xóa. Luôn có giá trị, ban đầu
     * là <code>false</code>.
     */
    @NonNull
    public LiveData<Boolean> getFormLoadFailed() {
        return formLoadFailed;
    }

    /**
     * Tên sách đang sửa, để nêu trong câu xác nhận xóa. Rỗng nếu đang thêm mới hoặc chưa nạp xong. Trả chuỗi chứ không trả
     * {@link #editing} để giao diện không giữ được tham chiếu tới đối tượng sửa được.
     */
    @NonNull
    public String editingName() {
        return Optional.ofNullable(editing).map(BookForm::getTitle).orElse("");
    }

    /**
     * Form để đọc các ô nhập vào khi bấm Lưu: bản đã nạp (giữ <code>id</code>) khi sửa, bản trống khi thêm. Mỗi lần gọi ở
     * chế độ thêm cho một bản trống mới, nên không có dữ liệu của lần nhập trước dính sang lần sau.
     */
    @NonNull
    public BookForm formForSave() {
        return Optional.ofNullable(editing).orElseGet(BookForm::empty);
    }

    // Action

    /**
     * <p>Ghi nhớ sách đang mở và nạp những gì form còn thiếu. Gọi ở <code>loadData()</code> của Fragment, tức mỗi lần View
     * được dựng lại, nên phải <b>idempotent</b>: đang nạp thì bỏ qua; thể loại đã có thì không nạp lại; sách chỉ nạp khi
     * là sửa và chưa nạp.</p>
     *
     * <p>Khi cần cả hai thì nạp <b>thể loại trước, sách sau</b>: spinner phải có đủ thể loại trước khi đổ form, nếu không
     * thể loại của sách không khớp mục nào và bị đưa về "chưa phân loại". Một trong hai lỗi thì báo người dùng; riêng ở chế
     * độ sửa còn bật {@link #getFormLoadFailed()}. Cờ {@link #loadingForm} luôn được hạ khi xong, nên lần gọi sau (View
     * dựng lại) được thử lại.</p>
     *
     * @param id mã sách cần sửa, hoặc <code>null</code> khi thêm mới
     */
    public void ensureLoaded(@Nullable Long id) {
        this.id = id;
        if (loadingForm) return;
        boolean needCategories = categories.getValue() == null;
        boolean needBook = id != null && editing == null;
        if (!needCategories && !needBook) return;
        loadingForm = true;
        if (needCategories) {
            run(
                    categoryRepository::findAll,
                    list -> {
                        categories.setValue(list);
                        if (needBook) loadBook(id);
                    },
                    e -> {
                        onLoadError(e);
                        if (needBook) failLoadingForm();
                    })
                    .onComplete(() -> {
                        // Còn sách phải nạp thì loadBook mới là nơi hạ cờ, hạ ở đây sẽ mở cửa cho một lần nạp trùng.
                        if (!needBook) loadingForm = false;
                    });
        } else {
            loadBook(id);
        }
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
    public void save(@NonNull BookForm form) {
        if (Boolean.TRUE.equals(processing.getValue())) {
            return;
        }
        Book book = form.toBook();                  // main thread: snapshot trước khi đẩy cho thread nền
        boolean creating = book.getId() == null;
        processing.setValue(true);
        run(() -> {
                    if (creating) bookRepository.insert(book);
                    else bookRepository.update(book);
                    return book;
                },
                saved -> done.setValue(new Event<>(creating ? ACTION_CREATED : ACTION_UPDATED)),
                this::onWriteError)
                .onComplete(() -> processing.setValue(false));
    }

    /**
     * <p>Xóa sách đang sửa. Chỉ có nghĩa ở chế độ sửa; thêm mới thì bỏ qua.</p>
     *
     * <p>Cũng chặn thao tác đôi bằng {@link #getProcessing()}, cùng lý do với {@link #save}.</p>
     */
    public void delete() {
        if (id == null || Boolean.TRUE.equals(processing.getValue())) {
            return;
        }
        Long target = id;                           // snapshot, việc nền không đọc field của ViewModel
        processing.setValue(true);
        run(() -> bookRepository.deleteById(target),
                deleted -> done.setValue(new Event<>(ACTION_DELETED)),
                this::onWriteError)
                .onComplete(() -> processing.setValue(false));
    }

    // Utilities

    /**
     * Nạp sách cần sửa rồi đổ vào form. Chỉ gọi khi {@link #loadingForm} đang bật và thể loại đã có (xem
     * {@link #ensureLoaded}). Lỗi thì báo người dùng và đánh dấu form hỏng; {@link #loadingForm} hạ ở
     * <code>onComplete</code> trong cả hai trường hợp.
     *
     * @param id mã sách cần nạp
     */
    private void loadBook(Long id) {
        run(
                () -> BookForm.from(bookRepository
                        .findById(id)
                        .orElseThrow(() -> new IllegalStateException("Book not found: " + id))),
                form -> {
                    editing = form;
                    formToShow.setValue(new Event<>(form));
                },
                e -> {
                    onLoadError(e);
                    failLoadingForm();
                })
                .onComplete(() -> loadingForm = false);
    }

    /** Không dựng được form để sửa: hạ cờ đang nạp và đánh dấu form hỏng, xem {@link #formLoadFailed}. */
    private void failLoadingForm() {
        loadingForm = false;
        formLoadFailed.setValue(true);
    }

    /**
     * Báo lỗi ghi cho người dùng. Sách không có cột nào <code>unique</code> nên vi phạm ràng buộc ở đây chỉ có thể là khóa
     * ngoại: thể loại đã chọn vừa bị xóa ở nơi khác trong lúc form đang mở. Lỗi khác báo chung. Chạy ở main thread
     * (callback <code>onError</code> của {@link #run}).
     */
    private void onWriteError(Throwable e) {
        Log.e(TAG, "Failed to write book", e);
        int message = e instanceof SQLiteConstraintException ? R.string.book_category_missing : R.string.error_save;
        error.setValue(new Event<>(getApplication().getString(message)));
    }

    /** Báo lỗi nạp dữ liệu (sách hoặc thể loại) cho người dùng. Chạy ở main thread. */
    private void onLoadError(Throwable e) {
        Log.e(TAG, "Failed to load data", e);
        error.setValue(new Event<>(getApplication().getString(R.string.error_load)));
    }
}
