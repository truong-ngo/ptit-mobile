package vn.truongngo.android.no3;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

import vn.truongngo.android.R;
import vn.truongngo.android.no3.model.Employee;
import vn.truongngo.android.no3.model.EmployeeAdapter;
import vn.truongngo.android.no3.sql.EmployeeDbHelper;

public class EmployeeManagementMainActivity extends AppCompatActivity {

    private EditText edtSearch;
    private Spinner spSortSalary;
    private TextView tvListTitle;
    private ListView lvEmployees;

    private EmployeeDbHelper dbHelper;
    private EmployeeAdapter adapter;
    private final List<Employee> employeeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.employee_management_activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new EmployeeDbHelper(this);
        initViews();
        setupEvents();
    }

    private void initViews() {
        edtSearch = findViewById(R.id.edtSearch);
        spSortSalary = findViewById(R.id.spSortSalary);
        tvListTitle = findViewById(R.id.tvListTitle);
        lvEmployees = findViewById(R.id.lvEmployees);

        adapter = new EmployeeAdapter(this, employeeList);
        lvEmployees.setAdapter(adapter);

        Button btnOpenAdd = findViewById(R.id.btnOpenAdd);
        btnOpenAdd.setOnClickListener(view -> {
            Intent intent = new Intent(EmployeeManagementMainActivity.this, AddEditEmployeeActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Vòng đời onResume: Được gọi lại mỗi khi quay về từ màn hình Thêm/Sửa
     * Đảm bảo danh sách luôn tự động cập nhật dữ liệu mới nhất từ SQLite!
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadDataFromDb();
    }

    private void setupEvents() {
        // Search listener
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadDataFromDb();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Sort listener
        spSortSalary.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadDataFromDb();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Item click listener
        lvEmployees.setOnItemClickListener((parent, view, position, id) -> {
            Employee selectedEmp = employeeList.get(position);
            Intent intent = new Intent(EmployeeManagementMainActivity.this, AddEditEmployeeActivity.class);
            intent.putExtra("EXTRA_EMPLOYEE", selectedEmp);
            startActivity(intent);
        });
    }

    /**
     * Truy vấn trực tiếp từ SQLite theo Từ khóa tìm kiếm và Thứ tự sắp xếp lương
     */
    @SuppressLint("SetTextI18n")
    private void loadDataFromDb() {
        String keyword = edtSearch.getText().toString().trim();
        int sortOption = spSortSalary.getSelectedItemPosition();

        List<Employee> freshData = dbHelper.searchAndSortEmployees(keyword, sortOption);

        employeeList.clear();
        employeeList.addAll(freshData);
        adapter.notifyDataSetChanged();

        if (!employeeList.isEmpty()) {
            lvEmployees.setSelection(0);
        }

        tvListTitle.setText("DANH SÁCH NHÂN SỰ (" + employeeList.size() + ")");
    }
}
