package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.androidexample.ui.CommentUi;
import com.example.androidexample.utils.CommentApi;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ProfessorEditClass extends AppCompatActivity {

    EditText courseName, courseId, description;
    Button save;

    private static final String BASE_URL = "http://coms-3090-019.class.las.iastate.edu:8080";

    private final ArrayList<JSONObject> comments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.professor_edit_class);

        courseName = findViewById(R.id.inputCourseName);
        courseName.setText(getIntent().getStringExtra("course"));

        courseId = findViewById(R.id.inputCourseId);
        courseId.setText(getIntent().getStringExtra("id"));

        description = findViewById(R.id.inputDescription);
        description.setText(getIntent().getStringExtra("description"));

        save = findViewById(R.id.btnSaveClass);
        save.setOnClickListener(view -> updateClass());

        courseId.setEnabled(false);

        // 🔹 Load comments for this specific course
        String id = courseId.getText().toString().trim();
        if (!id.isEmpty()) {
            loadComments(id);
        }
    }

    private void updateClass() {
        String name = courseName.getText().toString().trim();
        String id   = courseId.getText().toString().trim();
        String desc = description.getText().toString().trim();

        if (id.isEmpty()) {
            Toast.makeText(this, "Course ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String netId = getIntent().getStringExtra("username"); // logged-in user

        try {
            JSONObject body = new JSONObject();
            body.put("courseName", name);
            body.put("description", desc);

            String url = BASE_URL + "/courses/" + id;

            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.PUT,
                    url,
                    body,
                    response -> {
                        Toast.makeText(this, "Course updated", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ProfessorEditClass.this, ProfessorHome.class);
                        intent.putExtra("username", getIntent().getStringExtra("username"));
                        startActivity(intent);
                        finish(); // close this screen
                    },
                    error -> {
                        if (error.networkResponse != null) {
                            int code = error.networkResponse.statusCode;
                            String serverBody = new String(error.networkResponse.data);
                            System.out.println("UPDATE ERROR " + code + " body: " + serverBody);

                            String msg;
                            if (code == 403) {
                                msg = "Not authorized to update this course.";
                            } else if (code == 400) {
                                msg = "Course not found or bad request.";
                            } else {
                                msg = "Error " + code;
                            }
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                        }

                        System.out.println(error);
                    }
            ) {
                @Override
                public java.util.Map<String, String> getHeaders() {
                    java.util.Map<String, String> headers = new java.util.HashMap<>();
                    headers.put("Content-Type", "application/json");
                    // adjust header name to whatever your filter expects
                    headers.put("X-Auth-NetId", netId);
                    return headers;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show();
        }
    }

    // =========================
    // COMMENTS SECTION
    // =========================

    // GET /comments/{courseCode}
    private void loadComments(String courseCode) {
        CommentApi.getCommentsForCourse(
                this,
                courseCode,
                result -> {
                    comments.clear();
                    comments.addAll(result);
                    outputComments();
                },
                error -> {
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null) {
                        System.out.println("COMMENTS ERROR " + nr.statusCode +
                                " body=" + new String(nr.data));
                    } else {
                        System.out.println("COMMENTS ERROR " + error);
                    }
                }
        );
    }

    // Render comments into a GridLayout like your courses
    private void outputComments() {
        GridLayout commentsGrid = findViewById(R.id.commentsGrid);
        if (commentsGrid == null) return;
        String course = courseId.getText().toString().trim();

        CommentUi.renderProfessorCommentsGrid(
                this,
                commentsGrid,
                comments,
                "PROFESSOR",
                // Respond
                (commentId, currResponse) -> {
                    showResponseDialog(commentId, currResponse);
                },
                // Delete
                commentId -> {
                    deleteComment(course, commentId);
                }
        );
    }

    private void deleteComment(String courseCode, long commentId) {
        CommentApi.deleteComment(
                this,
                commentId,
                () -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadComments(courseCode);
                },
                err -> {
                    NetworkResponse nr = err.networkResponse;
                    Log.e("VOLLEY", "DELETE err " + (nr != null ? nr.statusCode : -1));
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    loadComments(courseCode);
                }
        );
    }

    private void showResponseDialog(int commentId, String existingResponse) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        builder.setTitle("Professor Response");

        // input field
        final EditText input = new EditText(this);
        input.setHint("Type your response...");
        input.setText(existingResponse != null && !existingResponse.equals("null") ? existingResponse : "");
        input.setPadding(32, 32, 32, 32);

        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String newResponse = input.getText().toString().trim();
            submitProfessorResponse(commentId, newResponse);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void submitProfessorResponse(int commentId, String responseText) {
        // logged-in user
        String netId = getIntent().getStringExtra("username");

        // if you stored role in SharedPreferences earlier (recommended)
        // SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        // String role = prefs.getString("userRole", "PROFESSOR"); // default professor for now
        String role = "PROFESSOR"; // hard-coded if you know this screen is only for professors

        String url = BASE_URL + "/comments/" + commentId + "/respond";

        RequestQueue queue = Volley.newRequestQueue(this);

        com.android.volley.toolbox.StringRequest request =
                new com.android.volley.toolbox.StringRequest(
                        Request.Method.POST,
                        url,
                        resp -> {
                            Toast.makeText(this, "Response submitted", Toast.LENGTH_SHORT).show();
                            // Refresh comments for this class
                            loadComments(courseId.getText().toString().trim());
                        },
                        err -> {
                            Toast.makeText(this, "Error submitting response", Toast.LENGTH_SHORT).show();
                            err.printStackTrace();
                        }
                ) {
                    @Override
                    public byte[] getBody() {
                        // send the raw string the controller expects
                        return responseText.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }

                    @Override
                    public String getBodyContentType() {
                        // matches @RequestBody String nicely
                        return "text/plain; charset=utf-8";
                    }

                    @Override
                    public java.util.Map<String, String> getHeaders() {
                        java.util.Map<String, String> headers = new java.util.HashMap<>();
                        headers.put("Content-Type", "text/plain; charset=utf-8");
                        headers.put("X-Auth-NetId", netId);

                        return headers;
                    }
                };

        queue.add(request);
    }
}