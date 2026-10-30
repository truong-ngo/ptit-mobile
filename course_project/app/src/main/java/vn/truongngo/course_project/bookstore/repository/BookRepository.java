package vn.truongngo.course_project.bookstore.repository;

import org.jspecify.annotations.NonNull;

import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.libs.repository.BaseDbHelper;
import vn.truongngo.course_project.libs.repository.BaseSqliteRepository;

public class BookRepository extends BaseSqliteRepository<Book, Long> {

    /**
     * Quét siêu dữ liệu của <code>entityClass</code> (lần đầu mới quét, sau đó dùng cache) và kiểm tra bảng của nó có
     * trong <code>helper</code>. Chưa mở database, việc đó chỉ xảy ra ở lệnh đầu tiên.
     *
     * @param helper
     * @param entityClass
     * @throws IllegalArgumentException nếu <code>entityClass</code> chưa được đăng ký trong <code>helper</code> (bảng sẽ không tồn tại)
     * @throws IllegalStateException    nếu <code>entityClass</code> không hợp lệ làm thực thể (thiếu <code>@Id</code>, kiểu field chưa hỗ trợ...)
     */
    public BookRepository(@NonNull BaseDbHelper helper, @NonNull Class<Book> entityClass) {
        super(helper, entityClass);
    }
}
