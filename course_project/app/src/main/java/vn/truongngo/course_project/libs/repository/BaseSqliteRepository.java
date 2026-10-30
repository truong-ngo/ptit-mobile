package vn.truongngo.course_project.libs.repository;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import vn.truongngo.course_project.libs.repository.specification.Criteria;
import vn.truongngo.course_project.libs.util.Assert;

/**
 * <p>Cài đặt {@link Repository} trên SQLite: ánh xạ thực thể với bảng bằng annotation (<code>@Table</code>,
 * <code>@Id</code>, <code>@Column</code>, <code>@References</code>) thay vì <code>RowMapper</code> viết tay.</p>
 *
 * <p>Mọi hàm chạm vào ổ đĩa nên <b>phải gọi ngoài main thread</b> (vi phạm thì ném <code>IllegalStateException</code>),
 * thường qua <code>Async.run</code>. Đường đọc dùng {@link #readableDb()}, đường ghi dùng {@link #writableDb()}; muốn
 * truy vấn riêng cho một bảng thì kế thừa lớp này và dùng hai hàm đó, hoặc dùng {@link #query} cho trường hợp đơn giản.</p>
 *
 * <pre>{@code
 * BaseSqliteRepository<Book, Long> books = new BaseSqliteRepository<>(helper, Book.class);
 * Async.run(lifetime(), () -> books.search(criteria, PageRequest.of(0, 20)), page -> adapter.submit(page.content()));
 * }</pre>
 *
 * <p><code>SQLiteDatabase</code> lấy từ {@link #readableDb()} và {@link #writableDb()} do <code>BaseDbHelper</code> sở hữu và
 * dùng chung cho cả tiến trình, nên repository <b>không bao giờ đóng nó</b> (đóng sẽ cắt ngang các thread khác và buộc
 * mở lại ở lệnh sau); chỉ <code>Cursor</code> mới được đóng sau mỗi truy vấn. Vì vậy cảnh báo <code>resource</code> của IDE
 * về <code>AutoCloseable</code> được tắt ở cấp lớp.</p>
 *
 * @param <E>  thực thể: có constructor không tham số và đúng một field <code>@Id</code>
 * @param <ID> kiểu khóa chính
 */
@SuppressWarnings("resource")
public class BaseSqliteRepository<E, ID> implements Repository<E, ID> {

    /** Nơi lấy SQLiteDatabase; sở hữu và đóng/mở database, repository không đóng. */
    private final BaseDbHelper helper;

    /** Siêu dữ liệu ánh xạ của thực thể, lấy từ cache theo lớp. */
    private final EntityMeta<E> meta;

    /**
     * Quét siêu dữ liệu của <code>entityClass</code> (lần đầu mới quét, sau đó dùng cache) và kiểm tra bảng của nó có
     * trong <code>helper</code>. Chưa mở database, việc đó chỉ xảy ra ở lệnh đầu tiên.
     *
     * @throws IllegalArgumentException nếu <code>entityClass</code> chưa được đăng ký trong <code>helper</code> (bảng sẽ không tồn tại)
     * @throws IllegalStateException    nếu <code>entityClass</code> không hợp lệ làm thực thể (thiếu <code>@Id</code>, kiểu field chưa hỗ trợ...)
     */
    public BaseSqliteRepository(@NonNull BaseDbHelper helper, @NonNull Class<E> entityClass) {
        Assert.notNull(helper, "helper cannot be null");
        this.helper = helper;
        this.meta = EntityMetaRegistry.of(entityClass);
        Assert.isTrue(helper.entities().contains(entityClass), () -> entityClass.getName() + " is not registered with BaseDbHelper, its table would not exist");
    }

    /**
     * <p>Database để <b>đọc</b> (<code>SELECT</code>), dành cho lớp con viết truy vấn riêng. Thường là chính đối tượng của
     * {@link #writableDb()}; chỉ khi database buộc phải mở ở chế độ chỉ đọc (ví dụ đầy ổ đĩa) thì mới khác, lúc đó các
     * lệnh đọc vẫn chạy còn lệnh ghi sẽ lỗi.</p>
     *
     * <p>Lần mở đầu tiên có thể tạo hoặc tạo lại bảng, nên cấm gọi ở main thread.</p>
     *
     * @throws IllegalStateException nếu gọi ở main thread
     */
    @NonNull
    protected final SQLiteDatabase readableDb() {
        BaseDbHelper.requireWorkerThread();
        return helper.getReadableDatabase();
    }

