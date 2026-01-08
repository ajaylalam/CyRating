package com.example.androidexample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MockScheduleActivity extends AppCompatActivity {
    private LinearLayout classList, addedClassesList;
    private ArrayList<Object> MWFlist = new ArrayList<>();
    private ArrayList<Object> TRlist = new ArrayList<>();
    private ArrayList<String> namesAdded = new ArrayList<>();
    private EditText search, name;
    private TextView save, profile, back, delete;


    /**
     * function gets called on page load
     * defines the different buttons on the page and where they should route to alongside what to pass to the different screens (username)
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mock_schedule_page);

        classList = findViewById(R.id.class_list_container);
        addedClassesList = findViewById(R.id.added_classes_container);
        search = findViewById(R.id.search_input);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getClasses(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        name = findViewById(R.id.name_input);

        save = findViewById(R.id.save_exit_btn);
        save.setOnClickListener(view -> {
            try {
                saveScheduleBackend();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        profile = findViewById(R.id.profile_btn);
        profile.setText(getIntent().getStringExtra("username"));
        profile.setOnClickListener(view -> {
            Intent intent = new Intent(this, MenuProfileActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        back = findViewById(R.id.return_btn);
        back.setOnClickListener(view -> {
            Intent intent = new Intent(this, CreateScheduleActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        delete = findViewById(R.id.delete_btn);
        delete.setOnClickListener(view -> {
            deleteSchedule();
        });

        long id = getIntent().getLongExtra("id", -2);
        if (id >= 0) {
            previouslySavedSchedule(id);
        }

        getClasses("");
    }

    /**
     * If user wishes to delete schedule, when that button is clicked, this function is called.
     * The schedule is deleted from the backend and routed back to the CreateSchedule page which displays all the previously created schedules.
     */
    private void deleteSchedule() {
        long id = getIntent().getLongExtra("id", -2);
        RequestQueue queue = Volley.newRequestQueue(this);
        if (id >= 0) {
            String deleteUrl = "http://coms-3090-019.class.las.iastate.edu:8080/mock-schedules/" + id;
            StringRequest deleteRequest = new StringRequest(
                    Request.Method.DELETE,
                    deleteUrl,
                    response -> {
                        Toast.makeText(this, "Old schedule deleted.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, CreateScheduleActivity.class);
                        intent.putExtra("username", getIntent().getStringExtra("username"));
                        startActivity(intent);
                    },
                    error -> {
                        Toast.makeText(this, "Delete failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("Schedule", "Delete error: ", error);
                    }
            );
            queue.add(deleteRequest);
        } else {
            Toast.makeText(this, "No schedule saved.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Function outputs the selected classes to the calendar in a neat manner
     * Without this function, classes are outputted inconsistenly and irregularly and doesn't look as good
     * @param id
     */
    private void previouslySavedSchedule(long id) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/mock-schedules/" + id;
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("VOLLEY", "GET ok (" + response.length() + "): " + response);
//                    Toast.makeText(this, "Loaded " + response.length() + " classes", Toast.LENGTH_SHORT).show();
                    try {
                        JSONArray classes = response.getJSONArray("items");
                        for (int i = 0; i < classes.length(); i++) {
                            JSONObject singleClass = (JSONObject) classes.get(i);
                            if (singleClass.get("daysOfWeek").toString().equals("MWF")) {
                                MWFlist.add(singleClass);
                            } else {
                                TRlist.add(singleClass);
                            }
                            namesAdded.add(singleClass.get("courseId").toString());
                        }

                        addToCalendar();
                        saveToSchedule();
                        name.setText(response.get("title").toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
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
     * This function works though all the necessary methods to save a mock schedule to the backend
     * It first needs to create a mock schedule, then gets the id from the schedule and adds the classes one by one to that schedule.
     * @throws JSONException in case some object cannot be represented as a JSONArray or Object
     */
    private void saveScheduleBackend() throws JSONException {
        String scheduleName = name.getText().toString().trim();
        if (scheduleName.isEmpty()) {
            Toast.makeText(this, "Must enter a name for the schedule", Toast.LENGTH_SHORT).show();
            return;
        }

        // Combine MWF and TR lists
        ArrayList<Object> list = new ArrayList<>();
        int MWFIndex = 0, TRIndex = 0;
        while (list.size() < MWFlist.size() + TRlist.size()) {
            if (MWFIndex < MWFlist.size()) list.add(MWFlist.get(MWFIndex++));
            if (TRIndex < TRlist.size()) list.add(TRlist.get(TRIndex++));
        }

        if (list.size() < 3) {
            Toast.makeText(this, "Must enter at least 3 classes for the schedule.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        String baseUrl = "http://coms-3090-019.class.las.iastate.edu:8080/mock-schedules";
        long id = getIntent().getLongExtra("id", -1);

        // Define what happens after delete (or if no old schedule)
        Runnable createNewSchedule = () -> {
            try {
                JSONObject scheduleBody = new JSONObject();
                scheduleBody.put("title", name.getText().toString());
                scheduleBody.put("term", "Spring 2026");
                scheduleBody.put("ownerNetId", getIntent().getStringExtra("username"));

                JsonObjectRequest createRequest = new JsonObjectRequest(
                        Request.Method.POST,
                        baseUrl,
                        scheduleBody,
                        response -> {
                            try {
                                long scheduleId = response.getLong("id");
                                Log.d("Schedule", "Created schedule with ID: " + scheduleId);

                                // Add all courses
                                for (Object obj : list) {
                                    JSONObject course = (JSONObject) obj;
                                    String courseId = course.getString("courseId");
                                    String sectionId = course.getString("sectionId");

                                    JSONObject item = new JSONObject();
                                    item.put("courseId", courseId);
                                    item.put("sectionId", sectionId);

                                    String itemUrl = baseUrl + "/" + scheduleId + "/items";
                                    JsonObjectRequest addItemRequest = new JsonObjectRequest(
                                            Request.Method.POST,
                                            itemUrl,
                                            item,
                                            itemRes -> Log.d("Schedule", "Added item: " + itemRes.toString()),
                                            err -> Log.e("Schedule", "Error adding item: " + err.toString())
                                    );
                                    queue.add(addItemRequest);
                                }

                                Toast.makeText(this, "Schedule saved successfully!", Toast.LENGTH_SHORT).show();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        },
                        error -> Log.e("Schedule", "Error creating schedule: " + error.toString())
                );
                queue.add(createRequest);

            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // If an old schedule exists, delete it first
        if (id >= 0) {
            String deleteUrl = baseUrl + "/" + id;
            StringRequest deleteRequest = new StringRequest(
                    Request.Method.DELETE,
                    deleteUrl,
                    response -> {
                        Toast.makeText(this, "Old schedule deleted.", Toast.LENGTH_SHORT).show();
                        createNewSchedule.run(); // Create after deletion completes
                    },
                    error -> {
                        Toast.makeText(this, "Delete failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("Schedule", "Delete error: ", error);
                    }
            );
            queue.add(deleteRequest);
        } else {
            createNewSchedule.run(); // No old schedule, just create
        }
    }

    /**
     * Function adds classes to the UI calendar
     */
    private void addToCalendar() {
        long id = getIntent().getLongExtra("id", -2);
        GridLayout grid = findViewById(R.id.schedule_grid);
        for (int i = grid.getChildCount() - 1; i > 4; i--) {
            View child = grid.getChildAt(i);
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) child.getLayoutParams();
            grid.removeViewAt(i);
        }
        ArrayList<Object> list;


        // Construct ArrayList of MWF and TR alternating
        list = new ArrayList<>();
        int MWFIndex = 0, TRIndex = 0;
        while (list.size() < MWFlist.size() + TRlist.size()) {
            if (MWFIndex < MWFlist.size()) {
                list.add(MWFlist.get(MWFIndex));
                MWFIndex++;
            }
            if (TRIndex < TRlist.size()) {
                list.add(TRlist.get(TRIndex));
                TRIndex++;
            }
        }

        // Iterate through the list while removing the first element each time to ensure alternation.
        while (!list.isEmpty()) {
            Object obj = list.remove(0);
            JSONObject classObj = (JSONObject) obj;
            if (MWFlist.contains(obj)) {
                for (int i = 0; i <= 4; i += 2) {
                    String sectionId = classObj.optString("sectionId", "");

                    TextView classBlock = new TextView(this);
                    classBlock.setText(sectionId);
                    classBlock.setGravity(Gravity.CENTER);
                    classBlock.setTextColor(Color.parseColor("#212121"));
                    classBlock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    classBlock.setBackgroundResource(R.drawable.calendar_class_bg);

                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0;
                    params.height = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
                    params.setMargins(4, 4, 4, 4);
                    params.columnSpec = GridLayout.spec(i, 1f);
                    grid.addView(classBlock, params);
                }
            } else {
                for (int i = 1; i <= 3; i += 2) {
                    String sectionId = classObj.optString("sectionId", "");

                    TextView classBlock = new TextView(this);
                    classBlock.setText(sectionId);
                    classBlock.setGravity(Gravity.CENTER);
                    classBlock.setTextColor(Color.parseColor("#212121"));
                    classBlock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                    classBlock.setBackgroundResource(R.drawable.calendar_class_bg);

                    GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                    params.width = 0;
                    params.height = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
                    params.setMargins(4, 4, 4, 4);
                    params.columnSpec = GridLayout.spec(i, 1f);
                    grid.addView(classBlock, params);
                }
            }
        }
    }

    /**
     * This saves the selected class to a list below the calendar which allows users to remove the class from the calendar
     */
    private void saveToSchedule() {
        // Clear existing views
        addedClassesList.removeAllViews();
        JSONArray response = new JSONArray();
        for (Object obj : MWFlist) {
            response.put(obj);
        }

        for (Object obj : TRlist) {
            response.put(obj);
        }

        // Example: Create 5 class cards dynamically
        for (int i = 0; i < response.length(); i++) {
            // Create horizontal container (the card)
            try {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setBackgroundResource(R.drawable.class_card_bg);
                card.setPadding(16, 16, 16, 16);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 12, 0, 12); // margin between cards
                card.setLayoutParams(cardParams);
                card.setElevation(6f);
                card.setGravity(Gravity.CENTER_VERTICAL);

                // Create the class name TextView
                TextView className = new TextView(this);
                className.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                ));

                final JSONObject classObj = response.getJSONObject(i);
                final String classText = classObj.getString("sectionId");
                final String daysOfTheWeek = classObj.getString("daysOfWeek");
                className.setText(classText + " " + daysOfTheWeek);
                className.setTextColor(Color.parseColor("#212121"));
                className.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                className.setTypeface(null, Typeface.BOLD);

                // Create the Add button TextView
                TextView removeBtn = new TextView(this);
                removeBtn.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                removeBtn.setText("Remove");
                removeBtn.setTextColor(Color.WHITE);
                removeBtn.setBackgroundResource(R.drawable.rounded_button);
                removeBtn.setPadding(32, 12, 32, 12);
                removeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                removeBtn.setElevation(3f);

                // Add button click listener (example)
                removeBtn.setOnClickListener(v -> {
                    addedClassesList.removeView(card);

                    if (MWFlist.contains(classObj)) {
                        MWFlist.remove(classObj);
                    } else {
                        TRlist.remove(classObj);
                    }

                    getClasses(search.getText().toString());
                    addToCalendar();

                    try {
                        String courseId = classObj.getString("courseId");
                        namesAdded.remove(courseId);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });

                // Add both elements to the card
                card.addView(className);
                card.addView(removeBtn);

                // Add the card to the parent container
                addedClassesList.addView(card);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * gets available sections from backend
     * @param filter for passing to output classes
     */
    private void getClasses(String filter) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/sections";
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("VOLLEY", "GET ok (" + response.length() + "): " + response);
//                    Toast.makeText(this, "Loaded " + response.length() + " classes", Toast.LENGTH_SHORT).show();
                    outputClasses(response, filter);
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
     * Outputs classes fetched from getClasses()
     * If a class is selected, the other section of that same class is removed from potential classes to select
     * @param response from getClasses()
     * @param filter from user searched in editText
     */
    private void outputClasses(JSONArray response, String filter) {
        // Clear existing views
        classList.removeAllViews();

        // Example: Create 5 class cards dynamically
        for (int i = 0; i < response.length(); i++) {
            // Create horizontal container (the card)
            try {
                final JSONObject classObj = response.getJSONObject(i);
                final String classText = classObj.getString("sectionId");
                final String daysOfTheWeek = classObj.getString("daysOfWeek");
                final String courseId = classObj.getString("courseId");

                if (!classText.toLowerCase().contains(filter.toLowerCase()) && !daysOfTheWeek.toLowerCase().contains(filter.toLowerCase())) {
                    continue;
                }

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setBackgroundResource(R.drawable.class_card_bg);
                card.setPadding(16, 16, 16, 16);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 12, 0, 12); // margin between cards
                card.setLayoutParams(cardParams);
                card.setElevation(6f);
                card.setGravity(Gravity.CENTER_VERTICAL);

                // Create the class name TextView
                TextView className = new TextView(this);
                className.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                ));

                className.setText(classText + " " + daysOfTheWeek);
                className.setTextColor(Color.parseColor("#212121"));
                className.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                className.setTypeface(null, Typeface.BOLD);

                // Create the Add button TextView
                TextView addBtn = new TextView(this);
                addBtn.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                addBtn.setText("Add");
                addBtn.setTextColor(Color.WHITE);
                addBtn.setBackgroundResource(R.drawable.rounded_button);
                addBtn.setPadding(32, 12, 32, 12);
                addBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                addBtn.setElevation(3f);

                // Add button click listener (example)
                addBtn.setOnClickListener(v -> {
                    if (daysOfTheWeek.equals("MWF")) {
                        if (MWFlist.size() < 4) {
                            MWFlist.add(classObj);
                        } else {
                            Toast.makeText(this, "Too many classes selected for MWF.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        if (TRlist.size() < 4) {
                            TRlist.add(classObj);
                        } else {
                            Toast.makeText(this, "Too many classes selected for TR.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    classList.removeView(card);

                    saveToSchedule();
                    addToCalendar();

                    namesAdded.add(courseId);
                    outputClasses(response, search.getText().toString());
                });

                // Add both elements to the card
                if (!namesAdded.contains(courseId)) {
                    card.addView(className);
                    card.addView(addBtn);
                    classList.addView(card);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
