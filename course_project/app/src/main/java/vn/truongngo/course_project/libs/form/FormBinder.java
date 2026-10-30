package vn.truongngo.course_project.libs.form;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import vn.truongngo.course_project.libs.form.validation.FormValidator;
import vn.truongngo.course_project.libs.form.validation.SelfValidating;
import vn.truongngo.course_project.libs.util.Assert;
import vn.truongngo.course_project.libs.util.ClassUtils;
import vn.truongngo.course_project.libs.util.ReflectionUtils;
import vn.truongngo.course_project.libs.util.ViewBinder;

/**
 * <p>Ghép một lớp model với các ô nhập trong layout theo <code>@FormField</code>: đổ dữ liệu model lên view
 * ({@link #populate}) và đọc ngược lại có kiểm tra ({@link #readInto}). Không sinh giao diện: các ô nhập đã nằm sẵn trong
 * XML, lớp này chỉ nối chúng với model. Gọi từ main thread.</p>
 *
 * <p>Lỗi hiện <b>ngay cạnh từng ô</b> (qua <code>TextInputLayout</code> nếu ô nằm trong đó, không thì
 * <code>EditText.setError</code>), tất cả cùng lúc, và con trỏ nhảy tới ô lỗi đầu tiên; lỗi trong ô tự mất khi người
 * dùng gõ tiếp. Lỗi không có chỗ báo riêng (checkbox, spinner) và lỗi của {@link SelfValidating} đi qua
 * {@link #onFormError}, mặc định là Snackbar gắn vào <code>root</code>.</p>
 *
 * <p><b>Thứ tự dùng:</b> {@link #of} (tìm view, một lần cho mỗi cây View) &rarr; {@link #choices} cho mỗi
 * <code>Spinner</code> không phải enum &rarr; {@link #populate} đổ model lên ô (mở form) &rarr; {@link #readInto} hoặc
 * {@link #read} đọc ngược lại (bấm Lưu). <code>choices</code> phải đứng trước <code>populate</code> vì
 * <code>Spinner</code> cần danh sách để biết chọn mục nào.</p>
 *
 * <p><b>Vòng đời:</b> một thể hiện gắn với đúng cây View truyền vào <code>of</code> (nó giữ tham chiếu tới các ô và tới
 * <code>root</code>), nên phải bỏ cùng View khi View bị hủy; Fragment dựng lại View thì tạo lại cả {@link FormBinder}.
 * Nó chỉ giữ liên kết giữa model và view, không giữ dữ liệu: nội dung các ô do chính các view lưu.</p>
 *
 * <pre>{@code
 * FormBinder<Book> form = FormBinder.of(Book.class, root)
 *         .choices("categoryId", categories, c -> c.id, c -> c.name, "Uncategorized");
 * form.populate(book);                       // model sang view, mở form sửa
 * if (form.readInto(book)) { save(book); }   // view sang model qua kiểm tra, bấm Lưu
 * }</pre>
 *
 * @param <M> lớp model
 */
public final class FormBinder<M> {

    /**
     * Danh sách lựa chọn của một <code>Spinner</code>: giá trị ghi vào field và chữ hiển thị, <b>hai danh sách song song
     * cùng độ dài và cùng thứ tự</b>, vì <code>Spinner</code> làm việc theo vị trí: vị trí đang chọn tra ra giá trị
     * ({@code values.get}), giá trị của field tra ra vị trí cần chọn ({@code values.indexOf}). Giá trị có thể là
     * <code>null</code> (lựa chọn "không chọn").
     */
    private record Choices(List<Object> values, List<String> labels) {}

    /** Một field của form gắn với view của nó. */
    private static final class Entry {
        /**
         * Field metadata
         * */
        final FormMeta.FieldMeta meta;

        /**
         * View gắn với field
         * */
        final View view;

        /** Chỉ dùng cho <code>Spinner</code>; null cho đến khi enum tự điền hoặc {@code choices} được gọi. */
        Choices choices;

        Entry(FormMeta.FieldMeta meta, View view) {
            this.meta = meta;
            this.view = view;
        }
    }

    /** Lớp model, dùng để tạo model mới ở {@link #read} và để ghi tên lớp vào thông báo lỗi. */
    private final Class<M> modelClass;

