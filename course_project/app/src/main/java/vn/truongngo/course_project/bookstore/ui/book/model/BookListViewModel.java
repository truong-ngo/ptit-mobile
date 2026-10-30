package vn.truongngo.course_project.bookstore.ui.book.model;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.bookstore.repository.BookRepository;
import vn.truongngo.course_project.bookstore.repository.BookstoreDatabase;
import vn.truongngo.course_project.bookstore.repository.CategoryRepository;
import vn.truongngo.course_project.libs.async.Job;
import vn.truongngo.course_project.libs.base.BaseViewModel;
import vn.truongngo.course_project.libs.base.Event;
import vn.truongngo.course_project.libs.repository.PageRequest;
import vn.truongngo.course_project.libs.repository.specification.Criteria;

/**
 * <p>State và việc nền của màn hình danh sách sách: các trang đã tải, trang kế tiếp, và {@link BookFilterModel} (bộ lọc
 * đang áp dụng cùng thể loại để chọn). Giao diện chỉ gọi {@link #ensureLoaded()}, {@link #applyFilter},
 * {@link #loadNextPage()} rồi vẽ {@link #getBooks()}, {@link #getLoadState()} và {@link BookFilterModel#getCategories()}.</p>
 *
 * <p>Đây là ViewModel duy nhất của màn hình: {@link BookFilterModel} chỉ là lớp state thường nằm trong nó, nên mọi việc
 * nền (tải sách, tải thể loại) đều do lớp này chạy và đổi state ở main thread. Việc nền chỉ đọc DB trên dữ liệu đã chụp
 * sẵn.</p>
 */
public class BookListViewModel extends BaseViewModel {

    // Constant

    private static final String TAG = BookListViewModel.class.getSimpleName();

    /** Số sách mỗi trang. */
    private static final int PAGE_SIZE = 20;

    // Dependency

    /** Nơi đọc sách. Dùng chung một thể hiện cho cả tiến trình (xem {@link BookstoreDatabase}). */
    private final BookRepository bookRepository;

    /** Nơi đọc thể loại cho spinner của bộ lọc. Dùng chung một thể hiện cho cả tiến trình. */
    private final CategoryRepository categoryRepository;

    // UI state

    /** State của khối lọc: bộ lọc đang áp dụng và thể loại để chọn. */
    private final BookFilterModel filter = new BookFilterModel();

    /** Không có giá trị ban đầu, để observer không nhận một danh sách rỗng giả trước khi trang đầu về. */
    private final MutableLiveData<List<Book>> books = new MutableLiveData<>();

    /** Lần tải đang chạy thuộc loại nào, hoặc {@link LoadState#IDLE} khi không tải. Chỉ đổi ở main thread. */
    private final MutableLiveData<LoadState> loadState = new MutableLiveData<>(LoadState.IDLE);

    /** Lỗi cần báo người dùng; gói trong {@link Event} để xoay màn hình không hiện lại thông báo cũ. */
    private final MutableLiveData<Event<String>> error = new MutableLiveData<>();

    /** Phát mỗi khi trang đầu thay thế danh sách. Xem {@link #getScrollToTop()}. */
    private final MutableLiveData<Event<Boolean>> scrollToTop = new MutableLiveData<>();

    // Paging state

    /** Trang sẽ tải ở lần gọi kế tiếp, tiến lên sau mỗi lần tải thành công. */
    private PageRequest nextPage = PageRequest.of(0, PAGE_SIZE);

    /** Có trang kế tiếp để tải không, để load more. Đặt lại thành <code>true</code> mỗi khi quay về trang đầu. */
    private boolean hasNext = true;

    /** Đã có ít nhất một lần tải thành công, để xoay màn hình không tải lại. */
    private boolean loaded = false;

