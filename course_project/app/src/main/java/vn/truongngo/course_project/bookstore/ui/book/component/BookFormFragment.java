package vn.truongngo.course_project.bookstore.ui.book.component;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.jspecify.annotations.Nullable;

import java.util.Collections;

import vn.truongngo.course_project.R;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.bookstore.ui.book.model.BookForm;
import vn.truongngo.course_project.bookstore.ui.book.model.BookFormViewModel;
import vn.truongngo.course_project.libs.base.BaseFragment;
import vn.truongngo.course_project.libs.form.FormBinder;
import vn.truongngo.course_project.libs.util.Dialogs;
import vn.truongngo.course_project.libs.util.ViewBinder;

/**
 * Màn hình form sách, dùng chung cho thêm và sửa (có argument <code>ARG_ID</code> là sửa). Chỉ nối các ô nhập với
 * {@link BookFormViewModel} và vẽ state của nó; ghi xong thì báo màn danh sách qua <code>setFragmentResult</code>
 * (khóa <code>RESULT_KEY</code>) rồi đóng form.
 */
public class BookFormFragment extends BaseFragment<BookFormViews> {

    /** Khóa kết quả gửi cho màn danh sách khi ghi xong. */
    public static final String RESULT_KEY = "book_form_saved";

    /** Khóa argument chứa mã sách cần sửa; vắng mặt nghĩa là thêm mới. */
    public static final String ARG_ID = "book_id";
    /** Khóa trong kết quả chứa hành động vừa làm (một trong <code>ACTION_*</code> của ViewModel). */
    public static final String KEY_ACTION = "action";

    /** ViewModel của màn hình, gắn với Fragment nên sống qua xoay màn hình. Gán lại ở mỗi lần View dựng lại. */
    private BookFormViewModel viewModel;

    /**
     * Tạo form qua factory để argument đi theo <code>Bundle</code> (sống qua việc Fragment bị tạo lại).
     *
     * @param id mã sách cần sửa, hoặc <code>null</code> để thêm mới
     */
    public static BookFormFragment newInstance(@Nullable Long id) {
        BookFormFragment fragment = new BookFormFragment();
        if (id != null) {
            Bundle args = new Bundle();
            args.putLong(ARG_ID, id);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    protected int layoutResId() {
        return R.layout.fragment_book_form;
    }

    @NonNull
    @Override
    protected BookFormViews bindViews(@NonNull View root) {
        BookFormViews v = ViewBinder.create(BookFormViews.class, root);
        v.form = FormBinder.of(BookForm.class, root);
        return v;
    }

    @Override
    protected void initViews() {
        viewModel = viewModel(BookFormViewModel.class);
        views().form.choices("categoryId", Collections.emptyList(), Category::getId, Category::getName, "Chưa phân loại");
        views().form.populate(BookForm.empty());
        views().deleteButton.setVisibility(idArg() != null ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void initEvents() {
        views().saveButton.setOnClickListener(v -> {
            BookForm form = viewModel.formForSave();
            if (!views().form.readInto(form)) return;
            viewModel.save(form);
        });
        views().cancelButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        views().deleteButton.setOnClickListener(v -> Dialogs.confirm(
                requireContext(), lifetime(),
                getString(R.string.book_delete_title),
                getString(R.string.book_delete_message, viewModel.editingName()),
                getString(R.string.book_delete_confirm),
                viewModel::delete));
    }

    @Override
    protected void observe() {
        // Load list category cho spinner
        observe(viewModel.getCategories(), categories ->
                views().form.choices("categoryId", categories, Category::getId, Category::getName, "Chưa phân loại"));

        observe(viewModel.getFormToShow(), event -> {
            BookForm form = event.consume();
            if (form != null) views().form.populate(form);
        });

        // Hai nguồn cùng quyết định bật/tắt nút nên dùng chung một hàm, để observer này không ghi đè observer kia.
        observe(viewModel.getProcessing(), saving -> updateButtons());
        observe(viewModel.getFormLoadFailed(), failed -> updateButtons());

        // Lưu thành công, gửi kết quả cho list
        observe(viewModel.getDone(), event -> {
            String action = event.consume();
            if (action == null) return;
            Bundle result = new Bundle();
            result.putString(KEY_ACTION, action);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            getParentFragmentManager().popBackStack();
        });

        // Show lỗi
        observe(viewModel.getError(), event -> {
            String message = event.consume();
            if (message != null) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void loadData() {
        viewModel.ensureLoaded(idArg());
    }

    /**
     * Nút Lưu và Xóa khóa khi đang ghi hoặc khi form sửa không nạp được (lúc đó form trống, Lưu sẽ tạo sách mới và hộp xác
     * nhận xóa không có tên); nút Hủy chỉ khóa khi đang ghi, để người dùng vẫn thoát ra được.
     */
    private void updateButtons() {
        boolean saving = Boolean.TRUE.equals(viewModel.getProcessing().getValue());
        boolean loadFailed = Boolean.TRUE.equals(viewModel.getFormLoadFailed().getValue());
        views().saveButton.setEnabled(!saving && !loadFailed);
        views().deleteButton.setEnabled(!saving && !loadFailed);
        views().cancelButton.setEnabled(!saving);
    }

    /** Mã sách trong argument, hoặc <code>null</code> khi thêm mới. */
    @Nullable
    private Long idArg() {
        Bundle args = getArguments();
        return args != null && args.containsKey(ARG_ID) ? args.getLong(ARG_ID) : null;
    }
}
