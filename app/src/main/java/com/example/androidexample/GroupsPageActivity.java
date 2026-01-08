package com.example.androidexample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GroupsPageActivity extends AppCompatActivity {
    private TextView classes, home, create_schedule, profile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_page);

        classes = findViewById(R.id.classes_btn);
        home = findViewById(R.id.home_btn);
        create_schedule = findViewById(R.id.create_schedule);
        profile = findViewById(R.id.profile_btn);
        profile.setText(getIntent().getStringExtra("username"));
        profile.setOnClickListener(view -> {
            Intent intent = new Intent(this, MenuProfileActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        classes.setOnClickListener(view -> {
            Intent intent = new Intent(this, ClassesPageActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        home.setOnClickListener(view -> {
            Intent intent = new Intent(this, NewHomeActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        create_schedule.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateScheduleActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view instanceof EditText) {
            Rect outRect = new Rect();
            view.getGlobalVisibleRect(outRect);

            // If the user taps outside the EditText
            if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                view.clearFocus();
                hideKeyboard(view);

                // If you have a custom dialog or overlay, hide it here:
                // findViewById(R.id.searchDialogLayout).setVisibility(View.GONE);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}