    /** Dùng dựng adapter cho <code>Spinner</code>; lấy từ <code>root</code> nên cùng theme với màn hình. */
    private final Context context;

    /** Các field của form theo thứ tự xử lý (<code>@FormField#order</code>), mỗi field đã gắn với view của nó. */
    private final List<Entry> entries = new ArrayList<>();

    /** Nơi nhận lỗi không gắn được vào một ô cụ thể; mặc định là Snackbar, thay được bằng {@link #onFormError}. */
    private Consumer<String> formErrorHandler;

    /** Chế độ chỉ xem chung, xem {@link #setReadOnly}. */
    private boolean readOnly;

    /**
     * Dựng bộ ghép: với mỗi field gắn <code>@FormField</code> tìm view cùng id trong <code>root</code> và chuẩn bị nó
     * ({@link #setUp}). Mọi sai sót về cấu hình (thiếu id, loại view không hợp kiểu field) lộ ngay ở đây chứ không đợi tới
     * lúc người dùng bấm Lưu. Handler lỗi mặc định hiện Snackbar gắn vào <code>root</code>.
     */
    private FormBinder(Class<M> modelClass, View root) {
        Assert.notNull(modelClass, "modelClass cannot be null");
        Assert.notNull(root, "root cannot be null");
        this.modelClass = modelClass;
        this.context = root.getContext();
        this.formErrorHandler = message -> Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();

        for (FormMeta.FieldMeta meta : FormMetaRegistry.of(modelClass)) {
            String where = modelClass.getSimpleName() + "." + meta.field().getName();
            View view = ViewBinder.find(root, meta.idName(), where);
            Entry entry = new Entry(meta, view);
            setUp(entry, where);
            entries.add(entry);
        }
    }

    /**
     * Tạo bộ ghép cho <code>modelClass</code>, tìm view theo id khai báo trong <code>@FormField</code> bên trong
     * <code>root</code>.
     *
     * @throws IllegalArgumentException nếu thiếu id hoặc view không nằm trong layout
     * @throws IllegalStateException    nếu view thuộc loại chưa hỗ trợ, hoặc không hợp với kiểu field
     */
    @NonNull
    public static <M> FormBinder<M> of(@NonNull Class<M> modelClass, @NonNull View root) {
        return new FormBinder<>(modelClass, root);
    }

    /**
     * <p>Cung cấp danh sách lựa chọn cho một <code>Spinner</code> không phải enum (spinner của enum tự có danh sách từ
     * các hằng): giá trị của lựa chọn đang chọn (ví dụ id thể loại) chính là giá trị ghi vào field, nên kiểu của nó phải
     * trùng kiểu field (field <code>Long</code> thì trả <code>Long</code>). Gọi <b>trước</b> {@link #populate}.</p>
     *
     * <p>Gọi lại được để đổi danh sách (ví dụ thể loại vừa nạp xong): mỗi lần là một adapter mới nên lựa chọn về mục đầu
     * tiên, muốn giữ lựa chọn thì gọi {@link #populate} lại sau đó.</p>
     *
     * @param noneLabel khác null thì thêm một lựa chọn đầu tiên mang giá trị <code>null</code>, ví dụ "Chưa phân loại"
     * @throws IllegalArgumentException nếu không có field đó, hoặc view của nó không phải <code>Spinner</code>
     */
    @NonNull
    public <T> FormBinder<M> choices(@NonNull String fieldName,
                                     @NonNull List<T> items,
                                     @NonNull Function<T, Object> valueOf,
                                     @NonNull Function<T, String> labelOf,
                                     @Nullable String noneLabel) {
        Entry entry = entryOf(fieldName);
        Assert.isTrue(entry.view instanceof Spinner,
                () -> modelClass.getSimpleName() + "." + fieldName + ": choices only applies to a Spinner");

        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (noneLabel != null) {
            values.add(null);
            labels.add(noneLabel);
        }
        for (T item : items) {
            values.add(valueOf.apply(item));
            labels.add(labelOf.apply(item));
        }
        entry.choices = new Choices(values, labels);
        applyAdapter((Spinner) entry.view, labels);
        return this;
    }

    /** Như bản trên nhưng không có lựa chọn "không chọn": spinner luôn đang chọn một phần tử của <code>items</code>. */
    @NonNull
    public <T> FormBinder<M> choices(@NonNull String fieldName,
                                     @NonNull List<T> items,
                                     @NonNull Function<T, Object> valueOf,
                                     @NonNull Function<T, String> labelOf) {
        return choices(fieldName, items, valueOf, labelOf, null);
    }

