package vn.truongngo.course_project.bookstore.domain;

import androidx.annotation.NonNull;

/**
 * <p>Tình trạng của một cuốn sách cũ.</p>
 *
 * <p>ORM lưu theo <b>tên hằng</b> (<code>GOOD</code>, <code>FAIR</code>...) chứ không theo nhãn hiển thị, nên đổi tên hằng
 * sẽ làm sai dữ liệu đã lưu; nhãn thì đổi tự do.</p>
 */
public enum BookCondition {

    GOOD("Tốt"),
    FAIR("Khá"),
    POOR("Kém");

    private final String label;

    BookCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Trả về nhãn, vì <code>Spinner</code> của <code>FormBinder</code> hiển thị mỗi giá trị enum bằng <code>toString</code>. */
    @NonNull
    @Override
    public String toString() {
        return label;
    }
}
