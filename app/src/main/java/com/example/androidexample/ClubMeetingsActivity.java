package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClubMeetingsActivity extends AppCompatActivity {

    private ListView listMeetings;
    private TextView tvEmptyMeetings;
    private ProgressBar progressMeetings;
    private TextView btnBack;
    private TextView tvClubTitle;

    private final ArrayList<String> displayList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private int clubId;
    private String username;
    private String groupName;  // "SWE Club"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_meetings);

        clubId    = getIntent().getIntExtra("clubId", -1);
        username  = getIntent().getStringExtra("username");
        groupName = getIntent().getStringExtra("groupName");

        listMeetings      = findViewById(R.id.listMeetings);
        tvEmptyMeetings   = findViewById(R.id.tvEmptyMeetings);
        progressMeetings  = findViewById(R.id.progressMeetings);
        btnBack           = findViewById(R.id.btnBack);
        tvClubTitle       = findViewById(R.id.tvClubTitle);

        if (groupName != null && !groupName.isEmpty()) {
            tvClubTitle.setText(groupName + " Meetings");
        }

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                displayList);
        listMeetings.setAdapter(adapter);

        // Back button: just close this screen -> back to details
        btnBack.setOnClickListener(v -> finish());

        // Optional: tapping a meeting jumps straight back to chat
        listMeetings.setOnItemClickListener((parent, view, position, id) -> {
            // Go straight back to the club chat
            Intent i = new Intent(ClubMeetingsActivity.this, GroupChatActivity.class);
            String groupId = "CLUB-" + clubId;
            i.putExtra(GroupChatActivity.EXTRA_GROUP_ID, groupId);
            i.putExtra(GroupChatActivity.EXTRA_USERNAME, username);
            i.putExtra(GroupChatActivity.EXTRA_GROUP_NAME, groupName);
            i.putExtra(GroupChatActivity.EXTRA_BACK_TARGET, "clubs_menu");
            startActivity(i);
            finish();
        });

        loadMeetings();
    }

    private void loadMeetings() {
        if (clubId <= 0) {
            tvEmptyMeetings.setText("No club selected.");
            tvEmptyMeetings.setVisibility(View.VISIBLE);
            return;
        }

        String url = ApiConfig.BASE_HTTP + "/clubs/" + clubId + "/meetings";

        progressMeetings.setVisibility(View.VISIBLE);
        tvEmptyMeetings.setVisibility(View.GONE);

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressMeetings.setVisibility(View.GONE);
                    handleMeetingsResponse(response);
                },
                error -> {
                    progressMeetings.setVisibility(View.GONE);
                    tvEmptyMeetings.setText("Failed to load meetings");
                    tvEmptyMeetings.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Error loading meetings", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                if (username != null) {
                    headers.put("X-NetID", username);
                }
                return headers;
            }
        };

        queue.add(req);
    }

    private void handleMeetingsResponse(JSONArray arr) {
        displayList.clear();

        if (arr.length() == 0) {
            tvEmptyMeetings.setText("No meetings scheduled for this club.");
            tvEmptyMeetings.setVisibility(View.VISIBLE);
        } else {
            tvEmptyMeetings.setVisibility(View.GONE);
        }

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);

                String title = obj.optString("title",
                        obj.optString("name", "Meeting " + (i + 1)));
                String time = obj.optString("time",
                        obj.optString("startTime", ""));

                String line = title;
                if (!time.isEmpty()) line += " – " + time;

                displayList.add(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        adapter.notifyDataSetChanged();
    }
}