    /**
     * Nơi nhận lỗi không gắn được vào một ô cụ thể, thay cho Snackbar mặc định: lỗi của <code>Spinner</code> và
     * <code>CheckBox</code>/<code>Switch</code> (không có chỗ hiện lỗi riêng) và lỗi của {@link SelfValidating} (kiểm tra
     * chéo nhiều field). Lỗi của <code>EditText</code> không đi qua đây mà hiện cạnh chính ô đó.
     */
    @NonNull
    public FormBinder<M> onFormError(@NonNull Consumer<String> handler) {
        this.formErrorHandler = handler;
        return this;
    }

    /**
     * Chế độ chỉ xem: khóa mọi ô (<code>setEnabled(false)</code>), dùng khi chỉ cho xem một bản ghi. Tắt chế độ này thì
     * chỉ các ô có <code>@FormField(readOnly = true)</code> vẫn bị khóa. Có hiệu lực ngay, và được áp lại sau mỗi
     * {@link #populate}.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        applyEnabled();
    }

    /**
     * <p>Đổ dữ liệu model lên các ô (mở form, hoặc đặt lại về giá trị của model) và xóa các lỗi đang hiện. Với từng ô:</p>
     * <ul>
     *   <li><code>EditText</code>: chữ là giá trị field đổi sang chuỗi, <code>null</code> thành rỗng;</li>
     *   <li><code>CheckBox</code>/<code>Switch</code>: đánh dấu khi field là <code>true</code>;</li>
     *   <li><code>Spinner</code>: chọn mục có giá trị bằng (<code>equals</code>) giá trị field; không có mục nào khớp thì
     *       chọn mục đầu tiên.</li>
     * </ul>
     * <p>Cuối cùng áp lại trạng thái khóa ô ({@link #setReadOnly} và <code>readOnly</code> của từng field).</p>
     *
     * @throws IllegalStateException nếu một <code>Spinner</code> chưa có danh sách lựa chọn
     */
    public void populate(@NonNull M model) {
        Assert.notNull(model, "model cannot be null");
        clearErrors();
        for (Entry entry : entries) {
            Object value = entry.meta.getter().apply(model);
            View view = entry.view;
            if (view instanceof EditText) {
                ((EditText) view).setText(FormValues.format(value));
            } else if (view instanceof CompoundButton) {
                ((CompoundButton) view).setChecked(Boolean.TRUE.equals(value));
            } else {
                Choices choices = choicesOf(entry);
                int index = choices.values.indexOf(value);
                ((Spinner) view).setSelection(Math.max(index, 0));
            }
        }
        applyEnabled();
    }

