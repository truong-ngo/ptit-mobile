package vn.truongngo.android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

public class WelcomeActivity extends AppCompatActivity {

    final String[] friendsList = {
            "Nguyễn Văn Nam",
            "Trần Thị Mai",
            "Lê Hoàng Long",
            "Phạm Thu Hà",
            "Đỗ Minh Quân"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.welcome_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        String username = intent.getStringExtra("USER_NAME");
        TextView textView = findViewById(R.id.greeting);
        textView.setText("Xin chào: " + username);

        ListView listFriends = findViewById(R.id.listFriendTable);
        ArrayAdapter<String> contents = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                friendsList);
        listFriends.setAdapter(contents);

        listFriends.setOnItemClickListener(((parent, view, position, id) -> {
            String friendName = friendsList[position];
            Snackbar.make(listFriends, "Xin chào bạn: " + friendName, Toast.LENGTH_SHORT).show();
        }));
    }
}