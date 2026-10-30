package vn.truongngo.android.no3.sql;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import vn.truongngo.android.no3.model.Employee;

public final class EmployeeDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "employee_list";
    private static final String TABLE_NAME = "employee";
    private static final String EMPLOYEE_CODE = "employee_code";
    private static final String FULL_NAME = "full_name";
    private static final String BIRTH_YEAR = "birth_year";
    private static final String GENDER = "gender";
    private static final String HOMETOWN = "hometown";
    private static final String EDUCATION_LEVEL = "education_level";
    private static final String AVATAR = "avatar";
    private static final String SALARY = "salary";
    private static final String DEPARTMENT = "department";

    private final Context context;

    public EmployeeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        Log.d("DB Manager", "DB Manager");
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                EMPLOYEE_CODE + " TEXT PRIMARY KEY, " +
                FULL_NAME + " TEXT, " +
                BIRTH_YEAR + " INTEGER, " +
                GENDER + " TEXT, " +
                HOMETOWN + " TEXT, " +
                EDUCATION_LEVEL + " TEXT, " +
                AVATAR + " TEXT, " +
                SALARY + " REAL, " +
                DEPARTMENT + " TEXT)";
        db.execSQL(sqlQuery);
        Toast.makeText(context, "Create Database successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
        Toast.makeText(context, "Drop successfully", Toast.LENGTH_SHORT).show();
    }

    public boolean checkEmployeeExists(String employeeCode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + EMPLOYEE_CODE + " = ?", new String[]{employeeCode});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public long addEmployee(Employee employee) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(EMPLOYEE_CODE, employee.getEmployeeCode());
        values.put(FULL_NAME, employee.getFullName());
        values.put(BIRTH_YEAR, employee.getBirthYear());
        values.put(GENDER, employee.getGender());
        values.put(HOMETOWN, employee.getHometown());
        values.put(EDUCATION_LEVEL, employee.getEducationLevel());
        values.put(AVATAR, employee.getAvatar());
        values.put(SALARY, employee.getSalary());
        values.put(DEPARTMENT, employee.getDepartment());

        return db.insert(TABLE_NAME, null, values);
    }

    public int update(Employee employee) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(FULL_NAME, employee.getFullName());
        values.put(BIRTH_YEAR, employee.getBirthYear());
        values.put(GENDER, employee.getGender());
        values.put(HOMETOWN, employee.getHometown());
        values.put(EDUCATION_LEVEL, employee.getEducationLevel());
        values.put(AVATAR, employee.getAvatar());
        values.put(SALARY, employee.getSalary());
        values.put(DEPARTMENT, employee.getDepartment());

        return db.update(TABLE_NAME, values, EMPLOYEE_CODE + " = ?", new String[]{employee.getEmployeeCode()});
    }

    public int delete(String employeeCode) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, EMPLOYEE_CODE + " = ?", new String[]{employeeCode});
    }

    public List<Employee> getAllEmployee() {
        List<Employee> listEmployee = new ArrayList<Employee>();
        String selectQuery = "SELECT * FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Employee employee = new Employee();
                employee.setEmployeeCode(cursor.getString(0));
                employee.setFullName(cursor.getString(1));
                employee.setBirthYear(cursor.getInt(2));
                employee.setGender(cursor.getString(3));
                employee.setHometown(cursor.getString(4));
                employee.setEducationLevel(cursor.getString(5));
                employee.setAvatar(cursor.getString(6));
                employee.setSalary(cursor.getDouble(7));
                employee.setDepartment(cursor.getString(8));
                listEmployee.add(employee);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return listEmployee;
    }

    public List<Employee> searchAndSortEmployees(String keyword, int sortOption) {
        List<Employee> listEmployee = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + TABLE_NAME);
        List<String> argsList = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            queryBuilder.append(" WHERE ").append(FULL_NAME).append(" LIKE ? OR ").append(EMPLOYEE_CODE).append(" LIKE ?");
            String wildCard = "%" + keyword.trim() + "%";
            argsList.add(wildCard);
            argsList.add(wildCard);
        }

        if (sortOption == 1) {
            queryBuilder.append(" ORDER BY ").append(SALARY).append(" ASC");
        } else if (sortOption == 2) {
            queryBuilder.append(" ORDER BY ").append(SALARY).append(" DESC");
        } else {
            queryBuilder.append(" ORDER BY ").append(EMPLOYEE_CODE).append(" ASC");
        }

        String[] selectionArgs = argsList.isEmpty() ? null : argsList.toArray(new String[0]);
        Cursor cursor = db.rawQuery(queryBuilder.toString(), selectionArgs);

        if (cursor.moveToFirst()) {
            do {
                Employee employee = new Employee();
                employee.setEmployeeCode(cursor.getString(0));
                employee.setFullName(cursor.getString(1));
                employee.setBirthYear(cursor.getInt(2));
                employee.setGender(cursor.getString(3));
                employee.setHometown(cursor.getString(4));
                employee.setEducationLevel(cursor.getString(5));
                employee.setAvatar(cursor.getString(6));
                employee.setSalary(cursor.getDouble(7));
                employee.setDepartment(cursor.getString(8));
                listEmployee.add(employee);
            } while (cursor.moveToNext());
            cursor.close();
        }

        return listEmployee;
    }

    public int getEmployeesCount() {
        String countQuery = "SELECT * FROM " + TABLE_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}
