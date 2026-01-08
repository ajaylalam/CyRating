package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.example.androidexample.utils.AssignmentApi;
import com.example.androidexample.utils.AssignmentUi;
import com.example.androidexample.utils.ClassUi;
import com.example.androidexample.utils.ClassesApi;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;

public class ProfessorHome extends AppCompatActivity {
    // class field
    ArrayList<JSONObject> courses = new ArrayList<>();
    GridLayout assignments;
    ArrayList<JSONObject> allAssignments = new ArrayList<>();
    Button logout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professor_home);

        String username = getIntent().getStringExtra("username");
        getClasses(username);

        assignments = findViewById(R.id.assignmentsGrid);
        logout = findViewById(R.id.logOut);
        logout.setOnClickListener(view -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }

    // function to output courses that this professor teaches
    private void outputClasses() {
        GridLayout gridLayout = findViewById(R.id.classDisplay);

        ClassUi.renderCourseGrid(
                this,
                gridLayout,
                courses,
                course -> {
                    String code = course.optString("courseId");
                    String name = course.optString("courseName");

                    Intent intent = new Intent(this, ProfessorEditClass.class);
                    intent.putExtra("course", name);
                    intent.putExtra("id", code);
                    intent.putExtra("description", course.optString("description"));
                    intent.putExtra("username", getIntent().getStringExtra("username"));
                    startActivity(intent);
                }
        );

        allAssignments.clear();
        for (JSONObject c : courses) {
            String code = c.optString("courseId");
            getAssignments(code);
        }
    }
    private void outputAssignments() {
        if (assignments == null) {
            assignments = findViewById(R.id.assignmentsGrid);
        }

        // still sort here so helper stays generic
        allAssignments.sort(Comparator.comparing(o -> o.optString("dueDate")));

        AssignmentUi.renderAssignmentGrid(
                this,
                assignments,
                allAssignments,
                /* showSubmitButton = */ false,
                /* onCardClick = */ assignment -> {},
                /* onSubmitClick = */ null
        );
    }
    private void getAssignments(String courseId) {
        AssignmentApi.getAssignmentsForCourse(
                this,
                courseId,
                assignmentsList -> {
                    // success: add to your list and update UI
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

    // function to fetch all courses who have the same netid as this professor
    private void getClasses(String professorNetId) {
        ClassesApi.getCoursesForProfessor(
            this,
            professorNetId,
            result -> {
                courses.clear();
                courses.addAll(result);

                System.out.println("Loaded " + courses.size() + " courses");
                outputClasses();
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
}