    /**
     * Lần tải gần nhất có lỗi không; đặt lại ở đầu mỗi {@link #fetch()}, nên luôn nói về lần tải hiện tại. Giao diện đọc
     * nó khi {@link #getLoadState()} về {@link LoadState#IDLE} để biết lần tải vừa kết thúc theo kiểu nào.
     */
    private boolean lastLoadFailed = false;

    // Running work

    /** Lần tải đang chạy, để đổi bộ lọc thì hủy kết quả cũ, tránh trang của bộ lọc cũ ghi đè bộ lọc mới. */
    private Job<?> currentJob;

    /**
     * Lần nạp thể loại đang chạy, để lần nạp mới hủy lần cũ. Tách khỏi {@link #currentJob} (xem
     * {@link #loadCategories()}).
     */
    private Job<?> categoriesJob;

    /** Phiên bản thể loại (từ <code>CatalogViewModel</code>) đã nạp, để chỉ nạp lại khi có phiên bản khác. */
    private int appliedCategoriesVersion = 0;

    /**
     * Phải là <code>public</code> với đúng chữ ký <code>(Application)</code> để <code>ViewModelProvider</code> tạo được
     * bằng reflection.
     */
    public BookListViewModel(@NonNull Application application) {
        super(application);
        bookRepository = BookstoreDatabase.bookRepository(application);
        categoryRepository = BookstoreDatabase.categoryRepository(application);
    }

    // Getter

    /**
     * Các sách đã tải của bộ lọc hiện tại, gồm mọi trang đã nối. Chưa có giá trị cho tới khi trang đầu tiên về, và mỗi
     * lần tải xong một trang chỉ phát đúng một thay đổi. Đổi bộ lọc thì danh sách cũ được giữ tới khi trang đầu của bộ
     * lọc mới về rồi thay thế luôn.
     */
    @NonNull
    public LiveData<List<Book>> getBooks() {
        return books;
    }

    /**
     * Trạng thái tải: {@link LoadState#REFRESHING} khi tải trang đầu (mở màn hình, đổi bộ lọc),
     * {@link LoadState#LOADING_MORE} khi tải trang kế, {@link LoadState#IDLE} khi không tải. Luôn có giá trị.
     */
    @NonNull
    public LiveData<LoadState> getLoadState() {
        return loadState;
    }

    /** Thông báo lỗi cho người dùng, mỗi lỗi lấy ra được một lần qua {@link Event#consume()}. */
    @NonNull
    public LiveData<Event<String>> getError() {
        return error;
    }

    /**
     * <p>Báo giao diện đưa danh sách về đầu, vì trang đầu vừa <b>thay thế</b> cả danh sách (mở màn hình, đổi bộ lọc). Trang
     * nối thêm (load more) không phát sự kiện này.</p>
     *
     * <p>Dùng một sự kiện riêng thay vì để giao diện suy ra từ {@link #getLoadState()}: <code>LiveData</code> hoãn giao
     * hàng khi giao diện đang ẩn và chỉ giao giá trị cuối, nên lúc giao thì <code>loadState</code> đã về
     * {@link LoadState#IDLE} và khoảnh khắc {@link LoadState#REFRESHING} đã mất. {@link Event} chưa bị lấy ra thì vẫn được
     * giao đúng một lần, và đã lấy rồi (ví dụ sau khi xoay màn hình) thì không cuộn thừa. Giao diện phải đăng ký observer
     * này <b>sau</b> observer của {@link #getBooks()}, để danh sách mới đã vào adapter trước khi cuộn.</p>
     */
    @NonNull
    public LiveData<Event<Boolean>> getScrollToTop() {
        return scrollToTop;
    }

    /**
     * State của khối lọc: {@link BookFilterModel#getCriteria()} để giao diện dựng lại (xoay màn hình) đổ lại vào các ô lọc,
     * và {@link BookFilterModel#getCategories()} để nạp spinner thể loại. Giao diện chỉ đọc; đổi bộ lọc phải qua
     * {@link #applyFilter} để danh sách được tải lại.
     */
    @NonNull
    public BookFilterModel getFilter() {
        return filter;
    }

