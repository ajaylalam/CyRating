package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;


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

public class EnrolledClubsActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://coms-3090-019.class.las.iastate.edu:8080";

    // UI
    private ListView listView;
    private TextView tvEmptyClubs;
    private TextView btnBack;
    private TextView btnHome;
    private TextView btnRefreshClubs;
    private ProgressBar progressBar;

    // data
    private final ArrayList<ClubItem> clubList = new ArrayList<>();
    private final ArrayList<String> displayList = new ArrayList<>();
    private ArrayAdapter<String> listAdapter;

    private String username;     // used for X-NetID
    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrolled_clubs);

        // username / netId from previous screen
        username = getIntent().getStringExtra("netId");
        if (username == null || username.isEmpty()) {
            username = getIntent().getStringExtra("username");
        }

        // init Volley
        queue = Volley.newRequestQueue(this);

        // hook up views (IDs must match XML)
        listView        = findViewById(R.id.listEnrolledClubs);
        tvEmptyClubs    = findViewById(R.id.tvEmptyClubs);
        btnBack         = findViewById(R.id.back_btn);
        btnHome         = findViewById(R.id.home_btn);
        btnRefreshClubs = findViewById(R.id.btnRefreshClubs);
        progressBar     = findViewById(R.id.progressBar);

        listAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );
        listView.setAdapter(listAdapter);
        // when user taps an enrolled club -> open that club's chat
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= clubList.size()) return;

            ClubItem clicked = clubList.get(position);

            Intent i = new Intent(EnrolledClubsActivity.this, ClubChatActivity.class);
            i.putExtra(ClubChatActivity.EXTRA_CLUB_ID, clicked.id);
            i.putExtra(ClubChatActivity.EXTRA_USER_ID, username);
            i.putExtra(ClubChatActivity.EXTRA_CLUB_NAME, clicked.name);
            startActivity(i);
        });


        // back: just finish
        btnBack.setOnClickListener(v -> finish());

        // home: go to NewHomeActivity
        btnHome.setOnClickListener(v -> {
            Intent i = new Intent(this, NewHomeActivity.class);
            i.putExtra("username", username);
            startActivity(i);
            finish();
        });

        // REFRESH button – reload enrolled list
        btnRefreshClubs.setOnClickListener(v -> loadEnrolledClubs());

        // (later) list item click → open chat/details
        // listView.setOnItemClickListener((parent, view, position, id) -> { ... });

        // load once when screen opens
        loadEnrolledClubs();
    }

    private void loadEnrolledClubs() {
        String url = BASE_URL + "/clubs/enrolled";

        progressBar.setVisibility(View.VISIBLE);
        tvEmptyClubs.setVisibility(View.GONE);

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    handleEnrolledResponse(response);
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmptyClubs.setText("Failed to load enrolled clubs");
                    tvEmptyClubs.setVisibility(View.VISIBLE);
                    Toast.makeText(this,
                            "Error: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("X-NetID", username);
                return headers;
            }
        };

        queue.add(req);
    }

    private void handleEnrolledResponse(JSONArray arr) {
        clubList.clear();
        displayList.clear();

        if (arr.length() == 0) {
            tvEmptyClubs.setText("You haven't enrolled in any clubs yet.");
            tvEmptyClubs.setVisibility(View.VISIBLE);
        } else {
            tvEmptyClubs.setVisibility(View.GONE);
        }

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                int id = obj.getInt("id");
                String name = obj.optString("name", "Club " + id);
                String desc = obj.optString("description", "");

                ClubItem item = new ClubItem(id, name, desc);
                clubList.add(item);

                String display = name;
                if (!desc.isEmpty()) display += " – " + desc;
                displayList.add(display);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        listAdapter.notifyDataSetChanged();
    }
}
