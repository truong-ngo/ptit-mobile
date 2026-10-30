package vn.truongngo.course_project.bookstore.domain;

import java.util.Objects;

import vn.truongngo.course_project.libs.repository.annotation.Column;
import vn.truongngo.course_project.libs.repository.annotation.Id;
import vn.truongngo.course_project.libs.repository.annotation.Table;

/**
 * Thể loại sách (ví dụ Văn học, Khoa học, Tiểu sử).
 */
@Table(name = "category")
public class Category {

    /** Null cho tới khi được <code>insert</code>: SQLite tự sinh khóa tự tăng rồi gán ngược vào đây. */
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    public Category() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Category other)) {
            return false;
        }
        return Objects.equals(id, other.id) && Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
