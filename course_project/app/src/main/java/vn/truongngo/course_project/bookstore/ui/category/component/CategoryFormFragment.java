package vn.truongngo.course_project.bookstore.ui.category.component;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.jspecify.annotations.Nullable;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.ui.category.model.CategoryForm;
import vn.truongngo.course_project.bookstore.ui.category.model.CategoryFormViewModel;
import vn.truongngo.course_project.bookstore.ui.shared.CatalogViewModel;
import vn.truongngo.course_project.libs.base.BaseFragment;
import vn.truongngo.course_project.libs.form.FormBinder;
import vn.truongngo.course_project.libs.util.Dialogs;
import vn.truongngo.course_project.libs.util.ViewBinder;

/**
 * Màn hình form thể loại, dùng chung cho thêm và sửa (có tham số <code>ARG_ID</code> là sửa). Chỉ nối các ô nhập với
 * {@link CategoryFormViewModel} và vẽ state của nó; ghi xong thì báo màn danh sách qua <code>setFragmentResult</code>
 * (khóa <code>RESULT_KEY</code>) rồi đóng form.
 */
public class CategoryFormFragment extends BaseFragment<CategoryFormViews> {

    /** Khóa kết quả gửi cho màn danh sách khi ghi xong. */
    public static final String RESULT_KEY = "category_form_saved";
    /** Khóa argument chứa mã thể loại cần sửa; vắng mặt nghĩa là thêm mới. */
    public static final String ARG_ID = "category_id";
    /** Khóa trong kết quả chứa hành động vừa làm (một trong ACTION_* của ViewModel). */
    public static final String KEY_ACTION = "action";

    /** ViewModel của màn hình, gắn với Fragment nên sống qua xoay màn hình. Gán lại ở mỗi lần View dựng lại. */
    private CategoryFormViewModel viewModel;

    /**
     * Tạo form qua factory để argument đi theo Bundle (sống qua việc Fragment bị tạo lại).
     *
     * @param id mã thể loại cần sửa, hoặc 
ull để thêm mới
     */
    public static CategoryFormFragment newInstance(@Nullable Long id) {
        CategoryFormFragment fragment = new CategoryFormFragment();
        if (id != null) {
            Bundle args = new Bundle();
            args.putLong(ARG_ID, id);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    protected int layoutResId() {
        return R.layout.fragment_category_form;
    }

    @NonNull
    @Override
    protected CategoryFormViews bindViews(@NonNull View root) {
        CategoryFormViews v = ViewBinder.create(CategoryFormViews.class, root);
        v.form = FormBinder.of(CategoryForm.class, root);
        return v;
    }

    @Override
    protected void initViews() {
        viewModel = viewModel(CategoryFormViewModel.class);
        views().form.populate(CategoryForm.empty());
        // Nút xóa chỉ có khi sửa một thể loại có sẵn.
        views().deleteButton.setVisibility(idArg() != null ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initEvents() {
        views().saveButton.setOnClickListener(v -> {
            // Đọc vào form của ViewModel (có id khi sửa), không dùng read() vì nó tạo form mới và làm mất id.
            CategoryForm form = viewModel.formForSave();
            if (!views().form.readInto(form)) return;      // lỗi validate
            viewModel.save(form);
        });
        views().cancelButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        views().deleteButton.setOnClickListener(v -> Dialogs.confirm(
                requireContext(), lifetime(),
                getString(R.string.category_delete_title),
                getString(R.string.category_delete_message, viewModel.editingName()),
                getString(R.string.category_delete_confirm),
                viewModel::delete));
    }

    @Override
    protected void observe() {
        // Đổ thể dữ liệu sửa (với case sửa)
        observe(viewModel.getFormToShow(), event -> {
            CategoryForm form = event.consume();
            if (form != null) views().form.populate(form);
        });

        // Hai nguồn cùng quyết định bật/tắt nút nên dùng chung một hàm, để observer này không ghi đè observer kia.
        observe(viewModel.getProcessing(), saving -> updateButtons());
        observe(viewModel.getFormLoadFailed(), failed -> updateButtons());

        // Ghi xong: báo các tab khác (bộ lọc của tab Sách giữ danh sách thể loại cũ), rồi close form.
        observe(viewModel.getDone(), event -> {
            String action = event.consume();
            if (action == null) return;
            activityViewModel(CatalogViewModel.class).notifyCategoriesChanged();
            Bundle result = new Bundle();
            result.putString(KEY_ACTION, action);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            getParentFragmentManager().popBackStack();
        });

        observe(viewModel.getError(), event -> {
            String message = event.consume();
            if (message != null) Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
        });
    }

    @Override
    protected void loadData() {
        viewModel.ensureLoaded(idArg());
    }

    /**
     * Nút Lưu và Xóa khóa khi đang ghi hoặc khi form sửa không nạp được (lúc đó form trống, Lưu sẽ tạo thể loại mới và hộp
     * xác nhận xóa không có tên); nút Hủy chỉ khóa khi đang ghi, để người dùng vẫn thoát ra được.
     */
    private void updateButtons() {
        boolean saving = Boolean.TRUE.equals(viewModel.getProcessing().getValue());
        boolean loadFailed = Boolean.TRUE.equals(viewModel.getFormLoadFailed().getValue());
        views().saveButton.setEnabled(!saving && !loadFailed);
        views().deleteButton.setEnabled(!saving && !loadFailed);
        views().cancelButton.setEnabled(!saving);
    }

    @Nullable
    private Long idArg() {
        Bundle args = getArguments();
        return args != null && args.containsKey(ARG_ID) ? args.getLong(ARG_ID) : null;
    }
}
