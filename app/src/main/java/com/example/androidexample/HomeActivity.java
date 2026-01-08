package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.widget.Button;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class HomeActivity extends AppCompatActivity {
    private RelativeLayout page;
    private GridLayout classParent, groupParent, messagesParent;
    private TextView profile;
    private Button logOut;
   // private Button logOut;
    private void addCells(GridLayout parent, String prefix, int count) {
        int pad = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        int minH = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());

        for (int i = 1; i <= count; i++) {
            final int index = i;
            TextView cell = new TextView(this);
            cell.setText(prefix + " " + i);
            cell.setTextColor(ContextCompat.getColor(this, R.color.white));
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            cell.setPadding(pad, pad, pad, pad);
            cell.setMinHeight(minH);

            cell.setBackground(ContextCompat.getDrawable(this, R.drawable.rounded_button));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // needed for weight
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(margin, margin, margin, margin);
            cell.setLayoutParams(params);

            cell.setOnClickListener(v -> {
                if (prefix.equals("Class")) {
                    Intent intent = new Intent(this, ClassActivity.class);
                    intent.putExtra("type", prefix);
                    intent.putExtra("index", index);
                    intent.putExtra("username", getIntent().getStringExtra("username"));
                    startActivity(intent);
                }
                else if (prefix.equals("Group")) {
                    Intent intent = new Intent(this, GroupMenuActivity.class);
                    intent.putExtra("type", prefix);
                    intent.putExtra("index", index);
                    intent.putExtra("username", getIntent().getStringExtra("username"));
                    startActivity(intent);
                }
            });

            parent.addView(cell);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        page = findViewById(R.id.home_page);
        classParent = findViewById(R.id.classDisplay);
        groupParent = findViewById(R.id.groupDisplay);
        messagesParent = findViewById(R.id.messagesDisplay);
        profile = findViewById(R.id.profile);
        logOut = findViewById(R.id.logOut);


        // Outputs all classes the user is enrolled in
        addCells(classParent, "Class", 6);

        // Outputs all groups the user is a part of
        addCells(groupParent, "Group", 3);

        // Outputs all direct messages that the user has participated in
        addCells(messagesParent, "Message", 3);

        page.setOnClickListener(v -> {
            View current = getCurrentFocus();
            if (current != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
                }
                current.clearFocus();
            }
        });

        String username = getIntent().getStringExtra("username");
        if (username == null) {
            username = "Test_User";
        }
        final String finalUsername = username;
        profile.setText(finalUsername);
        profile.setOnClickListener(view -> {
            Intent intent = new Intent(this, MenuProfileActivity.class);
            intent.putExtra("username", finalUsername);
            startActivity(intent);
        });

        logOut.setOnClickListener(view -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }
}

