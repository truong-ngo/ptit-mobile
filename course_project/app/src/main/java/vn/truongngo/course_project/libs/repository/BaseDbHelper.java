package vn.truongngo.course_project.libs.repository;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Looper;
import android.util.Log;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Mở file SQLite và tạo bảng từ annotation của các thực thể được đăng ký (giống <code>hbm2ddl</code> của Hibernate).</p>
 *
 * <p>Version tự suy ra từ schema (mã băm các câu <code>CREATE TABLE</code>): thêm, bớt hay đổi field và ràng buộc thì
 * version đổi, không cần tăng số bằng tay. Khi version trong file khác version tính được, các bảng của thực thể bị xóa
 * và tạo lại nên <b>mất dữ liệu cũ</b> &mdash; đủ cho bài tập, không dùng cho dữ liệu thật. Bảng của thực thể đã bị bỏ
 * khỏi danh sách đăng ký thì không bị xóa.</p>
 *
 * <p>Giữ <b>một thể hiện cho mỗi file database</b> trong cả tiến trình (ví dụ một trường tĩnh hoặc ở lớp
 * <code>Application</code>): nhiều thể hiện cùng một file sẽ tranh khóa nhau. Mở database chạm vào ổ đĩa nên các hàm lấy
 * <code>SQLiteDatabase</code> phải gọi ngoài main thread.</p>
 *
 * <p>Tạo helper <b>chưa</b> mở file và chưa tạo bảng: việc đó chỉ xảy ra ở lần đầu <code>getWritableDatabase</code> được
 * gọi (thường qua một repository). Ngược lại, schema được dựng và kiểm tra ngay trong constructor, nên đăng ký sai thực
 * thể sẽ báo lỗi sớm thay vì lúc mở database.</p>
 *
 * <pre>{@code
 * BaseDbHelper helper = new BaseDbHelper(context, "books.db", Category.class, Book.class);
 * BaseSqliteRepository<Book, Long> books = new BaseSqliteRepository<>(helper, Book.class);
 * }</pre>
 */
public class BaseDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "BaseDbHelper";

    /** Bản sao của danh sách truyền vào constructor, để người gọi sửa mảng gốc sau đó cũng không đổi schema đã dựng. */
    private final Class<?>[] entities;

    /**
     * Dựng schema từ các thực thể để tính version, chưa mở file database. <code>context</code> được đổi sang
     * <code>Application</code> để helper sống lâu hơn Activity mà không giữ nó lại (rò rỉ bộ nhớ).
     *
     * @param name     tên file database, nằm trong thư mục riêng của ứng dụng
     * @param entities các lớp thực thể, thứ tự bất kỳ (thư viện tự xếp bảng cha trước bảng con)
     * @throws IllegalArgumentException nếu không đăng ký thực thể nào
     * @throws IllegalStateException    nếu một thực thể không hợp lệ, hoặc khóa ngoại tham chiếu thực thể chưa đăng ký
     */
    public BaseDbHelper(@NonNull Context context, @NonNull String name, @NonNull Class<?>... entities) {
        super(context.getApplicationContext(), name, null, SqlBuilder.versionOf(entities));
        this.entities = entities.clone();
    }

    /** Các thực thể đã đăng ký, dùng để kiểm tra repository chỉ trỏ tới bảng có thật. */
    @NonNull
    List<Class<?>> entities() {
        return Collections.unmodifiableList(Arrays.asList(entities));
    }

    /**
     * <p>Chạy <code>work</code> trong một transaction: mọi lệnh ghi của các repository dùng chung helper này, trên cùng
     * thread, hoặc cùng được lưu hoặc cùng bị hủy (giống <code>@Transactional</code> khi các repository chung một
     * <code>DataSource</code>). Transaction của SQLite gắn với thread, nên <code>work</code> phải chạy hết trên thread gọi
     * hàm này, không được đẩy sang thread khác.</p>
     *
     * <p><code>work</code> ném ngoại lệ thì toàn bộ bị hủy và ngoại lệ được ném lại nguyên vẹn. Lồng nhau thì chỉ lớp
     * ngoài cùng commit, và một lớp trong bị lỗi sẽ làm hủy cả lớp ngoài.</p>
     *
     * @throws IllegalStateException nếu gọi ở main thread
     */
    public void doInTransaction(@NonNull Runnable work) {
        requireWorkerThread();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            work.run();
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Mở database chạm vào ổ đĩa (và có thể chạy <code>onCreate</code>/<code>onUpgrade</code>), nên không được làm ở main
     * thread vì sẽ chặn giao diện.
     *
     * @throws IllegalStateException nếu gọi ở main thread
     */
    static void requireWorkerThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("A repository touches SQLite and must not be called on the main thread. Use Async.run(...).");
        }
    }

    /** SQLite tắt ràng buộc khóa ngoại theo mặc định, nên phải bật lại ở mỗi lần mở. */
    @Override
    public void onConfigure(@NonNull SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Tạo toàn bộ bảng theo thứ tự cha trước con. Chỉ chạy khi file database chưa tồn tại, hoặc từ
     * {@link #recreate} sau khi các bảng cũ đã bị xóa.
     */
    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        for (String sql : SqlBuilder.createStatements(entities)) {
            db.execSQL(sql);
        }
    }

    /** Xóa và tạo lại bảng, dữ liệu cũ bị mất. */
    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db, oldVersion, newVersion);
    }

    /**
     * Version là mã băm nên schema mới có thể mang số nhỏ hơn dù là bản mới hơn; xử lý như nâng cấp thay vì để
     * <code>SQLiteOpenHelper</code> ném lỗi mặc định.
     */
    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        recreate(db, oldVersion, newVersion);
    }

    /**
     * Xóa các bảng của thực thể (con trước cha) rồi tạo lại. Việc kiểm tra khóa ngoại được hoãn tới lúc commit để việc
     * xóa bảng không vướng ràng buộc của schema cũ.
     */
    private void recreate(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Schema changed (" + oldVersion + " -> " + newVersion + "), dropping and recreating tables, existing data is lost");
        db.execSQL("PRAGMA defer_foreign_keys = ON");
        for (String sql : SqlBuilder.dropStatements(entities)) {
            db.execSQL(sql);
        }
        onCreate(db);
    }
}
