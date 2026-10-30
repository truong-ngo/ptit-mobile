package vn.truongngo.course_project.libs.base;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import vn.truongngo.course_project.libs.async.Async;
import vn.truongngo.course_project.libs.async.Job;

/**
 * <p>Khuôn chung cho ViewModel: giữ state và chạy việc nền của một màn hình, tách khỏi giao diện.</p>
 *
 * <p>ViewModel sống lâu hơn View (qua xoay màn hình, và khi Fragment nằm trong back stack), nên việc nền của nó gắn
 * với {@link #lifetime()} riêng, kết thúc ở <code>onCleared</code>, chứ không gắn với <code>Lifetime</code> của View.
 * Nhờ vậy kết quả về lúc View đang vắng mặt vẫn vào được state, và View dựng lại sẽ nhận state mới nhất.</p>
 *
 * <p>Quy ước giữa ViewModel và giao diện: ViewModel không giữ View, Fragment, container hay <code>Context</code> của
 * Activity, và chỉ để lộ state qua <code>LiveData</code> chỉ đọc. Giao diện chỉ gọi hàm công khai của ViewModel rồi
 * vẽ state nhận được (xem {@link BaseFragment#observe()}).</p>
 *
 * <p>Kế thừa <code>AndroidViewModel</code> để repository lấy được <code>Application</code> (sống bằng tiến trình, không
 * rò rỉ như Activity). <code>ViewModelProvider</code> mặc định đã biết truyền <code>Application</code> cho constructor,
 * nên lớp con chỉ cần khai báo constructor một tham số <code>Application</code>.</p>
 */
public abstract class BaseViewModel extends AndroidViewModel {

    /** Kết thúc ở {@link #onCleared()}; manual vì ViewModel không có Lifecycle riêng để bám theo. */
    private final Lifetime lifetime = Lifetime.manual();

    /**
     * Lớp con phải khai báo constructor <code>public</code> một tham số <code>Application</code> và gọi lên đây, vì
     * <code>ViewModelProvider</code> tạo ViewModel bằng reflection theo đúng chữ ký đó.
     */
    protected BaseViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Khoảng đời của dữ liệu: kết thúc khi ViewModel bị hủy hẳn, không phải khi View bị hủy. Như mọi {@link Lifetime},
     * kết thúc chỉ làm callback bị bỏ, việc nền đang chạy vẫn chạy xong (để lệnh ghi không mất giữa chừng).
     */
    @NonNull
    protected final Lifetime lifetime() {
        return lifetime;
    }

    /**
     * Chạy <code>task</code> ở thread nền gắn với {@link #lifetime()}, lỗi không có <code>onError</code> bị ném lại ở
     * main thread (xem {@link Async}). <code>task</code> chỉ nên làm phần thuần (đọc DB, tính toán) và <b>không</b>
     * sửa state, vì nó chạy ngoài main thread; việc đổi state làm trong <code>onSuccess</code>.
     */
    @NonNull
    protected final <T> Job<T> run(@NonNull Callable<T> task, @NonNull Consumer<T> onSuccess) {
        return Async.run(lifetime, task, onSuccess);
    }

    /** Như {@link #run(Callable, Consumer)}, thêm <code>onError</code> chạy ở main thread khi <code>task</code> ném lỗi. */
    @NonNull
    protected final <T> Job<T> run(@NonNull Callable<T> task,
                                   @NonNull Consumer<T> onSuccess,
                                   @NonNull Consumer<Throwable> onError) {
        return Async.run(lifetime, task, onSuccess, onError);
    }

    /** Kết thúc {@link #lifetime()}. Lớp con override phải gọi <code>super.onCleared()</code>. */
    @Override
    protected void onCleared() {
        lifetime.end();
    }
}
