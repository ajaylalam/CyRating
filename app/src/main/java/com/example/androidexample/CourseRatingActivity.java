package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.androidexample.ui.CommentUi;
import com.example.androidexample.utils.CommentApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseRatingActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText commentEditText;
    private Button submitButton, backButton;
    private RequestQueue queue;
    private TextView averageRatingTextView, rateCourseTitle;
    private GridLayout comments;
    private final List<JSONObject> commentList = new ArrayList<>();

    // Replace this later dynamically (from intent or course selection)
    private String courseId;

    private static final String BASE_URL = "http://coms-3090-019.class.las.iastate.edu:8080";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_rating);

        ratingBar = findViewById(R.id.ratingBar);
        commentEditText = findViewById(R.id.commentEditText);
        submitButton = findViewById(R.id.submitButton);
        averageRatingTextView = findViewById(R.id.averageRatingTextView);
        backButton = findViewById(R.id.btnBack);

        queue = Volley.newRequestQueue(this);

        // TEMP: use hardcoded courseId until Home/Search integration
        courseId = getIntent().getStringExtra("courseId");
        if (courseId == null || courseId.isEmpty()) {
            courseId = "COMS309";
        }

        // Fetch average when page opens
        fetchAverageRating();

        // Handle submit button
        submitButton.setOnClickListener(view -> {
            if (submitRating()) {
                postComment(getIntent().getStringExtra("username"), commentEditText.getText().toString(), courseId);
            }
        });

        // Handle Back button
        backButton.setOnClickListener(view -> {
            finish();
        });

        rateCourseTitle = findViewById(R.id.rateCourseTitle);
        rateCourseTitle.setText("Rate " + courseId);

        comments = findViewById(R.id.commentsGrid);
        getComments();
    }

    private void postComment(String username, String commentPosted, String className) {
        CommentApi.postComment(
                this,
                username,
                commentPosted,
                className,
                () -> {
                    Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show();
                    commentEditText.setText("");
                    getComments(); // or loadComments() if you renamed it
                },
                error -> {
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null) {
                        Log.e("VOLLEY", "POST err " + nr.statusCode +
                                " body=" + new String(nr.data));
                        Toast.makeText(this,
                                "Post failed: " + nr.statusCode,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("VOLLEY", "POST err " + error);
                        Toast.makeText(this,
                                "Network error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private TextView buildActionButton(String label) {
        int padH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        int padV = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());

        TextView b = new TextView(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(true);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        b.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_secondary));
        b.setPadding(padH, padV, padH, padV);
        b.setElevation(3f);
        return b;
    }

    private void showEditDialog(long id, String username, String oldText, String className) {
        final EditText input = new EditText(this);
        input.setText(oldText);
        input.setSelection(input.getText().length());
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Edit Comment")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String newText = input.getText().toString().trim();
                    if (newText.isEmpty()) {
                        Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    updateComment(id, username, newText, className);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateComment(long id, String username, String text, String className) {
        CommentApi.updateComment(
                this,
                id,
                username,
                text,
                className,
                () -> {
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                    getComments();
                },
                err -> {
                    NetworkResponse nr = err.networkResponse;
                    Log.e("VOLLEY", "PUT err " + (nr != null ? nr.statusCode : -1));
                }
        );
    }


    private void deleteComment(long id) {
        CommentApi.deleteComment(
                this,
                id,
                () -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    getComments();
                },
                err -> {
                    NetworkResponse nr = err.networkResponse;
                    Log.e("VOLLEY", "DELETE err " + (nr != null ? nr.statusCode : -1));
                }
        );
    }
    private void getComments() {
        String username = getIntent().getStringExtra("username");

        CommentApi.getCommentsForCourse(
            this,
            courseId,
            result -> {
                commentList.clear();
                commentList.addAll(result);

                CommentUi.renderCourseRatingCommentsGrid(
                        this,
                        comments,
                        commentList,
                        username,
                        // Edit clicked
                        (commentId, currentText) -> {
                            showEditDialog(commentId, username, currentText, courseId);
                        },
                        // Delete clicked
                        commentId -> {
                            CommentApi.deleteComment(
                                    this,
                                    commentId,
                                    () -> {
                                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                                        getComments();
                                    },
                                    err -> {
                                        NetworkResponse nr = err.networkResponse;
                                        Log.e("VOLLEY","DELETE err "+(nr!=null?nr.statusCode:-1));
                                    }
                            );
                        }
                );
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
     * Fetches average rating from backend
     */
    private void fetchAverageRating() {
        String url = BASE_URL + "/courses/" + courseId + "/average";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double average = response.getDouble("average");
                        averageRatingTextView.setText("Average Rating: " + String.format("%.2f ★", average));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        averageRatingTextView.setText("Average Rating: N/A");
                    }
                },
                error -> {
                    error.printStackTrace();
                    averageRatingTextView.setText("Average Rating: N/A");
                });

        queue.add(request);
    }

    /**
     * Sends rating and optional comment to backend using POST
     */
    private boolean submitRating() {
        float rating = ratingBar.getRating();
        String comment = commentEditText.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please give a star rating", Toast.LENGTH_SHORT).show();
            return false;
        }

        String url = BASE_URL + "/courses/" + courseId + "/ratings";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("ratingValue", (int) rating);
            jsonBody.put("comment", comment.isEmpty() ? "" : comment);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    Toast.makeText(this, "Rating submitted!", Toast.LENGTH_SHORT).show();

                    // Reset inputs
                    ratingBar.setRating(0);
                    commentEditText.setText("");

                    // Update displayed average rating
                    fetchAverageRating();
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Failed to submit rating", Toast.LENGTH_LONG).show();
                });

        queue.add(request);
        return true;
    }
}