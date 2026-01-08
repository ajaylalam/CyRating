package com.example.androidexample;

import static com.google.android.material.internal.ViewUtils.dpToPx;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
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
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class CreateScheduleActivity extends AppCompatActivity {
    private TextView classes, home, groups, profile, create_schedule_btn;
    private GridLayout schedulesDisplay;
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_schedule);

        classes = findViewById(R.id.classes_btn);
        home = findViewById(R.id.home_btn);
        groups = findViewById(R.id.groups_btn);
        profile = findViewById(R.id.profile_btn);
        profile.setText(getIntent().getStringExtra("username"));
        profile.setOnClickListener(view -> {
            Intent intent = new Intent(this, MenuProfileActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });

        create_schedule_btn = findViewById(R.id.create_schedule_btn);
        create_schedule_btn.setOnClickListener(view -> {
            Intent intent = new Intent(this, MockScheduleActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            intent.putExtra("id", -1);
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

        groups.setOnClickListener(view -> {
            Intent intent = new Intent(this, GroupMenuActivity01.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
        });
        TextView clubs = findViewById(R.id.clubs);
        clubs.setOnClickListener(view -> {
            Intent intent = new Intent(CreateScheduleActivity.this, ClubsMenuActivity.class);
            intent.putExtra("username", getIntent().getStringExtra("username"));
            startActivity(intent);
            finish();   // optional, matches your other nav buttons
        });

        schedulesDisplay = findViewById(R.id.schedulesDisplay);

        displaySchedules();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
    private void displaySchedules() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/mock-schedules?ownerNetId=" + getIntent().getStringExtra("username");
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d("VOLLEY", "GET ok (" + response.length() + "): " + response);

                    schedulesDisplay.removeAllViews();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject item = response.getJSONObject(i);
                            String text = item.getString("title"); // replace "term" with your actual key

                            // === Outer vertical layout ===
                            LinearLayout itemLayout = new LinearLayout(this);
                            itemLayout.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            layoutParams.setMargins(dpToPx(100), 0, dpToPx(100), dpToPx(10));
                            itemLayout.setLayoutParams(layoutParams);

                            // === TextView for the title ===
                            TextView title = new TextView(this);
                            title.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            title.setText(text);
                            title.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            title.setTextSize(18);
                            title.setTypeface(null, Typeface.BOLD);
                            title.setTextColor(Color.parseColor("#C8102E"));
                            title.setGravity(Gravity.CENTER);

                            // === Red line under the title ===
                            View underline = new View(this);
                            LinearLayout.LayoutParams underlineParams = new LinearLayout.LayoutParams(
                                    dpToPx(100),
                                    dpToPx(2)
                            );
                            underlineParams.gravity = Gravity.CENTER;
                            underline.setLayoutParams(underlineParams);
                            underline.setBackgroundColor(Color.parseColor("#C8102E"));

                            // Add both views to itemLayout
                            itemLayout.addView(title);
                            itemLayout.addView(underline);

                            itemLayout.setOnClickListener(view -> {
                                Intent intent = new Intent(this, MockScheduleActivity.class);
                                intent.putExtra("username", getIntent().getStringExtra("username"));
                                try {
                                    intent.putExtra("id", item.getLong("id"));
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                                startActivity(intent);
                            });

                            // Add to parent layout
                            schedulesDisplay.addView(itemLayout);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
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
        return;
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
