package com.example.androidexample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.androidexample.utils.ClassUi;
import com.example.androidexample.utils.ClassesApi;

import org.json.JSONArray;
import org.json.JSONObject;

public class ClassesPageActivity extends AppCompatActivity {
    private TextView home, groups, create_schedule, profile;
    private GridLayout classDisplay;
    private EditText search;

    // keep all courses here
    private final java.util.ArrayList<JSONObject> allCourses = new java.util.ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classes_page);

        home = findViewById(R.id.home_btn);
        groups = findViewById(R.id.groups_btn);
        create_schedule = findViewById(R.id.create_schedule);
        profile = findViewById(R.id.profile_btn);
        classDisplay = findViewById(R.id.classDisplay);

        search = findViewById(R.id.search_edit_text);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable editable) {
                filterAndRender(editable.toString());
            }
        });

        profile.setText(getIntent().getStringExtra("username"));
        profile.setOnClickListener(view -> {
            Intent intent = new Intent(this, MenuProfileActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        home.setOnClickListener(view -> {
            Intent intent = new Intent(this, NewHomeActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        groups.setOnClickListener(view -> {
            Intent intent = new Intent(this, GroupMenuActivity01.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        create_schedule = findViewById(R.id.create_schedule);
        create_schedule.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateScheduleActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });


        TextView clubs = findViewById(R.id.clubs);
        clubs.setOnClickListener(view -> {
            Intent intent = new Intent(ClassesPageActivity.this, ClubsMenuActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        fetchAllCourses();
    }

    private void fetchAllCourses() {
        ClassesApi.getAllCourses(
                this,
                coursesList -> {
                    allCourses.clear();
                    allCourses.addAll(coursesList);
                    // initial render: no filter
                    filterAndRender("");
                },
                error -> {
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null) {
                        Log.e("VOLLEY", "GET err " + nr.statusCode +
                                " body=" + new String(nr.data));
                        Toast.makeText(this,
                                "Load failed: " + nr.statusCode,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("VOLLEY", "GET err " + error);
                        Toast.makeText(this,
                                "Network error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    private void filterAndRender(String filter) {
        classDisplay.removeAllViews();

        java.util.List<JSONObject> filtered = new java.util.ArrayList<>();

        for (JSONObject obj : allCourses) {
            String courseId = obj.optString("courseId", "");
            if (filter == null || filter.isEmpty()
                    || courseId.toLowerCase().contains(filter.toLowerCase())) {
                filtered.add(obj);
            }
        }

        ClassUi.renderCourseGrid(
                this,
                classDisplay,
                filtered,
                course -> {
                    String courseId = course.optString("courseId");
                    Intent intent = new Intent(this, CourseRatingActivity.class);
                    intent.putExtra("courseId", courseId);
                    intent.putExtra("username", getIntent().getStringExtra("username"));
                    startActivity(intent);
                }
        );
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
