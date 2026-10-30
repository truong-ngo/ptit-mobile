package vn.truongngo.android.no3.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;

import vn.truongngo.android.R;

public class EmployeeAdapter extends BaseAdapter {

    private final Context context;
    private List<Employee> employeeList;
    private final LayoutInflater inflater;
    private final DecimalFormat salaryFormatter = new DecimalFormat("#,### đ");

    public EmployeeAdapter(Context context, List<Employee> employeeList) {
        this.context = context;
        this.employeeList = employeeList;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<Employee> newList) {
        this.employeeList = newList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return employeeList != null ? employeeList.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return employeeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    private static class ViewHolder {
        ImageView imgAvatar;
        TextView tvNameAndCode;
        TextView tvDepartmentAndHometown;
        TextView tvLevelAndBirth;
        TextView tvSalary;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_employee, parent, false);
            holder = new ViewHolder();
            holder.imgAvatar = convertView.findViewById(R.id.itemImgAvatar);
            holder.tvNameAndCode = convertView.findViewById(R.id.itemTvNameAndCode);
            holder.tvDepartmentAndHometown = convertView.findViewById(R.id.itemTvDepartmentAndHometown);
            holder.tvLevelAndBirth = convertView.findViewById(R.id.itemTvLevelAndBirth);
            holder.tvSalary = convertView.findViewById(R.id.itemTvSalary);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Employee employee = employeeList.get(position);

        holder.tvNameAndCode.setText(employee.getFullName() + " - " + employee.getEmployeeCode());
        holder.tvDepartmentAndHometown.setText("Phòng: " + employee.getDepartment() + " • Quê: " + employee.getHometown());
        holder.tvLevelAndBirth.setText(employee.getEducationLevel() + " • Sinh năm: " + employee.getBirthYear());
        holder.tvSalary.setText(salaryFormatter.format(employee.getSalary()).replace(',', '.'));

        String avatar = employee.getAvatar();
        if (avatar != null && !avatar.trim().isEmpty()) {
            if (avatar.startsWith("content://") || avatar.startsWith("file://")) {
                try {
                    holder.imgAvatar.setImageURI(Uri.parse(avatar));
                } catch (Exception e) {
                    holder.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            } else {
                int resId = context.getResources().getIdentifier(avatar, "drawable", context.getPackageName());
                if (resId != 0) {
                    holder.imgAvatar.setImageResource(resId);
                } else {
                    holder.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                }
            }
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        return convertView;
    }
}
