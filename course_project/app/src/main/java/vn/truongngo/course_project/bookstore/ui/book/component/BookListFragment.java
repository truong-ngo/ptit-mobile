package vn.truongngo.course_project.bookstore.ui.book.component;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;
import java.util.Optional;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.bookstore.domain.BookCondition;
import vn.truongngo.course_project.bookstore.ui.book.model.BookFormViewModel;
import vn.truongngo.course_project.bookstore.ui.book.model.BookListViewModel;
import vn.truongngo.course_project.bookstore.ui.shared.CatalogViewModel;
import vn.truongngo.course_project.libs.adapter.GenericRecyclerAdapter;
import vn.truongngo.course_project.libs.adapter.LoadMoreListener;
import vn.truongngo.course_project.libs.base.BaseFragment;
import vn.truongngo.course_project.bookstore.ui.book.model.LoadState;
import vn.truongngo.course_project.libs.util.Money;
import vn.truongngo.course_project.libs.util.StringUtils;
import vn.truongngo.course_project.libs.util.ViewBinder;
import vn.truongngo.course_project.libs.util.ViewUtils;

/**
 * Màn hình danh sách sách (tab Sách): khối lọc, danh sách phân trang với load more, hai vòng xoay theo {@link LoadState}.
 * Chỉ vẽ state của {@link BookListViewModel} và chuyển thao tác cho nó; bấm dòng hoặc nút nổi thì mở
 * {@link BookFormFragment}, và khi form ghi xong thì nhận kết quả để nạp lại danh sách và báo người dùng.
 */
public class BookListFragment extends BaseFragment<BookListViews> {

    /** ViewModel của màn hình, gắn với Fragment nên sống qua xoay màn hình. Gán lại ở mỗi lần View dựng lại. */
    private BookListViewModel viewModel;

    /** Adapter của danh sách; tạo lại cùng View vì nó giữ tham chiếu tới các dòng của <code>RecyclerView</code> cũ. */
    private GenericRecyclerAdapter<Book, BookItemViews> adapter;

    /** Gắn ở {@link #initEvents()}, gán lại mỗi lần View dựng lại. Giữ ở Fragment, không ở ViewModel, vì nó giữ <code>RecyclerView</code>. */
    private LoadMoreListener loadMore;

    @Override
    protected int layoutResId() {
        return R.layout.fragment_book_list;
    }

    @NonNull
    @Override
    protected BookListViews bindViews(@NonNull View root) {
        return ViewBinder.create(BookListViews.class, root);
    }

    @Override
    protected void initViews() {
        viewModel = viewModel(BookListViewModel.class);
        adapter = GenericRecyclerAdapter.of(R.layout.item_book, BookItemViews.class, (BookItemViews row, Book item) -> {
            row.title.setText(StringUtils.defaultIfBlank(item.getTitle(), ""));
            row.price.setText(Money.formatVnd(item.getPrice()));
            row.author.setText(StringUtils.defaultIfBlank(item.getAuthor(), "N/A"));
            row.condition.setText(Optional.ofNullable(item.getCondition()).map(BookCondition::getLabel).orElse("N/A"));
        });
        adapter.setIdProvider(Book::getId);
        views().listBooks.setAdapter(adapter);
        views().bookFilter.render(viewModel.getFilter().getCriteria());
    }