    /**
     * <p>Đọc mọi ô, kiểm tra, rồi ghi vào <code>model</code> nếu tất cả hợp lệ. Làm hai pha để sai một ô thì model vẫn
     * nguyên vẹn:</p>
     * <ol>
     *   <li><b>Đọc và kiểm tra từng ô, chưa ghi gì.</b> Đổi chữ sang kiểu của field (sai định dạng, ví dụ chữ ở ô số, là
     *       lỗi), rồi chạy các annotation ràng buộc ({@link FormValidator}). Lỗi hiện ngay cạnh ô, <b>mọi</b> ô lỗi cùng
     *       lúc chứ không dừng ở ô đầu, và con trỏ nhảy tới ô lỗi đầu tiên. Ô <code>readOnly</code> bị bỏ qua hẳn.</li>
     *   <li><b>Ghi vào model</b>, chỉ khi không có ô nào lỗi. Giá trị <code>null</code> của field kiểu nguyên thủy được
     *       thay bằng giá trị mặc định của kiểu (0, <code>false</code>...) vì không gán <code>null</code> cho nguyên
     *       thủy được.</li>
     * </ol>
     *
     * <p>Sau đó chạy {@link SelfValidating} nếu model cài đặt (kiểm tra chéo nhiều field); lỗi của nó đi qua
     * {@link #onFormError}. Lúc này model <b>đã được ghi</b>, nên khi sửa một bản ghi đang hiển thị ở nơi khác hãy
     * truyền bản sao. Các field của model không có ô nhập (như <code>id</code>) không bị đụng tới.</p>
     *
     * @return <code>true</code> nếu hợp lệ và đã ghi vào model
     */
    public boolean readInto(@NonNull M model) {
        Assert.notNull(model, "model cannot be null");
        clearErrors();

        List<Object> values = new ArrayList<>(entries.size());
        Entry firstInvalid = null;

        for (Entry entry : entries) {
            if (entry.meta.readOnly()) {
                values.add(null);   // không sửa, không kiểm tra, không ghi ngược
                continue;
            }
            Object value = null;
            String error = null;
            try {
                value = read(entry);
            } catch (IllegalArgumentException e) {
                error = "Field '" + entry.meta.label() + "' has an invalid format (" + FormValues.describe(entry.meta.field().getType()) + ").";
            }
            if (error == null) {
                Optional<String> violation = FormValidator.checkField(entry.meta.field(), entry.meta.label(), value);
                error = violation.orElse(null);
            }
            if (error != null) {
                showError(entry, error);
                if (firstInvalid == null) {
                    firstInvalid = entry;
                }
            }
            values.add(value);
        }

        if (firstInvalid != null) {
            firstInvalid.view.requestFocus();
            return false;
        }

        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry.meta.readOnly()) {
                continue;
            }
            Object value = values.get(i);
            Class<?> type = entry.meta.field().getType();
            if (value == null && type.isPrimitive()) {
                value = ClassUtils.getDefaultValue(type);
            }
            entry.meta.setter().accept(model, value);
        }

        if (model instanceof SelfValidating) {
            try {
                ((SelfValidating) model).validate();
            } catch (IllegalArgumentException e) {
                formErrorHandler.accept(e.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Tạo model mới bằng constructor không tham số rồi {@link #readInto}. Tiện khi thêm mới, nhưng model chỉ có các
     * field có ô nhập: field không có ô (như <code>id</code> của bản ghi đang sửa) <b>không</b> được mang theo. Khi sửa
     * hãy dùng {@link #readInto} với model đã nạp.</p>
     *
     * @return model đã điền, hoặc <code>null</code> nếu có lỗi (lỗi đã hiện cạnh các ô, người gọi chỉ việc dừng lại)
     */
    @Nullable
    public M read() {
        M model = ReflectionUtils.instantiateClass(modelClass);
        if (model == null) return null;
        return readInto(model) ? model : null;
    }

    /** Xóa mọi lỗi đang hiện cạnh các ô. {@link #populate} và {@link #readInto} tự gọi hàm này trước khi làm việc. */
    public void clearErrors() {
        for (Entry entry : entries) {
            setError(entry, null);
        }
    }

    /**
     * <p>Chuẩn bị view của một field theo loại view, và kiểm tra view hợp với kiểu field:</p>
     * <ul>
     *   <li><code>EditText</code>: gắn bộ theo dõi chữ để lỗi đang hiện cạnh ô <b>tự mất khi người dùng gõ tiếp</b>;</li>
     *   <li><code>CheckBox</code>/<code>Switch</code>: chỉ hợp với field boolean;</li>
     *   <li><code>Spinner</code>: field là enum thì tự có danh sách từ các hằng (chữ là <code>toString</code> của hằng,
     *       nên enum nên override nó để hiện nhãn tiếng Việt); kiểu khác phải gọi {@link #choices};</li>
     *   <li>loại view khác: không hỗ trợ.</li>
     * </ul>
     *
     * @throws IllegalStateException nếu view thuộc loại chưa hỗ trợ, hoặc <code>CheckBox</code>/<code>Switch</code> gắn với field không phải boolean
     */
    private void setUp(Entry entry, String where) {
        View view = entry.view;
        Class<?> type = ClassUtils.resolveWrapperIfNecessary(entry.meta.field().getType());

        if (view instanceof EditText) {
            ((EditText) view).addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    setError(entry, null);
                }
            });
        } else if (view instanceof CompoundButton) {
            if (type != Boolean.class) {
                throw new IllegalStateException(where + ": CheckBox/Switch only fits a boolean field");
            }
        } else if (view instanceof Spinner) {
            if (type.isEnum()) {
                List<Object> values = new ArrayList<>(Arrays.asList(Objects.requireNonNull(type.getEnumConstants())));
                List<String> labels = new ArrayList<>();
                for (Object constant : values) {
                    labels.add(String.valueOf(constant));
                }
                entry.choices = new Choices(values, labels);
                applyAdapter((Spinner) view, labels);
            }
        } else {
            throw new IllegalStateException(where + ": view '" + entry.meta.idName() + "' of type "
                    + view.getClass().getSimpleName() + " is not supported (use EditText, CheckBox/Switch or Spinner)");
        }
    }

    /** Gắn danh sách chữ vào một <code>Spinner</code> bằng layout mặc định của hệ thống. */
    private void applyAdapter(Spinner spinner, List<String> labels) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    /** Khóa hoặc mở từng ô theo chế độ chỉ xem chung và cờ <code>readOnly</code> của field. */
    private void applyEnabled() {
        for (Entry entry : entries) {
            entry.view.setEnabled(!readOnly && !entry.meta.readOnly());
        }
    }

    /**
     * Đọc giá trị của một field từ view: <code>EditText</code> qua {@link FormValues#parse} (chữ rỗng hoặc toàn khoảng
     * trắng cho <code>null</code> để <code>@NotNull</code> bắt lỗi "bắt buộc nhập"), <code>CheckBox</code>/<code>Switch</code>
     * cho đúng/sai, <code>Spinner</code> cho giá trị của mục đang chọn (<code>null</code> nếu không có mục nào được chọn).
     *
     * @throws IllegalArgumentException nếu chữ trong <code>EditText</code> không đúng định dạng của kiểu field
     * @throws IllegalStateException    nếu <code>Spinner</code> chưa có danh sách lựa chọn
     */
    private Object read(Entry entry) {
        View view = entry.view;
        if (view instanceof EditText) {
            return FormValues.parse(((EditText) view).getText().toString(), entry.meta.field().getType());
        }
        if (view instanceof CompoundButton) {
            return ((CompoundButton) view).isChecked();
        }
        Choices choices = choicesOf(entry);
        int position = ((Spinner) view).getSelectedItemPosition();
        return position >= 0 && position < choices.values.size() ? choices.values.get(position) : null;
    }

    /**
     * Danh sách lựa chọn của một <code>Spinner</code>.
     *
     * @throws IllegalStateException nếu chưa có (field không phải enum và chưa gọi {@link #choices})
     */
    private Choices choicesOf(Entry entry) {
        if (entry.choices == null) {
            throw new IllegalStateException(modelClass.getSimpleName() + "." + entry.meta.field().getName()
                    + ": the Spinner has no choices yet, call FormBinder.choices(...) before populate");
        }
        return entry.choices;
    }

    /**
     * Tìm field của form theo tên field Java.
     *
     * @throws IllegalArgumentException nếu không có field đó gắn <code>@FormField</code>
     */
    private Entry entryOf(String fieldName) {
        for (Entry entry : entries) {
            if (entry.meta.field().getName().equals(fieldName)) {
                return entry;
            }
        }
        throw new IllegalArgumentException("No field '" + fieldName + "' annotated with @FormField on " + modelClass.getSimpleName());
    }

    /**
     * Quyết định chỗ hiện một lỗi: <code>EditText</code> có chỗ báo riêng ngay cạnh ô ({@link #setError}), còn
     * <code>Spinner</code> và <code>CheckBox</code>/<code>Switch</code> thì không nên chuyển cho {@link #onFormError}.
     */
    private void showError(Entry entry, String message) {
        if (entry.view instanceof EditText) {
            setError(entry, message);
        } else {
            formErrorHandler.accept(message);
        }
    }

    /**
     * Đặt hoặc xóa (<code>null</code>) lỗi cạnh ô, qua <code>TextInputLayout</code> nếu ô nằm trong đó. Chỉ tìm tối đa hai
     * cấp cha, vì <code>TextInputLayout</code> bọc <code>EditText</code> qua một khung trung gian.
     */
    private void setError(Entry entry, @Nullable String message) {
        if (!(entry.view instanceof EditText)) {
            return;
        }
        ViewParent parent = entry.view.getParent();
        for (int depth = 0; depth < 2 && parent != null; depth++) {
            if (parent instanceof TextInputLayout) {
                ((TextInputLayout) parent).setError(message);
                return;
            }
            parent = parent.getParent();
        }
        ((EditText) entry.view).setError(message);
    }
}
