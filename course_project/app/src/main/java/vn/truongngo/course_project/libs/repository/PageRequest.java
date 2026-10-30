package vn.truongngo.course_project.libs.repository;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import vn.truongngo.course_project.libs.util.Assert;

/**
 * <p>Yêu cầu một trang kết quả: số trang (bắt đầu từ 0), kích thước trang và cột sắp xếp tùy chọn. Bất biến, mỗi hàm
 * thay đổi trả về đối tượng mới.</p>
 *
 * <p>Sắp xếp theo tên <b>field Java</b>, không phải tên cột. Khóa chính luôn là tiêu chí phụ để thứ tự ổn định giữa các
 * trang; không chỉ định sắp xếp thì mặc định khóa chính giảm dần.</p>
 */
public final class PageRequest {

    /** Chiều sắp xếp. */
    public enum Direction {

        /** Tăng dần. */
        ASC,

        /** Giảm dần. */
        DESC
    }

    /** Số trang, bắt đầu từ 0. */
    private final int page;

    /** Số dòng mỗi trang, tối thiểu 1. */
    private final int size;

    /** Null nghĩa là không chỉ định sắp xếp, repository dùng thứ tự mặc định. */
    private final String sortField;

    /** Chiều sắp xếp của {@link #sortField}; không có nghĩa khi <code>sortField</code> là null. */
    private final Direction direction;

    /** Kiểm tra tham số rồi gán. Chỉ tạo qua {@link #of}, {@link #orderBy} và {@link #next}. */
    private PageRequest(int page, int size, @Nullable String sortField, @NonNull Direction direction) {
        Assert.isTrue(page >= 0, "page must be >= 0");
        Assert.isTrue(size >= 1, "size must be >= 1");
        this.page = page;
        this.size = size;
        this.sortField = sortField;
        this.direction = direction;
    }

    /**
     * @param page trang cần lấy, bắt đầu từ 0
     * @param size số dòng mỗi trang, tối thiểu 1
     * @throws IllegalArgumentException nếu <code>page</code> âm hoặc <code>size</code> nhỏ hơn 1
     */
    @NonNull
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, Direction.ASC);
    }

    /**
     * Bản sao có chỉ định sắp xếp; khóa chính vẫn là tiêu chí phụ.
     *
     * @param field     tên field Java của thực thể
     * @param direction chiều sắp xếp
     * @throws IllegalArgumentException nếu <code>field</code> rỗng
     */
    @NonNull
    public PageRequest orderBy(@NonNull String field, @NonNull Direction direction) {
        Assert.hasText(field, "field cannot be blank");
        return new PageRequest(page, size, field.trim(), direction);
    }

    /** Trang kế tiếp, giữ nguyên kích thước và cách sắp xếp. */
    @NonNull
    public PageRequest next() {
        return new PageRequest(page + 1, size, sortField, direction);
    }

    /** Số trang, bắt đầu từ 0. */
    public int page() {
        return page;
    }

    /** Số dòng mỗi trang. */
    public int size() {
        return size;
    }

    /** Số dòng cần bỏ qua (<code>OFFSET</code>), tính bằng <code>long</code> để không tràn khi <code>page</code> lớn. */
    public long offset() {
        return (long) page * size;
    }

    /** @return tên field sắp xếp, hoặc <code>null</code> nếu không chỉ định */
    @Nullable
    public String sortField() {
        return sortField;
    }

    /** Chiều sắp xếp; chỉ có nghĩa khi {@link #sortField()} khác <code>null</code>. */
    @NonNull
    public Direction direction() {
        return direction;
    }
}
