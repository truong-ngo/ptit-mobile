package vn.truongngo.course_project.libs.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import vn.truongngo.course_project.libs.repository.annotation.Column;
import vn.truongngo.course_project.libs.repository.annotation.Id;
import vn.truongngo.course_project.libs.repository.annotation.OnDelete;
import vn.truongngo.course_project.libs.repository.annotation.References;
import vn.truongngo.course_project.libs.repository.annotation.Table;
import vn.truongngo.course_project.libs.repository.specification.Criteria;
import vn.truongngo.course_project.libs.repository.specification.CriteriaBuilder;
import vn.truongngo.course_project.libs.repository.specification.Operator;

/**
 * Kiểm thử phần lõi thuần Java của ORM (siêu dữ liệu, sinh DDL, dựng WHERE, chuyển kiểu) trên JVM.
 * Phần chạm SQLite thật ({@code BaseSqliteRepository}, {@code BaseDbHelper}) cần thiết bị nên chưa được kiểm thử ở đây.
 */
public class RepositoryCoreTest {

    enum Condition {TOT, KHA, CU}

    @Table(name = "category")
    static class Category {
        @Id
        Long id;
        @Column(nullable = false, unique = true)
        String name;
    }

    @Table(name = "book")
    static class Book {
        @Id
        Long id;
        @Column(nullable = false)
        String title;
        String author;
        Condition condition;
        long price;
        boolean hot;
        @Column(name = "cat_id")
        @References(entity = Category.class, onDelete = OnDelete.SET_NULL)
        Long categoryId;
        transient String note;
    }

    static class NoId {
        String name;
    }

    static class TwoIds {
        @Id
        Long a;
        @Id
        Long b;
    }

    static class BadType {
        @Id
        Long id;
        Object payload;
    }

    static class SetNullNotNullable {
        @Id
        Long id;
        @Column(nullable = false)
        @References(entity = Category.class, onDelete = OnDelete.SET_NULL)
        Long categoryId;
    }

    // ---- EntityMeta -------------------------------------------------------------------------------------

    @Test
    public void entityMeta_namesAndColumns() {
        EntityMeta<Book> meta = EntityMetaRegistry.of(Book.class);
        assertEquals("book", meta.tableName);
        assertEquals("id", meta.idColumn.columnName());
        assertTrue(meta.idColumn.autoIncrement());
        assertEquals("cat_id", meta.byField.get("categoryId").columnName());
        assertEquals("title", meta.byField.get("title").columnName());
        assertNull("transient field không phải cột", meta.byField.get("note"));
        assertEquals(7, meta.columns.size());
    }

    @Test
    public void entityMeta_defaultTableNameIsSnakeCase() {
        assertEquals("no_id_like", EntityMeta.toSnakeCase("NoIdLike"));
        assertEquals("category_id", EntityMeta.toSnakeCase("categoryId"));
    }

    @Test
    public void registry_scansOncePerClassAndReusesTheResult() {
        assertSame(EntityMetaRegistry.of(Book.class), EntityMetaRegistry.of(Book.class));
        // scan không cache: mỗi lần gọi trực tiếp cho một đối tượng mới, khác với kết quả của registry
        assertNotEquals(EntityMeta.scan(Book.class), EntityMetaRegistry.of(Book.class));
    }

    @Test
    public void entityMeta_rejectsInvalidEntities() {
        assertThrows(IllegalStateException.class, () -> EntityMetaRegistry.of(NoId.class));
        assertThrows(IllegalStateException.class, () -> EntityMetaRegistry.of(TwoIds.class));
        assertThrows(IllegalStateException.class, () -> EntityMetaRegistry.of(BadType.class));
        assertThrows(IllegalStateException.class, () -> EntityMetaRegistry.of(SetNullNotNullable.class));
    }

    @Test
    public void entityMeta_getterSetterAndNewInstance() {
        EntityMeta<Book> meta = EntityMetaRegistry.of(Book.class);
        Book book = meta.newInstance();
        meta.byField.get("title").setter().accept(book, "Số đỏ");
        assertEquals("Số đỏ", book.title);
        assertEquals("Số đỏ", meta.byField.get("title").getter().apply(book));
    }

    // ---- SchemaBuilder ----------------------------------------------------------------------------------

    @Test
    public void schema_createTableHasConstraintsAndForeignKey() {
        assertEquals("CREATE TABLE category (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE)",
                SqlBuilder.createTable(EntityMetaRegistry.of(Category.class)));
        assertEquals("CREATE TABLE book (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, author TEXT, "
                        + "condition TEXT, price INTEGER, hot INTEGER, cat_id INTEGER, "
                        + "FOREIGN KEY (cat_id) REFERENCES category(id) ON DELETE SET NULL)",
                SqlBuilder.createTable(EntityMetaRegistry.of(Book.class)));
    }

    @Test
    public void schema_parentCreatedBeforeChildAndDroppedAfter() {
        // Cố ý đăng ký con trước cha: thư viện phải tự xếp lại.
        List<String> create = SqlBuilder.createStatements(Book.class, Category.class);
        assertTrue(create.get(0).startsWith("CREATE TABLE category"));
        assertTrue(create.get(1).startsWith("CREATE TABLE book"));

        List<String> drop = SqlBuilder.dropStatements(Book.class, Category.class);
        assertEquals(Arrays.asList("DROP TABLE IF EXISTS book", "DROP TABLE IF EXISTS category"), drop);
    }

    @Test
    public void schema_unregisteredParentIsReported() {
        assertThrows(IllegalStateException.class, () -> SqlBuilder.createStatements(Book.class));
    }

