package vn.truongngo.course_project.bookstore.ui.category.component;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.bookstore.ui.book.model.LoadState;
import vn.truongngo.course_project.bookstore.ui.category.model.CategoryFormViewModel;
import vn.truongngo.course_project.bookstore.ui.category.model.CategoryListViewModel;
import vn.truongngo.course_project.libs.adapter.GenericRecyclerAdapter;
import vn.truongngo.course_project.libs.base.BaseFragment;
import vn.truongngo.course_project.libs.util.ViewBinder;
import vn.truongngo.course_project.libs.util.ViewUtils;

/**
 * Màn hình danh sách thể loại (tab Thể loại). Chỉ vẽ state của {@link CategoryListViewModel} và chuyển thao tác của
 * người dùng cho nó; bấm dòng hoặc nút nổi thì mở {@link CategoryFormFragment} (sửa hoặc thêm), và khi form ghi xong thì
 * nhận kết quả để nạp lại danh sách và báo người dùng.
 */
public class CategoryListFragment extends BaseFragment<CategoryListViews> {

    /** ViewModel của màn hình, gắn với Fragment nên sống qua xoay màn hình. Gán lại ở mỗi lần View dựng lại. */
    private CategoryListViewModel viewModel;

    /** Adapter của danh sách; tạo lại cùng View vì nó giữ tham chiếu tới các dòng của <code>RecyclerView</code> cũ. */
    private GenericRecyclerAdapter<Category, CategoryItemViews> adapter;

    @Override
    protected int layoutResId() {
        return R.layout.fragment_category_list;
    }

    @NonNull
    @Override
    protected CategoryListViews bindViews(@NonNull View root) {
        return ViewBinder.create(CategoryListViews.class, root);
    }

    @Override
    protected void initViews() {
        viewModel = viewModel(CategoryListViewModel.class);
        adapter = GenericRecyclerAdapter.of(R.layout.item_category, CategoryItemViews.class, (CategoryItemViews row, Category item) -> row.name.setText(item.getName()));
        adapter.setIdProvider(Category::getId);
        views().listCategories.setAdapter(adapter);
    }

    @Override
    protected void initEvents() {
        views().addCategoryFloatingButton.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.container, CategoryFormFragment.newInstance(null))
                .addToBackStack(null)
                .commit());

        // Bấm một dòng thì mở form ở chế độ sửa. BiConsumer của adapter nhận (item, position), item trước.
        adapter.setOnItemClick((item, position) -> getParentFragmentManager().beginTransaction()
                .replace(R.id.container, CategoryFormFragment.newInstance(item.getId()))
                .addToBackStack(null)
                .commit());

        // Form đã ghi xong: làm mới danh sách và báo đúng hành động (thêm, sửa hay xóa).
        getParentFragmentManager().setFragmentResultListener(
                CategoryFormFragment.RESULT_KEY, getViewLifecycleOwner(), (key, bundle) -> {
                    viewModel.refresh();
                    String action = bundle.getString(CategoryFormFragment.KEY_ACTION, "");
                    int text;
                    if (CategoryFormViewModel.ACTION_UPDATED.equals(action)) {
                        text = R.string.category_updated;
                    } else if (CategoryFormViewModel.ACTION_DELETED.equals(action)) {
                        text = R.string.category_deleted;
                    } else {
                        text = R.string.category_saved;
                    }
                    Snackbar.make(requireView(), text, Snackbar.LENGTH_SHORT)
                            .setAnchorView(views().addCategoryFloatingButton)
                            .show();
                });
    }

    @Override
    protected void observe() {
        observe(
                viewModel.getCategories(),
                categories -> {
                    adapter.submit(categories);
                    views().header.setText(getString(R.string.category_header_count, categories.size()));
                    updateLoadingState();
                });
        observe(viewModel.getLoadState(), state -> updateLoadingState());

        observe(viewModel.getError(), event -> {
            String message = event.consume();
            if (message != null) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
                        .setAnchorView(views().addCategoryFloatingButton)
                        .show();
            }
        });
    }

    @Override
    protected void loadData() {
        viewModel.ensureLoaded();
    }

    /**
     * Cập nhật vòng xoay và dòng "chưa có thể loại" từ {@link LoadState}. Vòng xoay hiện khi
     * {@link LoadState#REFRESHING}; dòng thông báo chỉ hiện khi {@link LoadState#IDLE} và chưa có thể loại, vì danh sách
     * rỗng lúc đang nạp chưa có nghĩa là không có dữ liệu. Gọi lại mỗi khi <code>loadState</code> hoặc danh sách đổi.
     */
    private void updateLoadingState() {
        LoadState state = viewModel.getLoadState().getValue();
        List<Category> categories = viewModel.getCategories().getValue();
        boolean hasItems = categories != null && !categories.isEmpty();
        boolean showMessage = state == LoadState.IDLE && !hasItems;

        ViewUtils.visibleIf(views().progress, state == LoadState.REFRESHING);
        ViewUtils.visibleIf(views().emptyListCategory, showMessage);
    }
}
