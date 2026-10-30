package vn.truongngo.course_project.libs.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import vn.truongngo.course_project.bookstore.domain.Book;
import vn.truongngo.course_project.bookstore.domain.BookCondition;
import vn.truongngo.course_project.bookstore.domain.Category;
import vn.truongngo.course_project.libs.repository.specification.Criteria;
import vn.truongngo.course_project.libs.repository.specification.CriteriaBuilder;

/**
 * Kiểm tra các lớp domain của BTL ánh xạ đúng sang schema và sang hai truy vấn của đề. Đặt ở package của thư viện
 * vì <code>SchemaBuilder</code> và <code>WhereBuilder</code> là package-private.
 */
public class BookstoreSchemaTest {

    @Test
    public void schema_matchesTheDomainDesign() {
        assertEquals("CREATE TABLE category (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)",
                SqlBuilder.createTable(EntityMetaRegistry.of(Category.class)));
        assertEquals("CREATE TABLE book (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, author TEXT, "
                        + "condition TEXT NOT NULL, price INTEGER NOT NULL, category_id INTEGER, "
                        + "FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL)",
                SqlBuilder.createTable(EntityMetaRegistry.of(Book.class)));
    }

    @Test
    public void schema_createsCategoryBeforeBook() {
        List<String> statements = SqlBuilder.createStatements(Book.class, Category.class);
        assertTrue(statements.get(0).startsWith("CREATE TABLE category"));
        assertTrue(statements.get(1).startsWith("CREATE TABLE book"));
    }

    @Test
    public void query_booksByCategory() {
        List<Criteria> criteria = CriteriaBuilder.builder().where("categoryId").isEqualTo(3L).build();
        WhereBuilder.Where where = WhereBuilder.build(EntityMetaRegistry.of(Book.class), criteria);
        assertEquals("category_id = ?", where.sql());
        assertEquals(Arrays.asList("3"), Arrays.asList(where.args()));
    }

    @Test
    public void query_goodConditionUnder100k_storesConditionByNameNotByLabel() {
        List<Criteria> criteria = CriteriaBuilder.builder()
                .where("condition").isEqualTo(BookCondition.GOOD)
                .where("price").isLessThan(100_000L)
                .build();
        WhereBuilder.Where where = WhereBuilder.build(EntityMetaRegistry.of(Book.class), criteria);
        assertEquals("condition = ? AND price < ?", where.sql());
        assertEquals(Arrays.asList("GOOD", "100000"), Arrays.asList(where.args()));
    }

    @Test
    public void enumIsStoredByNameAndDisplayedByLabel() {
        assertEquals("GOOD", SqlTypes.toDb(BookCondition.GOOD));
        assertEquals(BookCondition.FAIR, SqlTypes.fromDb("FAIR", BookCondition.class));
        assertEquals("Tốt", BookCondition.GOOD.toString());
    }
}
