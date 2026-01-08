package com.example.androidexample;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClassEnrollActivity extends AppCompatActivity {
    private boolean panelVisible = false;
    private View bottomPanel;
    private float hiddenTranslation;
    private GridLayout classDisplay;
    private LinearLayout selectedClassesDisplay;
    private ArrayList<String> selectedClasses = new ArrayList<>();
    private EditText search;
    private TextView finish;

    /**
     * Outputs selected classes by the user for enrolled
     */
    private void updateSelectedClasses() {
        selectedClassesDisplay.removeAllViews();

        if (selectedClasses.size() == 0) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.gold_border);
            int padding = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            row.setPadding(padding, padding, padding, padding);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int marginBottom = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            rowParams.setMargins(0, 0, 0, marginBottom);
            row.setLayoutParams(rowParams);

            TextView name = new TextView(this);
            name.setText("No classes Selected");
            name.setTextColor(Color.parseColor("#F1BE48"));
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(nameParams);

            row.addView(name);
            selectedClassesDisplay.addView(row);
        }

        for (String className : selectedClasses) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.gold_border);
            int padding = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            row.setPadding(padding, padding, padding, padding);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int marginBottom = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
            rowParams.setMargins(0, 0, 0, marginBottom);
            row.setLayoutParams(rowParams);

            TextView name = new TextView(this);
            name.setText(className);
            name.setTextColor(Color.parseColor("#F1BE48"));
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(nameParams);

            TextView removeBtn = new TextView(this);
            removeBtn.setText("Remove");
            removeBtn.setGravity(Gravity.CENTER);
            removeBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
            removeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            removeBtn.setBackgroundResource(R.drawable.rounded_textview);
            int btnPadding = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());
            removeBtn.setPadding(btnPadding * 2, btnPadding, btnPadding * 2, btnPadding);

            removeBtn.setOnClickListener(v -> {
                selectedClasses.remove(className);
                updateSelectedClasses();
            });

            row.addView(name);
            row.addView(removeBtn);
            selectedClassesDisplay.addView(row);
        }
    }

    /**
     * Fetches classes from backend
     * @param filter for searching for classes by user
     */
    private void getClasses(String filter) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/courses";
        System.out.println(url);
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("VOLLEY", "GET ok (" + response.length() + "): " + response);
                    outputClasses(classDisplay, response, filter);
                },
                error -> {
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null) {
                        Log.e("VOLLEY", "GET err " + nr.statusCode + " body=" + new String(nr.data));
                        Toast.makeText(this, "Load failed: " + nr.statusCode, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("VOLLEY", "GET err " + error);
                        Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
        request.setShouldCache(false);
        queue.getCache().clear();
        queue.add(request);
    }

    /**
     * Outputs all classes available for selection for enrollment
     * @param grid container for outputting classes
     * @param response from getClasses which iterates through the array outputting classes
     * @param filter
     */
    private void outputClasses(GridLayout grid, JSONArray response, String filter) {
        grid.removeAllViews();

        int columnCount = grid.getColumnCount();
        int displayedIndex = 0;

        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject obj = response.getJSONObject(i);
                String courseId = obj.getString("courseId");

                if (filter != null && !filter.isEmpty() && !courseId.toLowerCase().contains(filter.toLowerCase())) {
                    continue;
                }

                final TextView tv = new TextView(this);
                tv.setText(courseId);
                tv.setGravity(Gravity.CENTER);
                tv.setTextColor(ContextCompat.getColor(this, R.color.white));
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                tv.setBackgroundResource(R.drawable.class_enroll);

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
                params.setMargins(5, 5, 5, 5);
                params.columnSpec = GridLayout.spec(displayedIndex % columnCount, 1f);
                params.rowSpec = GridLayout.spec(displayedIndex / columnCount);
                tv.setLayoutParams(params);

                // Handle click
                tv.setOnClickListener(view -> {
                    String className = tv.getText().toString();
                    if (!selectedClasses.contains(className)) {
                        selectedClasses.add(className);
                        updateSelectedClasses();
                    }
                });

                grid.addView(tv);
                displayedIndex++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * function runs when page is loaded, giving button properties to the various necessary TextViews of the page
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_classes);

        bottomPanel = findViewById(R.id.selected_classes_panel);
        selectedClassesDisplay = findViewById(R.id.selected_classes_container);
        classDisplay = findViewById(R.id.classDisplay);

        bottomPanel.post(() -> {
            float peekHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    80,
                    getResources().getDisplayMetrics()
            );

            hiddenTranslation = bottomPanel.getHeight() - peekHeight;
            bottomPanel.setTranslationY(hiddenTranslation);
        });

        bottomPanel.setOnClickListener(v -> togglePanel());
        getClasses("");

        search = findViewById(R.id.search_edit_text);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                getClasses(editable.toString());
            }
        });

        finish = findViewById(R.id.finish_btn);
        finish.setOnClickListener(view -> {
            postEnrolledClasses();
            Intent intent = new Intent(this, NewHomeActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        updateSelectedClasses();
    }

    /**
     * When user is done selecting classes, enrolled class list is posted to the backend so that it can be outputted when user logs in
     */
    private void postEnrolledClasses() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String netId = getIntent().getStringExtra("username");
        JSONArray body = new JSONArray(selectedClasses);
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/users/" + getIntent().getStringExtra("username") + "/enrolled";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    Log.d("VOLLEY", "POST ok: " + response);
                    Toast.makeText(this, "Enrollment updated!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null) {
                        Log.e("VOLLEY", "POST err " + nr.statusCode + " body=" + new String(nr.data));
                        Toast.makeText(this, "Post failed: " + nr.statusCode, Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("VOLLEY", "POST err " + error);
                        Toast.makeText(this, "Network error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(request);
    }

    /**
     * Closes keyboard when user clicks off of it
     * @param ev
     * @return
     */
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void togglePanel() {
        float targetY = panelVisible ? hiddenTranslation : 0f;
        bottomPanel.animate()
                .translationY(targetY)
                .setDuration(300)
                .start();
        panelVisible = !panelVisible;
    }
}