    /**
     * Database để <b>ghi</b> (<code>INSERT</code>, <code>UPDATE</code>, <code>DELETE</code>), dành cho lớp con viết lệnh
     * riêng. Mở không được ở chế độ ghi thì ném lỗi ngay, thay vì im lặng trả về bản chỉ đọc. Cũng cấm gọi ở main thread.
     *
     * @throws IllegalStateException nếu gọi ở main thread, hoặc database không mở được ở chế độ ghi
     */
    @NonNull
    protected final SQLiteDatabase writableDb() {
        BaseDbHelper.requireWorkerThread();
        return helper.getWritableDatabase();
    }

    /**
     * {@inheritDoc}
     * <p>Khóa tự tăng thì cột id bị bỏ qua khi ghi để SQLite tự sinh. Khóa tự gán (<code>@Id(autoIncrement = false)</code>)
     * thì phải khác null.</p>
     *
     * @throws IllegalArgumentException                          nếu khóa tự gán là null
     * @throws android.database.sqlite.SQLiteConstraintException nếu vi phạm ràng buộc (<code>NOT NULL</code>,
     *                                                          <code>UNIQUE</code>, khóa ngoại), ví dụ tên thể loại trùng
     */
    @NonNull
    @Override
    public E insert(@NonNull E entity) {
        Assert.notNull(entity, "entity cannot be null");
        EntityMeta.ColumnMeta idColumn = meta.idColumn;
        if (!idColumn.autoIncrement()) {
            Assert.notNull(idColumn.getter().apply(entity), "A manually assigned primary key must not be null on insert");
        }

        Collection<EntityMeta.ColumnMeta> columns = idColumn.autoIncrement() ? meta.nonIdColumns : meta.columns;
        long rowId = writableDb().insertOrThrow(meta.tableName, idColumn.columnName(), toValues(entity, columns));
        if (idColumn.autoIncrement()) {
            idColumn.setter().accept(entity, SqlTypes.fromDb(rowId, idColumn.fieldType()));
        }
        return entity;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException                          nếu khóa chính của <code>entity</code> là null
     * @throws android.database.sqlite.SQLiteConstraintException nếu giá trị mới vi phạm ràng buộc (<code>NOT NULL</code>,
     *                                                          <code>UNIQUE</code>, khóa ngoại)
     */
    @Override
    public void update(@NonNull E entity) {
        Assert.notNull(entity, "entity cannot be null");
        Object idValue = meta.idColumn.getter().apply(entity);
        Assert.notNull(idValue, "Cannot update entity with null @Id value");

        int affected = writableDb().update(meta.tableName, toValues(entity, meta.nonIdColumns),
                meta.idColumn.columnName() + " = ?", new String[]{SqlTypes.toArg(idValue)});
        if (affected == 0) {
            throw new IllegalStateException("No record found to update in " + meta.tableName + " with id = " + idValue);
        }
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public Optional<E> findById(@NonNull ID id) {
        Assert.notNull(id, "id cannot be null");
        List<E> rows = run(meta.idColumn.columnName() + " = ?", new String[]{SqlTypes.toArg(id)}, null, "1");
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** {@inheritDoc} */
    @Override
    public boolean existsById(@NonNull ID id) {
        Assert.notNull(id, "id cannot be null");
        return DatabaseUtils.queryNumEntries(readableDb(), meta.tableName,
                meta.idColumn.columnName() + " = ?", new String[]{SqlTypes.toArg(id)}) > 0;
    }

    /** {@inheritDoc} */
    @Override
    public boolean deleteById(@NonNull ID id) {
        Assert.notNull(id, "id cannot be null");
        return writableDb().delete(meta.tableName, meta.idColumn.columnName() + " = ?", new String[]{SqlTypes.toArg(id)}) > 0;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public List<E> findAll() {
        return run(null, null, defaultOrder(), null);
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public Page<E> findAll(@NonNull PageRequest request) {
        return search(null, request);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException nếu có điều kiện trỏ tới field không thuộc thực thể, hoặc <code>BETWEEN</code>
     *                                  không đủ hai cận
     */
    @NonNull
    @Override
    public List<E> search(List<Criteria> criteria) {
        WhereBuilder.Where where = WhereBuilder.build(meta, criteria);
        return run(where.sql(), where.args(), defaultOrder(), null);
    }

    /**
     * {@inheritDoc}
     * <p>Lấy thêm một dòng so với <code>request.size()</code> để biết còn trang sau hay không, nên không tốn thêm truy
     * vấn <code>COUNT</code>.</p>
     *
     * @throws IllegalArgumentException nếu có điều kiện hoặc field sắp xếp trỏ tới field không thuộc thực thể, hoặc
     *                                  <code>BETWEEN</code> không đủ hai cận
     */
    @NonNull
    @Override
    public Page<E> search(List<Criteria> criteria, @NonNull PageRequest request) {
        Assert.notNull(request, "request cannot be null");
        WhereBuilder.Where where = WhereBuilder.build(meta, criteria);
        List<E> rows = run(where.sql(), where.args(), orderBy(request), request.offset() + "," + (request.size() + 1));
        boolean hasNext = rows.size() > request.size();
        List<E> content = hasNext ? new ArrayList<>(rows.subList(0, request.size())) : rows;
        return new Page<>(content, request.page(), request.size(), hasNext);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException nếu có điều kiện trỏ tới field không thuộc thực thể, hoặc <code>BETWEEN</code>
     *                                  không đủ hai cận
     */
    @Override
    public long count(List<Criteria> criteria) {
        WhereBuilder.Where where = WhereBuilder.build(meta, criteria);
        return DatabaseUtils.queryNumEntries(readableDb(), meta.tableName, where.sql(), where.args());
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException nếu <code>where</code> rỗng
     */
    @NonNull
    @Override
    public List<E> query(@NonNull String where, String... args) {
        Assert.hasText(where, "where cannot be blank");
        return run(where, args, defaultOrder(), null);
    }

    /**
     * Chạy một truy vấn <code>SELECT</code> trên bảng của thực thể và ánh xạ từng dòng. Mọi tham số có thể null, nghĩa là
     * không dùng mệnh đề đó.
     *
     * @param limit chuỗi <code>"count"</code> hoặc <code>"offset,count"</code> theo cú pháp của SQLite
     */
    private List<E> run(String selection, String[] args, String orderBy, String limit) {
        List<E> rows = new ArrayList<>();
        try (Cursor cursor = readableDb().query(meta.tableName, null, selection, args, null, null, orderBy, limit)) {
            while (cursor.moveToNext()) {
                rows.add(mapRow(cursor));
            }
        }
        return rows;
    }

    /**
     * Ánh xạ dòng hiện tại của con trỏ thành một thực thể mới, để lớp con ghi đè khi cần xử lý thêm. Cột NULL thì field
     * giữ giá trị mặc định của constructor.
     */
    @NonNull
    protected E mapRow(@NonNull Cursor cursor) {
        E entity = meta.newInstance();
        for (EntityMeta.ColumnMeta column : meta.columns) {
            int index = cursor.getColumnIndexOrThrow(column.columnName());
            if (cursor.isNull(index)) {
                continue;
            }
            Object raw = switch (column.sqlType()) {
                case INTEGER -> cursor.getLong(index);
                case REAL -> cursor.getDouble(index);
                case TEXT -> cursor.getString(index);
                case BLOB -> cursor.getBlob(index);
            };
            column.setter().accept(entity, SqlTypes.fromDb(raw, column.fieldType()));
        }
        return entity;
    }

    /** Đọc các cột của <code>entity</code> thành <code>ContentValues</code>; giá trị null được ghi là <code>NULL</code>. */
    private ContentValues toValues(E entity, Collection<EntityMeta.ColumnMeta> columns) {
        ContentValues values = new ContentValues();
        for (EntityMeta.ColumnMeta column : columns) {
            Object db = SqlTypes.toDb(column.getter().apply(entity));
            String key = column.columnName();
            if (db == null) {
                values.putNull(key);
            } else if (db instanceof Long) {
                values.put(key, (Long) db);
            } else if (db instanceof Double) {
                values.put(key, (Double) db);
            } else if (db instanceof String) {
                values.put(key, (String) db);
            } else {
                values.put(key, (byte[]) db);
            }
        }
        return values;
    }

    /** Thứ tự mặc định khi không yêu cầu sắp xếp: khóa chính giảm dần. */
    private String defaultOrder() {
        return meta.idColumn.columnName() + " DESC";
    }

    /**
     * Mệnh đề <code>ORDER BY</code> theo yêu cầu, luôn kèm khóa chính làm tiêu chí phụ để thứ tự ổn định giữa các trang
     * (hai dòng trùng giá trị sắp xếp không bị trùng hay sót khi chuyển trang).
     *
     * @throws IllegalArgumentException nếu field sắp xếp không thuộc thực thể
     */
    private String orderBy(PageRequest request) {
        if (request.sortField() == null) {
            return defaultOrder();
        }
        EntityMeta.ColumnMeta column = meta.byField.get(request.sortField());
        if (column == null) {
            throw new IllegalArgumentException(
                    "Unknown sort field '" + request.sortField() + "' on entity " + meta.type.getSimpleName());
        }
        if (column == meta.idColumn) {
            return column.columnName() + " " + request.direction();
        }
        return column.columnName() + " " + request.direction() + ", " + defaultOrder();
    }
}
