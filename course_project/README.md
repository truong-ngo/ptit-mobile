# course_project

Đồ án môn **Lập trình thiết bị di động** (PTIT), gồm hai phần trong cùng một module Android:

1. **`libs`**: một thư viện Android nhỏ, dùng lại được cho nhiều màn hình (Java, Android Views, không Compose, **không dùng code sinh tự động** như ViewBinding).
2. **`bookstore`**: ứng dụng bài tập lớn **Quản lý cửa hàng sách cũ**, viết trên thư viện đó.

Mục tiêu của thư viện là API gọn, lõi ít phụ thuộc, dùng annotation và reflection ở tầng model ↔ view, cùng tinh thần với các thư viện nhỏ trước đây của tác giả (mini-RMI, mini ORM, swing utils).

## Công nghệ

| | |
|---|---|
| Ngôn ngữ | Java 17 (source/target), Gradle chạy bằng JDK 21 |
| Android | minSdk 24, targetSdk 37, AGP 9.4.1 |
| Giao diện | Android Views, Material 3 (`NoActionBar`), edge-to-edge |
| Dữ liệu | SQLite (mini ORM tự viết), `ViewModel` + `LiveData` |
| Package gốc | `vn.truongngo.course_project` |

## Ứng dụng: Quản lý cửa hàng sách cũ

**Dữ liệu**
- `Book`: mã, tên sách, tác giả, tình trạng (`BookCondition`, enum), giá bán, thể loại.
- `Category`: mã, tên thể loại (tên là duy nhất). Xóa thể loại thì sách thuộc nó chuyển thành *chưa phân loại* (khóa ngoại `SET NULL`), không bị xóa.

**Chức năng**
- Thêm, sửa, xóa **thể loại**.
- Thêm, sửa, xóa **sách**, gán sách vào thể loại.
- Danh sách sách **phân trang** (cuộn tới cuối thì tải thêm).
- Lọc sách theo thể loại, tình trạng và khoảng giá, ví dụ sách tình trạng "Tốt" có giá dưới 100.000 đ. Ô "Giá đến" tính cả mốc (`<=`), nên "dưới 100.000" nhập `99999`.

**Màn hình**: hai tab (Sách, Thể loại) trong `MainActivity`; form thêm/sửa mở chồng lên bằng back stack, khi đó thanh tab ẩn đi.

## Cấu trúc mã nguồn

```
app/src/main/java/vn/truongngo/course_project/
├── MainActivity.java                 # chứa hai tab, chuyển tab, ẩn thanh tab khi ở màn con
├── MainViews.java
│
├── bookstore/                        # ứng dụng BTL
│   ├── domain/
│   │   ├── Book.java                 # thực thể sách (annotation ORM)
│   │   ├── BookCondition.java        # enum tình trạng sách
│   │   └── Category.java             # thực thể thể loại
│   ├── repository/
│   │   ├── BookRepository.java
│   │   ├── CategoryRepository.java
│   │   ├── BookstoreDatabase.java    # giữ helper và hai repository dùng chung
│   │   └── SampleData.java           # dữ liệu mẫu (hiện không được gọi)
│   └── ui/
│       ├── book/
│       │   ├── component/
│       │   │   ├── BookListFragment.java    # màn danh sách sách
│       │   │   ├── BookListViews.java
│       │   │   ├── BookItemViews.java       # một dòng sách
│       │   │   ├── BookFilter.java          # khối lọc (BaseLayout)
│       │   │   ├── BookFilterViews.java
│       │   │   ├── BookFormFragment.java    # form thêm/sửa/xóa sách
│       │   │   └── BookFormViews.java
│       │   └── model/
│       │       ├── BookListViewModel.java   # state danh sách, phân trang, bộ lọc
│       │       ├── BookFilterModel.java     # state khối lọc
│       │       ├── BookCriteria.java        # điều kiện lọc
│       │       ├── BookFormViewModel.java   # state form sách
│       │       ├── BookForm.java            # model ánh xạ ô nhập của form sách
│       │       └── LoadState.java           # IDLE / REFRESHING / LOADING_MORE
│       ├── category/
│       │   ├── component/
│       │   │   ├── CategoryListFragment.java
│       │   │   ├── CategoryListViews.java
│       │   │   ├── CategoryItemViews.java
│       │   │   ├── CategoryFormFragment.java
│       │   │   └── CategoryFormViews.java
│       │   └── model/
│       │       ├── CategoryListViewModel.java
│       │       ├── CategoryFormViewModel.java
│       │       └── CategoryForm.java
│       └── shared/
│           └── CatalogViewModel.java        # báo thể loại đổi giữa các tab
│
└── libs/                             # thư viện dùng lại
    ├── base/
    │   ├── BaseActivity.java
    │   ├── BaseFragment.java
    │   ├── BaseLayout.java
    │   ├── BaseViewModel.java
    │   ├── Event.java                # sự kiện xử lý một lần
    │   └── Lifetime.java             # khoảng đời để dọn tài nguyên
    ├── async/
    │   ├── Async.java                # chạy việc nền, callback về main thread
    │   └── Job.java                  # handle: cancel, onComplete
    ├── adapter/
    │   ├── GenericRecyclerAdapter.java
    │   ├── BaseViewHolder.java
    │   ├── ItemBinder.java           # lambda đổ dữ liệu có position
    │   └── LoadMoreListener.java     # tải thêm khi cuộn tới cuối
    ├── repository/                   # mini ORM trên SQLite
    │   ├── annotation/               # Table, Id, Column, References, OnDelete
    │   ├── specification/            # Criteria, CriteriaBuilder, Operator, WhereClause
    │   ├── Repository.java
    │   ├── BaseSqliteRepository.java
    │   ├── BaseDbHelper.java
    │   ├── Page.java, PageRequest.java
    │   └── (nội bộ) EntityMeta, EntityMetaRegistry, SqlBuilder, SqlType, SqlTypes, WhereBuilder
    ├── form/
    │   ├── annotation/FormField.java
    │   ├── validation/               # NotNull, Size, Min, Max, Pattern, SelfValidating, FormValidator
    │   ├── FormBinder.java
    │   └── (nội bộ) FormMeta, FormMetaRegistry, FormValues
    └── util/
        ├── ViewBinder.java, ViewId.java
        ├── ViewUtils.java, Keyboard.java
        ├── Money.java, Dialogs.java
        └── ReflectionUtils.java, ClassUtils.java, Assert.java, StringUtils.java

app/src/test/java/vn/truongngo/course_project/   # kiểm thử JVM
├── libs/repository/RepositoryCoreTest.java, BookstoreSchemaTest.java
├── libs/form/FormCoreTest.java
├── libs/adapter/LoadMoreListenerTest.java
├── libs/util/MoneyTest.java
└── ExampleUnitTest.java

app/src/main/res/layout/
├── activity_main.xml
├── fragment_book_list.xml, item_book.xml, layout_book_filter.xml, fragment_book_form.xml
├── fragment_category_list.xml, item_category.xml, fragment_category_form.xml
└── layout_user_banner.xml
```