    /**
     * Có trang kế tiếp để tải không, dùng cho load more. Không phải <code>LiveData</code>: giao diện chỉ đọc nó ngay lúc
     * {@link #getLoadState()} về {@link LoadState#IDLE}, khi giá trị đã ổn định.
     */
    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Lần tải gần nhất kết thúc bằng lỗi. Chỉ có nghĩa khi {@link #getLoadState()} là {@link LoadState#IDLE}; trong lúc
     * đang tải nó luôn là <code>false</code>.
     */
    public boolean lastLoadFailed() {
        return lastLoadFailed;
    }

    // Action

    /**
     * <p>Tải trang sách đầu và thể loại nếu chưa từng tải thành công. Gọi lại khi View được dựng lại thì không làm gì. Hai
     * việc có điều kiện riêng vì độc lập nhau: thể loại nạp lỗi thì lần gọi sau vẫn được thử lại dù sách đã có.</p>
     *
     * <p>Sách chỉ tải khi chưa có lần tải nào <b>đang chạy</b>: xoay màn hình giữa lúc đang tải trang đầu thì
     * <code>loaded</code> vẫn là <code>false</code>, và tải lại sẽ hủy việc đang chạy rồi bắt đầu từ đầu, vô ích. Lần
     * tải đầu đã lỗi thì {@link #getLoadState()} là {@link LoadState#IDLE}, nên View dựng lại sau đó vẫn thử lại được.</p>
     */
    public void ensureLoaded() {
        if (!loaded && !Objects.requireNonNull(loadState.getValue()).isLoading()) {
            reload();
        }
        if (!filter.hasCategories()) {
            loadCategories();
        }
    }

    /**
     * Đổi bộ lọc và tải lại từ trang đầu. Bộ lọc giống bộ lọc đang áp dụng (và sách đã tải một lần) thì bỏ qua, vì kết
     * quả sẽ y hệt.
     *
     * @param newCriteria bộ lọc người dùng vừa nhập
     */
    public void applyFilter(@NonNull BookCriteria newCriteria) {
        boolean changed = filter.apply(newCriteria);
        if (loaded && !changed) {
            return;
        }
        reload();
    }

    /**
     * Tải lại danh sách từ trang đầu theo bộ lọc hiện tại, dùng khi dữ liệu đã đổi từ nơi khác (ví dụ vừa thêm sách ở màn
     * form). Khác {@link #applyFilter} ở chỗ không so sánh bộ lọc: luôn tải lại.
     */
    public void refresh() {
        reload();
    }

    /** Tải trang kế tiếp và nối vào cuối danh sách. Bỏ qua khi hết trang hoặc đang có một lần tải (loại nào cũng vậy). */
    public void loadNextPage() {
        if (!hasNext || Objects.requireNonNull(loadState.getValue()).isLoading()) {
            return;
        }
        fetch();
    }

    /**
     * Thể loại đã đổi ở nơi khác (ví dụ vừa thêm ở màn form thể loại): nạp lại cho spinner của bộ lọc. Nhận số phiên bản
     * từ <code>CatalogViewModel</code>; phiên bản đã áp dụng rồi thì bỏ qua, nên gọi lại nhiều lần với cùng giá trị (lúc
     * đăng ký observer, xoay màn hình) không nạp thừa.
     *
     * @param version phiên bản thể loại hiện tại
     */
    public void onCategoriesVersion(int version) {
        if (version == appliedCategoriesVersion) {
            return;
        }
        appliedCategoriesVersion = version;
        loadCategories();
    }

    // Utilities

    /**
     * Quay về trang đầu rồi tải. Không xóa danh sách ở đây: xóa trước rồi nối sau sẽ phát hai thay đổi (rỗng, rồi trang
     * đầu) và để trống màn hình trong lúc chờ; trang đầu về sẽ thay thế danh sách cũ trong một lần.
     */
    private void reload() {
        nextPage = PageRequest.of(0, PAGE_SIZE);
        hasNext = true;
        fetch();
    }

    /**
     * <p>Tải <code>nextPage</code> theo bộ lọc của {@link #getFilter()}. Điều kiện và trang được chốt trước khi chạy nền để
     * việc nền không đọc state đang có thể đổi; state chỉ được sửa ở <code>onSuccess</code>, trên main thread.</p>
     *
     * <p>Hủy lần tải trước (nếu có) để kết quả của nó không ghi đè. Việc tính điều kiện nằm <b>trước</b> lúc hủy, để một
     * lỗi ở đó không bỏ lại lần tải cũ đã hủy mà chưa có lần mới thay, khiến {@link #getLoadState()} kẹt ở trạng thái đang
     * tải. {@link #getLoadState()} cho biết loại lần tải theo số trang: trang 0 là {@link LoadState#REFRESHING}, các trang
     * sau là {@link LoadState#LOADING_MORE}; về {@link LoadState#IDLE} ở <code>onComplete</code>, sau cả
     * <code>onSuccess</code> lẫn <code>onError</code>.</p>
     *
     * <p>Tải lỗi (trang nào cũng vậy) thì giữ nguyên danh sách đang hiện và chỉ báo lỗi. Hệ quả cần biết: trang đầu của
     * bộ lọc mới lỗi thì danh sách cũ (của bộ lọc trước) ở lại trong khi các ô lọc đã là bộ lọc mới, cho tới khi người dùng
     * lọc lại.</p>
     */
    private void fetch() {
        List<Criteria> sqlCriteria = filter.getCriteria().toSqlCriteria();
        PageRequest request = nextPage;
        if (currentJob != null) {
            currentJob.cancel();
        }
        lastLoadFailed = false;
        loadState.setValue(request.page() == 0 ? LoadState.REFRESHING : LoadState.LOADING_MORE);
        currentJob = run(
                () -> bookRepository.search(sqlCriteria, request),
                page -> {
                    List<Book> all = request.page() == 0
                            ? new ArrayList<>()
                            : new ArrayList<>(Objects.requireNonNull(books.getValue()));
                    all.addAll(page.content());
                    books.setValue(all);
                    if (request.page() == 0) {
                        scrollToTop.setValue(new Event<>(true));
                    }
                    hasNext = page.hasNext();
                    nextPage = request.next();
                    loaded = true;
                },
                e -> {
                    lastLoadFailed = true;
                    Log.e(TAG, "Failed to load books", e);
                    error.setValue(new Event<>(getApplication().getString(R.string.error_load)));
                })
                .onComplete(() -> loadState.setValue(LoadState.IDLE));
    }

    /**
     * <p>Nạp thể loại cho spinner của bộ lọc. Là một việc nền <b>riêng</b>, không gán vào <code>currentJob</code>, vì
     * {@link #fetch()} hủy <code>currentJob</code> mỗi khi đổi bộ lọc và không được hủy nhầm việc này. Lỗi chỉ ghi log,
     * không báo người dùng: bộ lọc vẫn dùng được với mục "Tất cả", và lần {@link #ensureLoaded()} sau sẽ thử lại vì
     * {@link BookFilterModel#hasCategories()} vẫn là <code>false</code>.</p>
     *
     * <p>Hủy lần nạp trước (nếu có): đây là việc chỉ đọc nên hủy được, và nhờ đó kết quả cũ không về sau rồi ghi đè kết quả
     * mới khi thể loại đổi nhanh liên tiếp.</p>
     */
    private void loadCategories() {
        if (categoriesJob != null) {
            categoriesJob.cancel();
        }
        categoriesJob = run(
                categoryRepository::findAll,
                filter::setCategories,
                e -> Log.e(TAG, "Failed to load categories", e));
    }
}
