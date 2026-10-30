package vn.truongngo.course_project.bookstore.ui.book.model;

/**
 * <p>Trạng thái tải dữ liệu theo trang của một ViewModel danh sách: không tải, đang tải lại từ trang đầu, hay đang tải
 * trang kế. ViewModel giữ nó trong một <code>LiveData</code> để giao diện chọn đúng chỉ báo cho từng loại tải.</p>
 *
 * <p>Dùng một enum thay cho hai cờ <code>boolean</code> (đang tải, đang tải thêm) vì hai cờ cho phép tổ hợp vô nghĩa
 * (đang tải thêm mà không đang tải) và phải đặt đúng thứ tự, nếu lệch thì observer thấy trạng thái nửa vời. Một enum
 * chỉ có một lần đổi cho mỗi thay đổi và không có tổ hợp sai. Cũng không nên đoán loại tải từ nội dung danh sách (có sách
 * hay chưa): đổi bộ lọc khi danh sách cũ còn sách sẽ bị hiểu nhầm là tải trang kế.</p>
 *
 * <p>Giá trị chuyển theo vòng <code>IDLE</code> &rarr; <code>REFRESHING</code> hoặc <code>LOADING_MORE</code> &rarr;
 * <code>IDLE</code>, và chỉ ViewModel được đổi. Tại mọi thời điểm chỉ có một lần tải chạy, vì lần tải mới hủy lần tải cũ
 * (xem <code>Job.cancel</code>).</p>
 */
public enum LoadState {

    /**
     * Không có lần tải nào. Đây là lúc đúng để hiện chữ "danh sách rỗng", vì rỗng khi đang tải chưa có nghĩa là không có
     * dữ liệu.
     */
    IDLE,

    /**
     * Đang tải <b>trang đầu</b>: mở màn hình lần đầu hoặc đổi bộ lọc. Danh sách cũ (nếu có) vẫn hiện cho tới khi trang
     * đầu mới về và thay thế nó, nên chỉ báo nên đặt ở nơi người dùng vừa thao tác, không ở cuối danh sách.
     */
    REFRESHING,

    /**
     * Đang tải <b>trang kế</b> để nối vào cuối danh sách (load more). Danh sách đã có sách và người dùng đang cuộn, nên
     * chỉ báo nên đặt ở cuối danh sách và không che các dòng.
     */
    LOADING_MORE;

    /** Có một lần tải đang chạy, dù thuộc loại nào. Tương đương <code>this != IDLE</code>. */
    public boolean isLoading() {
        return this != IDLE;
    }
}