Quy ước đặt tên: lớp `*Views` là **container giữ các view** của một màn hình hoặc một dòng (thay cho ViewBinding); `*Fragment` chỉ vẽ state và chuyển thao tác; `*ViewModel` giữ state và chạy việc nền.

## Thư viện `libs`

### `base/`: khung màn hình và vòng đời

- `BaseActivity<V>`, `BaseFragment<V>`, `BaseLayout<V>`: `V` là container view do người dùng viết. Mỗi lớp có các hook theo thứ tự cố định: `layoutResId()` → `bindViews(root)` → `initViews()` → `initEvents()` → (`observe()` ở Fragment) → `loadData()`.
- `Lifetime`: khoảng đời gắn với một `Lifecycle` (hoặc tự kết thúc cho `BaseLayout` và `ViewModel`). Dùng để hủy việc, đóng hộp thoại, gỡ listener khi màn hình bị hủy; `onEnd(Runnable)` đăng ký việc dọn.
- `BaseViewModel`: `AndroidViewModel` có `lifetime()` và `run(task, onSuccess[, onError])`.
- `Event<T>`: bọc sự kiện chỉ xử lý một lần (hiện Snackbar) để xoay màn hình không hiện lại.

```java
public class CategoryListFragment extends BaseFragment<CategoryListViews> {
    @Override protected int layoutResId() { return R.layout.fragment_category_list; }
    @Override protected CategoryListViews bindViews(View root) { return ViewBinder.create(CategoryListViews.class, root); }
    @Override protected void observe() { observe(viewModel.getCategories(), adapter::submit); }
    @Override protected void loadData() { viewModel.ensureLoaded(); }
}
```

Luồng một chiều: thao tác → `viewModel.hàm()` → việc nền → `onSuccess` đổi state (main thread) → `LiveData` → `observe()` vẽ lại.

### `async/`: việc nền

- `Async.run(lifetime, task, onSuccess[, onError])` chạy `task` ở pool nền (4 thread), callback luôn về **main thread** và luôn được post.
- `Lifetime` kết thúc thì callback bị bỏ nhưng việc nền vẫn chạy xong (lệnh ghi không mất khi thoát màn hình).
- `Job<T>` trả về: `cancel()` (bỏ kết quả, không bắt đầu việc còn trong hàng đợi), `onComplete(Runnable)` chạy như `finally`.

### `adapter/`: danh sách

- `GenericRecyclerAdapter<T, V>`: giữ danh sách, inflate layout, gom view con vào container và đổ dữ liệu bằng lambda; `submit` (kèm `DiffUtil` khi có `setIdProvider`), `append` (load more).
- `LoadMoreListener.attach(list, lifetime, onLoadMore)`: quyết định *khi nào* tải trang kế (cuộn tới còn vài dòng cuối), chặn tải trùng; nơi gọi báo `loaded(hasMore)` hoặc `failed()`.