    @Test
    public void schema_versionIsStablePositiveAndDependsOnSchema() {
        int both = SqlBuilder.versionOf(Category.class, Book.class);
        assertEquals(both, SqlBuilder.versionOf(Category.class, Book.class));
        assertEquals("thứ tự đăng ký không đổi version", both, SqlBuilder.versionOf(Book.class, Category.class));
        assertTrue(both >= 1);
        assertNotEquals(both, SqlBuilder.versionOf(Category.class));
    }

    // ---- WhereBuilder -----------------------------------------------------------------------------------

    @Test
    public void where_noCriteriaMeansNoClause() {
        EntityMeta<Book> meta = EntityMetaRegistry.of(Book.class);
        assertNull(WhereBuilder.build(meta, null).sql());
        assertNull(WhereBuilder.build(meta, CriteriaBuilder.builder().build()).sql());
    }

    @Test
    public void where_btlQuery_goodConditionUnder100k() {
        List<Criteria> criteria = CriteriaBuilder.builder()
                .where("condition").isEqualTo(Condition.TOT)
                .where("price").isLessThan(100_000)
                .build();
        WhereBuilder.Where where = WhereBuilder.build(EntityMetaRegistry.of(Book.class), criteria);
        assertEquals("condition = ? AND price < ?", where.sql());
        assertArrayEquals(new String[]{"TOT", "100000"}, where.args());
    }

    @Test
    public void where_fieldNamesAreMappedToColumnsAndBlankValuesSkipped() {
        List<Criteria> criteria = CriteriaBuilder.builder()
                .where("categoryId").isEqualTo(3L)
                .where("title").contains("  ")
                .where("author").contains("Nam")
                .build();
        WhereBuilder.Where where = WhereBuilder.build(EntityMetaRegistry.of(Book.class), criteria);
        assertEquals("cat_id = ? AND author LIKE ?", where.sql());
        assertArrayEquals(new String[]{"3", "%Nam%"}, where.args());
    }

    @Test
    public void where_inBetweenAndNullOperators() {
        EntityMeta<Book> meta = EntityMetaRegistry.of(Book.class);

        WhereBuilder.Where in = WhereBuilder.build(meta, CriteriaBuilder.builder()
                .where("condition").in(Arrays.asList(Condition.TOT, Condition.KHA)).build());
        assertEquals("condition IN (?, ?)", in.sql());
        assertArrayEquals(new String[]{"TOT", "KHA"}, in.args());

        WhereBuilder.Where between = WhereBuilder.build(meta, CriteriaBuilder.builder()
                .where("price").isBetween(10, 20).build());
        assertEquals("price BETWEEN ? AND ?", between.sql());
        assertArrayEquals(new String[]{"10", "20"}, between.args());

        WhereBuilder.Where nulls = WhereBuilder.build(meta, CriteriaBuilder.builder()
                .where("categoryId").isNull().build());
        assertEquals("cat_id IS NULL", nulls.sql());
        assertEquals(0, nulls.args().length);

        WhereBuilder.Where emptyIn = WhereBuilder.build(meta, Arrays.asList(
                new Criteria("condition", Operator.IN, Arrays.asList())));
        assertEquals("1 = 0", emptyIn.sql());
    }

    @Test
    public void where_booleanBecomesZeroOne_andUnknownFieldIsRejected() {
        EntityMeta<Book> meta = EntityMetaRegistry.of(Book.class);
        WhereBuilder.Where where = WhereBuilder.build(meta, CriteriaBuilder.builder()
                .where("hot").isEqualTo(true).build());
        assertArrayEquals(new String[]{"1"}, where.args());

        assertThrows(IllegalArgumentException.class, () -> WhereBuilder.build(meta, CriteriaBuilder.builder()
                .where("nope").isEqualTo(1).build()));
    }

    // ---- SqlTypes ---------------------------------------------------------------------------------------

    @Test
    public void sqlTypes_roundTrip() {
        Date date = new Date(1_700_000_000_000L);
        assertEquals(1L, SqlTypes.toDb(true));
        assertEquals(true, SqlTypes.fromDb(1L, boolean.class));
        assertEquals(false, SqlTypes.fromDb(0L, Boolean.class));
        assertEquals("TOT", SqlTypes.toDb(Condition.TOT));
        assertEquals(Condition.KHA, SqlTypes.fromDb("KHA", Condition.class));
        assertEquals("12345678901234567890.123456789", SqlTypes.toDb(new BigDecimal("12345678901234567890.123456789")));
        assertEquals(new BigDecimal("1.50"), SqlTypes.fromDb("1.50", BigDecimal.class));
        assertEquals(date.getTime(), SqlTypes.toDb(date));
        assertEquals(date, SqlTypes.fromDb(date.getTime(), Date.class));
        assertEquals(42, SqlTypes.fromDb(42L, int.class));
        assertEquals(42L, SqlTypes.fromDb(42L, long.class));
        assertEquals('x', SqlTypes.fromDb("x", char.class));
        assertEquals(1.5, SqlTypes.fromDb(1.5, double.class));
    }

    @Test
    public void sqlTypes_columnTypes() {
        assertEquals(SqlType.INTEGER, SqlTypes.typeOf(int.class));
        assertEquals(SqlType.INTEGER, SqlTypes.typeOf(Boolean.class));
        assertEquals(SqlType.REAL, SqlTypes.typeOf(double.class));
        assertEquals(SqlType.TEXT, SqlTypes.typeOf(Condition.class));
        assertEquals(SqlType.TEXT, SqlTypes.typeOf(BigDecimal.class));
        assertEquals(SqlType.BLOB, SqlTypes.typeOf(byte[].class));
        assertNull(SqlTypes.typeOf(Object.class));
    }
}
