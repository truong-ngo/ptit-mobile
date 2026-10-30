package vn.truongngo.course_project.bookstore.domain;

import java.util.Objects;

import vn.truongngo.course_project.libs.repository.annotation.Column;
import vn.truongngo.course_project.libs.repository.annotation.Id;
import vn.truongngo.course_project.libs.repository.annotation.OnDelete;
import vn.truongngo.course_project.libs.repository.annotation.References;
import vn.truongngo.course_project.libs.repository.annotation.Table;

/**
 * Một cuốn sách cũ của cửa hàng. Mỗi sách thuộc nhiều nhất một thể loại.
 */
@Table(name = "book")
public class Book {

    /** Null cho tới khi được <code>insert</code>: SQLite tự sinh khóa tự tăng rồi gán ngược vào đây. */
    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(nullable = false)
    private BookCondition condition;

    /** Giá bán, đơn vị đồng, lưu số nguyên để không sai số. */
    @Column(nullable = false)
    private long price;

    /**
     * Null nghĩa là chưa phân loại. Xóa thể loại thì cột này của các sách thuộc nó tự về null (<code>SET_NULL</code>),
     * sách không bị xóa theo.
     */
    @References(entity = Category.class, onDelete = OnDelete.SET_NULL)
    private Long categoryId;

    public Book() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BookCondition getCondition() {
        return condition;
    }

    public void setCondition(BookCondition condition) {
        this.condition = condition;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /** So sánh theo mọi field, để <code>submit</code> của adapter biết dòng nào có nội dung thay đổi. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book other)) {
            return false;
        }
        return price == other.price
                && Objects.equals(id, other.id)
                && Objects.equals(title, other.title)
                && Objects.equals(author, other.author)
                && condition == other.condition
                && Objects.equals(categoryId, other.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, condition, price, categoryId);
    }
}
