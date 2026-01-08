package com.example.androidexample;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.androidexample.utils.AssignmentApi;
import com.example.androidexample.utils.AssignmentUi;
import com.example.androidexample.utils.ClassUi;
import com.example.androidexample.utils.ClassesApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * New Homepage for the app which has reduced redness on the page.
 */
public class NewHomeActivity extends AppCompatActivity {

    private TextView classes, home, groups, profile, create_schedule, sendmessage, clubs;
    private GridLayout classOutput, assignments;
    private ArrayList<JSONObject> allAssignments = new ArrayList<>();
    private final ArrayList<JSONObject> enrolledCourses = new ArrayList<>();

    private Uri selectedFileUri = null;
    private ActivityResultLauncher<Intent> pickFileLauncher;
    private NotificationWebSocketClient wsClient;

    private void outputAssignments() {
        if (assignments == null) {
            assignments = findViewById(R.id.assignmentsGrid);
        }

        // sort by due date
        allAssignments.sort(Comparator.comparing(o -> o.optString("dueDate")));

        AssignmentUi.renderAssignmentGrid(
                this,
                assignments,
                allAssignments,
                /* showSubmitButton = */ true,
                /* onCardClick = */ assignment -> {},
                /* onSubmitClick = */ assignment -> {
                    showFileUploadDialog();
                }
        );
    }

    private TextView currentFileNameTextView;  // track the dialog's filename text

    private void showFileUploadDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Submit Assignment");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_file_upload, null);
        currentFileNameTextView = dialogView.findViewById(R.id.fileNameText);
        Button chooseFileBtn = dialogView.findViewById(R.id.chooseFileButton);

        // Reset previous selection
        selectedFileUri = null;
        currentFileNameTextView.setText("No file selected");

        chooseFileBtn.setOnClickListener(v -> openFilePicker());

        builder.setView(dialogView);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please choose a file first", Toast.LENGTH_SHORT).show();
            } else {
                // TODO: upload file using selectedFileUri
                Toast.makeText(this, "Would upload: " + selectedFileUri, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // or "application/pdf", "image/*", etc.
        pickFileLauncher.launch(intent);
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "Selected file";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    result = cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    private void getAssignments(String course) {
        AssignmentApi.getAssignmentsForCourse(
                this,
                course,
                assignmentsList -> {
                    // Add them to the home-page list and refresh UI
                    allAssignments.addAll(assignmentsList);
                    outputAssignments();
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

    /**
     * Fetches the enrolled classes for the logged in user and outputs them to the top of the page
     */
    private void getClasses() {
        ClassesApi.getEnrolledClasses(
                this,
                getIntent().getStringExtra("username"),
                classesList -> {
                    // Update your local data
                    enrolledCourses.clear();
                    enrolledCourses.addAll(classesList);

                    // Output UI
                    outputClasses(enrolledCourses);

                    // Load assignments for each class
                    allAssignments.clear();
                    for (JSONObject c : classesList) {
                        getAssignments(c.optString("courseId"));
                    }
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
    private void outputClasses(List<JSONObject> classesList) {
        classOutput.removeAllViews();

        ClassUi.renderCourseGrid(
                this,
                classOutput,
                classesList,
                course -> {
                    String courseId = course.optString("courseId");
                    Intent intent = new Intent(this, CourseRatingActivity.class);
                    intent.putExtra("courseId", courseId);
                    intent.putExtra("username", getIntent().getStringExtra("username"));
                    startActivity(intent);
                }
        );
    }

    /**
     * Called when the page gets loaded
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1) apply theme FIRST
        ThemeManager.applyTheme(this);

        // 2) then normal activity setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newhome);

        // === Theme toggle switch ===
        Switch switchTheme = findViewById(R.id.switchTheme);

        // 1) get current theme from prefs
        boolean isDark = ThemeManager.isDark(this);

        // 2) set initial state WITHOUT triggering the listener
        switchTheme.setOnCheckedChangeListener(null);
        switchTheme.setChecked(isDark);

        // 3) attach listener
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // if nothing actually changed, do nothing (avoids weird loops/reflashes)
            boolean currentlyDark = ThemeManager.isDark(NewHomeActivity.this);
            if (isChecked == currentlyDark) return;

            String newTheme = isChecked
                    ? ThemeManager.THEME_DARK
                    : ThemeManager.THEME_LIGHT;

            ThemeManager.setTheme(NewHomeActivity.this, newTheme);
            // no manual recreate(); AppCompatDelegate handles it
        });

        String username = getIntent().getStringExtra("username");

        // notifications WS
        WebSocketManager.getInstance().connect(username, this);

        classOutput = findViewById(R.id.classDisplay);

        // footer nav buttons
        classes = findViewById(R.id.classes_btn);
        classes.setOnClickListener(view -> {
            Intent intent = new Intent(this, ClassesPageActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        groups = findViewById(R.id.groups_btn);
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

        clubs = findViewById(R.id.clubs);
        clubs.setOnClickListener(view -> {
            Intent intent = new Intent(NewHomeActivity.this, ClubsMenuActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        profile = findViewById(R.id.profile_btn);
        profile.setText(username);
        profile.setOnClickListener(view -> {
            Intent intent = new Intent(this, MenuProfileActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        assignments = findViewById(R.id.assignmentsGrid);

        getClasses();
    }

    /**
     * Close keyboard when tapping outside EditText
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();
        if (view instanceof EditText) {
            Rect outRect = new Rect();
            view.getGlobalVisibleRect(outRect);

            if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                view.clearFocus();
                hideKeyboard(view);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}