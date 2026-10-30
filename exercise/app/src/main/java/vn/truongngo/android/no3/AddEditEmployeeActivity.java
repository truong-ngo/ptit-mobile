package vn.truongngo.android.no3;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

import vn.truongngo.android.R;
import vn.truongngo.android.no3.model.Employee;
import vn.truongngo.android.no3.sql.EmployeeDbHelper;

public class AddEditEmployeeActivity extends AppCompatActivity {

    private ImageView imgEmployeeAvatar;
    private TextView tvFormTitle;
    private EditText edtEmpCode, edtEmpName, edtBirthYear, edtHometown, edtSalary;
    private RadioButton rbMale, rbFemale;
    private Spinner spEducationLevel, spDepartment;
    private Button btnSave, btnDelete;

    private EmployeeDbHelper dbHelper;
    private Employee editingEmployee = null; 
    private String selectedAvatarUriString = "";

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imgEmployeeAvatar.setImageURI(uri);
                    selectedAvatarUriString = uri.toString();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_employee);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new EmployeeDbHelper(this);
        initViews();
        checkEditMode();
        setupEvents();
    }

    private void initViews() {
        tvFormTitle = findViewById(R.id.tvFormTitle);
        imgEmployeeAvatar = findViewById(R.id.imgEmployeeAvatar);
        edtEmpCode = findViewById(R.id.edtEmpCode);
        edtEmpName = findViewById(R.id.edtEmpName);
        edtBirthYear = findViewById(R.id.edtBirthYear);
        edtHometown = findViewById(R.id.edtHometown);
        edtSalary = findViewById(R.id.edtSalary);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        spEducationLevel = findViewById(R.id.spEducationLevel);
        spDepartment = findViewById(R.id.spDepartment);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private void checkEditMode() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_EMPLOYEE")) {
            editingEmployee = (Employee) intent.getSerializableExtra("EXTRA_EMPLOYEE");
        }

        if (editingEmployee != null) {
            tvFormTitle.setText(R.string.edit_employee);
            btnSave.setText(R.string.btn_update);
            btnDelete.setVisibility(View.VISIBLE);

            edtEmpCode.setText(editingEmployee.getEmployeeCode());
            edtEmpCode.setEnabled(false); 
            edtEmpName.setText(editingEmployee.getFullName());
            edtBirthYear.setText(String.valueOf(editingEmployee.getBirthYear()));
            edtHometown.setText(editingEmployee.getHometown());
            edtSalary.setText(String.valueOf((long) editingEmployee.getSalary()));

            if ("Nữ".equalsIgnoreCase(editingEmployee.getGender())) {
                rbFemale.setChecked(true);
            } else {
                rbMale.setChecked(true);
            }

            setSpinnerSelection(spEducationLevel, editingEmployee.getEducationLevel());
            setSpinnerSelection(spDepartment, editingEmployee.getDepartment());

            selectedAvatarUriString = editingEmployee.getAvatar() != null ? editingEmployee.getAvatar() : "";
            if (!selectedAvatarUriString.isEmpty()) {
                if ("anh_the".equals(selectedAvatarUriString)) {
                    imgEmployeeAvatar.setImageResource(R.drawable.anh_the);
                } else {
                    try {
                        imgEmployeeAvatar.setImageURI(Uri.parse(selectedAvatarUriString));
                    } catch (Exception e) {
                        imgEmployeeAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                }
            }
        } else {
            tvFormTitle.setText(R.string.add_employee);
            btnSave.setText(R.string.btn_save);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void setupEvents() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View.OnClickListener choosePhotoListener = v -> pickImageLauncher.launch("image/*");
        findViewById(R.id.cardAvatarWrapper).setOnClickListener(choosePhotoListener);
        findViewById(R.id.tvChangeAvatar).setOnClickListener(choosePhotoListener);

        btnSave.setOnClickListener(v -> handleSave());
        btnDelete.setOnClickListener(v -> handleDelete());
    }

    private void handleSave() {
        String employeeCode = edtEmpCode.getText().toString().trim();
        String fullName = edtEmpName.getText().toString().trim();
        String birthYearStr = edtBirthYear.getText().toString().trim();
        String hometown = edtHometown.getText().toString().trim();
        String salaryStr = edtSalary.getText().toString().trim();
        String gender = rbMale.isChecked() ? "Nam" : "Nữ";
        String educationLevel = spEducationLevel.getSelectedItem() != null ? spEducationLevel.getSelectedItem().toString() : "";
        String department = spDepartment.getSelectedItem() != null ? spDepartment.getSelectedItem().toString() : "";

        if (employeeCode.isEmpty() || fullName.isEmpty() || salaryStr.isEmpty()) {
            showSnackbar("Vui lòng nhập đủ: Mã, Tên và Mức lương!");
            return;
        }

        int birthYear = birthYearStr.isEmpty() ? 0 : Integer.parseInt(birthYearStr);
        double salary = Double.parseDouble(salaryStr);

        if (editingEmployee == null) {
            if (dbHelper.checkEmployeeExists(employeeCode)) {
                showSnackbar("Mã nhân sự '" + employeeCode + "' đã tồn tại! Vui lòng chọn mã khác.");
                return;
            }

            Employee newEmployee = new Employee(employeeCode, fullName, birthYear, gender, hometown, educationLevel, selectedAvatarUriString, salary, department);
            long result = dbHelper.addEmployee(newEmployee);
            if (result > 0) {
                showSnackbarAndFinish("Thêm nhân sự thành công!");
            } else {
                showSnackbar("Thêm thất bại, vui lòng thử lại!");
            }
        } else {
            Employee updatedEmployee = new Employee(employeeCode, fullName, birthYear, gender, hometown, educationLevel, selectedAvatarUriString, salary, department);
            int rows = dbHelper.update(updatedEmployee);
            if (rows > 0) {
                showSnackbarAndFinish("Cập nhật nhân sự thành công!");
            } else {
                showSnackbar("Cập nhật thất bại!");
            }
        }
    }

    private void handleDelete() {
        if (editingEmployee == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa nhân sự " + editingEmployee.getFullName() + " (" + editingEmployee.getEmployeeCode() + ") không?")
                .setPositiveButton("XÓA", (dialog, which) -> {
                    int rows = dbHelper.delete(editingEmployee.getEmployeeCode());
                    if (rows > 0) {
                        showSnackbarAndFinish("Đã xóa nhân sự!");
                    } else {
                        showSnackbar("Xóa thất bại!");
                    }
                })
                .setNegativeButton("HỦY", null)
                .show();
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT).show();
    }

    private void showSnackbarAndFinish(String message) {
        View root = findViewById(R.id.main);
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show();
        root.postDelayed(this::finish, 800);
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}
