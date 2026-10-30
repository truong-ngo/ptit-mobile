package vn.truongngo.course_project.bookstore.ui.shared;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * <p>ViewModel dùng chung của các tab, lấy bằng <code>BaseFragment#activityViewModel</code>: nơi một màn hình báo cho các
 * màn hình khác rằng dữ liệu danh mục đã đổi (ví dụ thêm thể loại ở màn form, trong khi bộ lọc của tab Sách đang giữ danh
 * sách thể loại cũ).</p>
 *
 * <p>Tín hiệu là một <b>số phiên bản</b> tăng dần chứ không phải {@link vn.truongngo.course_project.libs.base.Event}:
 * <code>Event</code> chỉ lấy ra được một lần nên với nhiều nơi nhận thì nơi nhận trước sẽ "ăn mất" tín hiệu của nơi sau.
 * Mỗi nơi nhận tự nhớ phiên bản đã áp dụng và nạp lại khi thấy một phiên bản khác, nên đúng cả khi tab đang ẩn lúc dữ liệu
 * đổi (nhận khi hiện lại) và khi xoay màn hình.</p>
 *
 * <p>Chỉ mang tín hiệu, không giữ dữ liệu danh mục: mỗi màn hình vẫn tự nạp từ repository. Chỉ gọi ở main thread, như
 * <code>LiveData.setValue</code>.</p>
 */
public class CatalogViewModel extends ViewModel {

    private final MutableLiveData<Integer> categoriesVersion = new MutableLiveData<>(0);

    /** Phiên bản danh sách thể loại, bắt đầu từ 0 và tăng một mỗi lần {@link #notifyCategoriesChanged()}. */
    @NonNull
    public LiveData<Integer> categoriesVersion() {
        return categoriesVersion;
    }

    /** Báo thể loại đã được thêm, sửa hoặc xóa, để các màn hình đang giữ danh sách thể loại nạp lại. */
    public void notifyCategoriesChanged() {
        categoriesVersion.setValue(Objects.requireNonNull(categoriesVersion.getValue()) + 1);
    }
}