```java
adapter = GenericRecyclerAdapter.of(R.layout.item_book, BookItemViews.class,
        (BookItemViews row, Book book) -> row.title.setText(book.getTitle()));
adapter.setIdProvider(Book::getId);
adapter.setOnItemClick((book, position) -> openForm(book.getId()));
```

### `repository/`: mini ORM trên SQLite

- Khai báo bảng bằng annotation: `@Table`, `@Id(autoIncrement)`, `@Column(name, nullable, unique)`, `@References(entity, onDelete)`.
- `BaseDbHelper` tạo bảng từ annotation (như `hbm2ddl`); version schema là mã băm các câu `CREATE TABLE`, nên **đổi schema thì bảng bị xóa và tạo lại (mất dữ liệu)**, chỉ hợp cho bài tập.
- `BaseSqliteRepository<E, ID>`: `insert`, `update`, `findById`, `deleteById`, `findAll`, `search(criteria[, PageRequest])`, `count`, `query(where, args)` cho trường hợp cần `OR`. Chạy đồng bộ và **cấm gọi ở main thread**, nên bọc trong `Async.run`.
- Lọc bằng danh sách `Criteria` nối `AND` (`CriteriaBuilder`); phân trang `PageRequest`/`Page` lấy thêm một dòng để biết `hasNext`.
- `BaseDbHelper.doInTransaction(Runnable)`: nhiều repository cùng lưu hoặc cùng hủy.
- Kiểu hỗ trợ: boolean, số, `String`, `char`, `BigDecimal`, enum, `java.util.Date`, `byte[]` (không hỗ trợ `java.time`, vì cần API 26).

### `form/`: ánh xạ model ↔ ô nhập

- `@FormField(id, label, order, readOnly)` gắn field của model với view trong XML; ràng buộc khai báo bằng `@NotNull`, `@Size`, `@Min`, `@Max`, `@Pattern`, `SelfValidating`.
- `FormBinder<M>`: `populate(model)` đổ model lên ô; `readInto(model)` đọc ô vào model **chỉ khi mọi ô hợp lệ**, báo tất cả lỗi cùng lúc cạnh từng ô và nhảy tới ô lỗi đầu; `choices(...)` cho Spinner; hỗ trợ `EditText`, `CheckBox`/`Switch`, `Spinner`.

```java
public class CategoryForm {
    @FormField(id = "etName", label = "Tên thể loại", order = 1)
    @NotNull(message = "Vui lòng nhập tên thể loại")
    @Size(max = 100, message = "Tên thể loại tối đa 100 ký tự")
    private String name;
}
// trong Fragment:  if (!form.readInto(model)) return;  viewModel.save(model);
```

### `util/`

- `ViewBinder` + `@ViewId`: điền view vào container bằng reflection (`@ViewId` lấy tên field làm tên id, hoặc `@ViewId("btnSave")`). Thay cho ViewBinding.
- `ViewUtils` (`require`, `visibleIf`), `Keyboard`, `Money` (định dạng và đọc tiền VND), `Dialogs` (`confirm`, `message` gắn `Lifetime`).
- `ReflectionUtils`, `ClassUtils`, `Assert`, `StringUtils` chép từ thư viện cũ của tác giả, chỉ sửa vài chỗ không hợp Android (ghi trong `CLAUDE.md`).

## Build và chạy

Yêu cầu: Android Studio (hoặc JDK 21 cho Gradle) và Android SDK có API 37.

```bash
./gradlew assembleDebug                    # build
./gradlew lintDebug                        # lint (bắt lỗi API mà assembleDebug bỏ sót)
./gradlew testDebugUnitTest                # kiểm thử JVM
./gradlew assembleDebug lintDebug testDebugUnitTest
```

Kiểm thử JVM phủ phần thuần Java của thư viện (`RepositoryCoreTest`, `FormCoreTest`, `Money`, quyết định `shouldLoad` của `LoadMoreListener`, schema ở `BookstoreSchemaTest`). Báo cáo lint nằm ở `app/build/reports/lint-results-debug.html`.

## Trạng thái và giới hạn

- Đã chạy trên thiết bị: danh sách sách, lọc, `ViewModel`/`LiveData`, `Async`, `ViewBinder`, `GenericRecyclerAdapter`.
- Chưa kiểm chứng đầy đủ trên thiết bị: thêm/sửa/xóa qua `FormBinder`, `Dialogs`, `LoadMoreListener` với dữ liệu lớn, bản release (luật giữ lớp của R8 ở `app/src/main/keepRules/rules.keep` chưa thử).
- Cơ sở dữ liệu mới cài đặt là **rỗng** (`SampleData` có sẵn dữ liệu mẫu nhưng hiện không được gọi).
- Thư viện chỉ hỗ trợ **một tiến trình**; `R.id.*` không phải hằng nên không dùng làm giá trị annotation (vì vậy `@ViewId`, `@FormField` nhận chuỗi tên id).
- Chưa làm vì BTL không cần: ngày giờ (`java.time`), ảnh, `Lifetimes.whileStarted`.

Chi tiết các quyết định thiết kế và quy ước viết tài liệu nằm trong [`CLAUDE.md`](CLAUDE.md).
