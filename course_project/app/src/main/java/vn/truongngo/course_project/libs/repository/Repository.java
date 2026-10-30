package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

import vn.truongngo.course_project.libs.repository.specification.Criteria;

/**
 * <p>Giao diện chuẩn cho tầng truy cập dữ liệu: CRUD, tìm kiếm động theo danh sách {@link Criteria} và phân trang.</p>
 *
 * <p>Các hàm chạy <b>đồng bộ</b> và chạm vào ổ đĩa, nên phải gọi ngoài main thread (thường qua <code>Async.run</code>);
 * cài đặt có thể ném lỗi nếu gọi ở main thread.</p>
 *
 * @param <E>  kiểu thực thể
 * @param <ID> kiểu khóa chính
 */
public interface Repository<E, ID> {

    /**
     * Thêm mới một thực thể. Nếu khóa chính tự tăng thì giá trị sinh ra được gán ngược vào <code>entity</code>.
     *
     * @return chính <code>entity</code>
     */
    @NonNull
    E insert(@NonNull E entity);

    /**
     * Cập nhật mọi cột trừ khóa chính của thực thể có cùng khóa chính. Field null được ghi thành <code>NULL</code>.
     *
     * @throws IllegalStateException nếu không có bản ghi nào mang khóa chính đó
     */
    void update(@NonNull E entity);

    /** Bản ghi mang khóa chính này, hoặc rỗng nếu không có. */
    @NonNull
    Optional<E> findById(@NonNull ID id);

    /** Có bản ghi mang khóa chính này không. Rẻ hơn {@link #findById} vì không đọc cả dòng. */
    boolean existsById(@NonNull ID id);

    /**
     * Xóa bản ghi theo khóa chính.
     *
     * @return <code>true</code> nếu có bản ghi bị xóa
     * @throws android.database.sqlite.SQLiteConstraintException nếu còn bản ghi con tham chiếu tới nó và khóa ngoại là
     *                                                          <code>RESTRICT</code>
     */
    boolean deleteById(@NonNull ID id);

    /** Toàn bộ bản ghi, sắp xếp khóa chính giảm dần. */
    @NonNull
    List<E> findAll();

    /** Một trang của toàn bộ bản ghi. */
    @NonNull
    Page<E> findAll(@NonNull PageRequest request);

    /**
     * Các bản ghi thỏa mọi điều kiện (nối bằng <code>AND</code>), sắp xếp khóa chính giảm dần.
     *
     * @param criteria rỗng hoặc <code>null</code> nghĩa là không lọc
     */
    @NonNull
    List<E> search(List<Criteria> criteria);

    /** Một trang của các bản ghi thỏa điều kiện; <code>criteria</code> rỗng hoặc <code>null</code> nghĩa là không lọc. */
    @NonNull
    Page<E> search(List<Criteria> criteria, @NonNull PageRequest request);

    /** Số bản ghi thỏa điều kiện; <code>criteria</code> rỗng hoặc <code>null</code> thì đếm cả bảng. */
    long count(List<Criteria> criteria);

    /**
     * Lối thoát khi {@link Criteria} không đủ (ví dụ cần <code>OR</code>): mệnh đề <code>WHERE</code> thô với tham số
     * <code>?</code>. Dùng tên <b>cột</b> SQL, không phải tên field Java. Sắp xếp khóa chính giảm dần.
     *
     * <pre>{@code repo.query("price < ? OR condition = ?", "50000", "TOT")}</pre>
     */
    @NonNull
    List<E> query(@NonNull String where, String... args);
}
