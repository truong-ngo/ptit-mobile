package vn.truongngo.course_project;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import java.util.Objects;

import vn.truongngo.course_project.bookstore.ui.book.component.BookListFragment;
import vn.truongngo.course_project.bookstore.ui.category.component.CategoryListFragment;
import vn.truongngo.course_project.libs.base.BaseActivity;
import vn.truongngo.course_project.libs.util.ViewBinder;

/**
 * <p>Activity duy nhất của ứng dụng: chứa hai tab ({@link BookListFragment}, {@link CategoryListFragment}) và thanh
 * chọn tab. Các màn con như form sách, form thể loại do chính hai tab mở chồng lên bằng back stack, nên Activity chỉ
 * lo hai việc: chuyển tab và ẩn thanh tab khi đang ở màn con.</p>
 *
 * <p>Hai tab được <b>thêm một lần</b> rồi chỉ ẩn/hiện, không thay thế nhau, để bộ lọc, các trang đã tải và vị trí cuộn của
 * mỗi tab được giữ khi chuyển qua lại.</p>
 */
public class MainActivity extends BaseActivity<MainViews> {

    /** Tag của tab Sách trong <code>FragmentManager</code>, để tìm lại sau khi Activity được tạo lại. */
    private static final String TAG_BOOKS = BookListFragment.class.getSimpleName();

    /** Tag của tab Thể loại trong <code>FragmentManager</code>, để tìm lại sau khi Activity được tạo lại. */
    private static final String TAG_CATEGORIES = CategoryListFragment.class.getSimpleName();

    @Override
    protected int layoutResId() {
        return R.layout.activity_main;
    }

    @NonNull
    @Override
    protected MainViews bindViews(@NonNull View root) {
        return ViewBinder.create(MainViews.class, root);
    }

    /**
     * Thêm hai tab nếu chưa có (tab Thể loại ẩn sẵn) và đăng ký việc ẩn/hiện thanh tab theo back stack. Chỉ thêm khi
     * chưa tìm thấy tab Sách theo tag: khi Activity được tạo lại, <code>FragmentManager</code> đã tự khôi phục các tab, thêm
     * nữa sẽ tạo bản trùng.
     */
    @Override
    protected void initViews() {
        FragmentManager fm = getSupportFragmentManager();

        if (fm.findFragmentByTag(TAG_BOOKS) == null) {
            Fragment categories = new CategoryListFragment();
            fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.container, new BookListFragment(), TAG_BOOKS)
                    .add(R.id.container, categories, TAG_CATEGORIES)
                    .hide(categories)
                    .setMaxLifecycle(categories, Lifecycle.State.STARTED)
                    .commit();
        }

        // Ẩn thanh tab khi vào form của sách hoặc thể loại, vì form được thay vào container và nằm trong back stack.
        fm.addOnBackStackChangedListener(this::updateBottomNavVisibility);
        updateBottomNavVisibility();
    }

    @Override
    protected void initEvents() {
        views.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId() == R.id.nav_categories ? TAG_CATEGORIES : TAG_BOOKS);
            return true;
        });
    }

    /**
     * Hiện tab có tag này và ẩn tab kia; hai Fragment luôn tồn tại nên bộ lọc, trang đã tải và vị trí cuộn được giữ. Tab ẩn
     * bị hạ xuống <code>STARTED</code> và tab hiện được nâng lên <code>RESUMED</code> để chỉ tab đang thấy mới ở trạng thái
     * hoạt động. Không làm gì nếu một trong hai tab chưa có.
     *
     * @param tag {@link #TAG_BOOKS} hoặc {@link #TAG_CATEGORIES}
     */
    private void showTab(String tag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment books = fm.findFragmentByTag(TAG_BOOKS);
        Fragment categories = fm.findFragmentByTag(TAG_CATEGORIES);
        if (books == null || categories == null) {
            return;
        }
        Fragment shown = TAG_BOOKS.equals(tag) ? books : categories;
        Fragment hidden = shown == books ? categories : books;

        fm.beginTransaction()
                .setReorderingAllowed(true)
                .hide(Objects.requireNonNull(hidden))
                .setMaxLifecycle(hidden, Lifecycle.State.STARTED)
                .show(Objects.requireNonNull(shown))
                .setMaxLifecycle(shown, Lifecycle.State.RESUMED)
                .commit();
    }

    /** Thanh tab chỉ hiện khi không có màn con nào trong back stack. */
    private void updateBottomNavVisibility() {
        boolean atRoot = getSupportFragmentManager().getBackStackEntryCount() == 0;
        views.bottomNav.setVisibility(atRoot ? View.VISIBLE : View.GONE);
    }
}