    @Override
    protected void observe() {
        observe(viewModel.getBooks(), books -> {
            adapter.submit(books);
            updateLoadingState();
        });
        // Register sau observer của books: apply list mới vào adapter rồi mới scroll về đầu.
        observe(viewModel.getScrollToTop(), event -> {
            if (event.consume() != null) {
                views().listBooks.scrollToPosition(0);
            }
        });

        observe(viewModel.getLoadState(), state -> {
            updateLoadingState();
            if (state == LoadState.IDLE) {
                if (viewModel.lastLoadFailed()) {
                    loadMore.failed();
                } else {
                    loadMore.loaded(viewModel.hasNext());
                }
            }
        });

        observe(viewModel.getFilter().getCategories(), categories ->
                views().bookFilter.setCategories(categories, viewModel.getFilter().getCriteria().categoryId));

        // Thể loại đổi ở màn khác (thêm ở form thể loại): nạp lại cho spinner của bộ lọc.
        observe(activityViewModel(CatalogViewModel.class).categoriesVersion(), viewModel::onCategoriesVersion);

        observe(viewModel.getError(), event -> {
            String message = event.consume();
            if (message != null) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                        .setAnchorView(views().addBookFloatingButton)
                        .show();
            }
        });
    }

    @Override
    protected void initEvents() {
        views().bookFilter.setOnApply(viewModel::applyFilter);

        // gán load more handler
        loadMore = LoadMoreListener.attach(views().listBooks, lifetime(), viewModel::loadNextPage);

        // Mở tab thêm layout thêm book khi bấm +
        views().addBookFloatingButton.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.container, new BookFormFragment())
                .addToBackStack(null)
                .commit());

        adapter.setOnItemClick((item, position) -> getParentFragmentManager().beginTransaction()
                .replace(R.id.container, BookFormFragment.newInstance(item.getId()))
                .addToBackStack(null)
                .commit());

        getParentFragmentManager().setFragmentResultListener(
                BookFormFragment.RESULT_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                    viewModel.refresh();
                    String action = bundle.getString(BookFormFragment.KEY_ACTION, "");
                    int text;
                    if (BookFormViewModel.ACTION_UPDATED.equals(action)) {
                        text = R.string.book_updated;
                    } else if (BookFormViewModel.ACTION_DELETED.equals(action)) {
                        text = R.string.book_deleted;
                    } else {
                        text = R.string.book_saved;
                    }
                    Snackbar.make(requireView(), text, Snackbar.LENGTH_SHORT)
                            .setAnchorView(views().addBookFloatingButton)
                            .show();
                });
    }

    @Override
    protected void loadData() {
        viewModel.ensureLoaded();
    }

    /**
     * <p>Cập nhật hai vòng xoay và dòng thông báo giữa màn hình từ {@link LoadState}. Vòng xoay chọn theo <b>loại</b> lần
     * tải chứ không đoán từ nội dung danh sách: {@link LoadState#REFRESHING} (tải trang đầu, kể cả khi đổi bộ lọc mà danh
     * sách cũ còn sách) hiện vòng ở giữa, {@link LoadState#LOADING_MORE} hiện vòng ở cuối danh sách.</p>
     *
     * <p>Dòng thông báo chỉ hiện khi {@link LoadState#IDLE} và chưa có sách, vì danh sách rỗng lúc đang tải chưa có nghĩa
     * là không có dữ liệu. Nội dung phân biệt <b>rỗng thật</b> với <b>rỗng do lỗi</b> bằng
     * {@link BookListViewModel#lastLoadFailed()}: đọc ở đây là đúng thời điểm, vì <code>onError</code> đặt cờ trước rồi
     * <code>onComplete</code> mới đưa state về <code>IDLE</code> và gọi lại hàm này. Lỗi khi danh sách đã có sách (ví dụ
     * load more) không hiện dòng này, vì lúc đó không còn rỗng.</p>
     *
     * <p>Gọi lại mỗi khi <code>loadState</code> hoặc <code>books</code> đổi.</p>
     */
    private void updateLoadingState() {
        LoadState state = viewModel.getLoadState().getValue();
        List<Book> books = viewModel.getBooks().getValue();
        boolean hasItems = books != null && !books.isEmpty();
        boolean showMessage = state == LoadState.IDLE && !hasItems;

        ViewUtils.visibleIf(views().progressCenter, state == LoadState.REFRESHING);
        ViewUtils.visibleIf(views().progressFooter, state == LoadState.LOADING_MORE);
        ViewUtils.visibleIf(views().emptyListBook, showMessage);
        views().emptyListBook.setText(showMessage && viewModel.lastLoadFailed() ? R.string.book_load_error : R.string.book_empty);
    }
}
