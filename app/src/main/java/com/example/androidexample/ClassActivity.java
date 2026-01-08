package com.example.androidexample;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ClassActivity extends AppCompatActivity {
    private TextView classNameTv, postButton, profile, back, ratingButton;
    private RelativeLayout page;
    private EditText commentEt;
    private GridLayout comments;

    private static final String BASE = "http://coms-3090-019.class.las.iastate.edu:8080";
    private static final String COMMENTS = BASE + "/comments";
    private String classKey;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        classNameTv = findViewById(R.id.className);
        postButton = findViewById(R.id.postBtn);
        profile = findViewById(R.id.profile);
        back = findViewById(R.id.btnBack);
        page = findViewById(R.id.class_page);
        commentEt = findViewById(R.id.enterComment);
        comments = findViewById(R.id.commentsGrid);

        ratingButton = findViewById(R.id.rating_btn);
        ratingButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, CourseRatingActivity.class);
            intent.putExtra("courseId", getIntent().getStringExtra("class_name"));
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        classKey = getIntent().getStringExtra("class_name");
        classNameTv.setText(classKey);

        username = getIntent().getStringExtra("username");
        profile.setText(username);

        final String finalUsername = username;
        postButton.setOnClickListener(view -> {
            String text = commentEt.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Enter a comment first", Toast.LENGTH_SHORT).show();
                return;
            }
            postComment(finalUsername, text, classKey);
        });

        back.setOnClickListener(view -> finish());

        page.setOnTouchListener((v, event) -> {
            View current = getCurrentFocus();
            if (current != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(current.getWindowToken(), 0);
                current.clearFocus();
            }
            v.performClick();
            return false;
        });
         getComments();
    }

    private void postComment(String username, String commentPosted, String className) {
        RequestQueue queue = Volley.newRequestQueue(this);
        JSONObject body = new JSONObject();
        try {
            body.put("user", username);
            body.put("comment", commentPosted);
            body.put("className", className);
        } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                COMMENTS,
                body,
                response -> {
                    Log.d("VOLLEY", "POST ok: " + response);
                    Toast.makeText(this, "Comment posted!", Toast.LENGTH_SHORT).show();
                    commentEt.setText("");
                    getComments(); // refresh list
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

    private void getComments() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String encodedClassKey = "";
        try {
            encodedClassKey = URLEncoder.encode(classKey, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            e.printStackTrace();
            encodedClassKey = classKey;
        }
        String url = COMMENTS + "/" + encodedClassKey;
        System.out.println(url);
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("VOLLEY", "GET ok (" + response.length() + "): " + response);
                    Toast.makeText(this, "Loaded " + response.length() + " comments", Toast.LENGTH_SHORT).show();
                    addCommentCells(comments, response);
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

    private void updateComment(long id, String username, String text, String className) {
        RequestQueue q = Volley.newRequestQueue(this);
        JSONObject body = new JSONObject();
        try {
            body.put("user", username);
            body.put("comment", text);
            body.put("className", className);
        } catch (Exception ignored) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                COMMENTS + "/" + id,
                body,
                res -> {
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                    getComments();
                },
                err -> {
                    NetworkResponse nr = err.networkResponse;
                    Log.e("VOLLEY","PUT err "+(nr!=null?nr.statusCode:-1));
                }) {
            @Override public Map<String,String> getHeaders(){
                Map<String,String> h = new HashMap<>();
                h.put("Content-Type","application/json");
                return h;
            }
        };
        q.add(req);
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

    private void deleteComment(long id) {
        RequestQueue q = Volley.newRequestQueue(this);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.DELETE,
                COMMENTS + "/" + id,
                null,
                res -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    getComments();
                },
                err -> {
                    NetworkResponse nr = err.networkResponse;
                    Log.e("VOLLEY","DELETE err "+(nr!=null?nr.statusCode:-1));
                });
        q.add(req);
    }

    private void addCommentCells(GridLayout parent, JSONArray comments) {
        parent.removeAllViews();

        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics());

        for (int i = comments.length() - 1; i >= 0; i--) {
            try {
                JSONObject obj = comments.getJSONObject(i);
                long id        = obj.optLong("id", -1);
                String user    = obj.optString("user");
                String text    = obj.optString("comment");
                String cls     = obj.optString("className");

                // Card container
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(ContextCompat.getDrawable(this, R.drawable.comment_card));
                card.setPadding(pad, pad, pad, pad);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.setMargins(margin, margin, margin, margin);
                card.setLayoutParams(lp);

                // Header: user • class
                TextView header = new TextView(this);
                header.setText(user);
                header.setTextColor(ContextCompat.getColor(this, R.color.white));
                header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
                card.addView(header);

                // Body: comment text
                TextView body = new TextView(this);
                body.setText(text);
                body.setTextColor(ContextCompat.getColor(this, R.color.white));
                body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                body.setPadding(0, (int)(pad*0.5), 0, (int)(pad*0.5));
                card.addView(body);

                // Button bar
                LinearLayout bar = new LinearLayout(this);
                bar.setOrientation(LinearLayout.HORIZONTAL);

                TextView btnEdit = buildActionButton("Edit");
                TextView btnDelete = buildActionButton("Delete");

                // Wire up actions
                btnEdit.setOnClickListener(v -> {
                    showEditDialog(id, user, text, cls);
                });

                btnDelete.setOnClickListener(v -> {
                    if (id <= 0) {
                        Toast.makeText(this, "Missing comment id", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    deleteComment(id);
                    getComments();
                });

                // Layout buttons with spacing
                LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                b1.setMargins(0, 0, margin, 0);
                btnEdit.setLayoutParams(b1);

                LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                b2.setMargins(margin, 0, 0, 0);
                btnDelete.setLayoutParams(b2);

                if (user.equals(username)) {
                    bar.addView(btnEdit);
                    bar.addView(btnDelete);
                    card.addView(bar);
                }
                parent.addView(card);

            } catch (Exception e) {
                Log.e("UI", "Render comment failed", e);
            }
        }
    }

    /** Small helper to keep buttons consistent */
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
}