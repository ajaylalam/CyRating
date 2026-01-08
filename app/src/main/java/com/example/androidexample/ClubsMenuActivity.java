package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClubsMenuActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://coms-3090-019.class.las.iastate.edu:8080";

    private String username;

    // top buttons
    private TextView backBtn, profileBtn;
    private TextView btnEnrolledClubs, btnCreateClub;

    // footer nav
    private TextView classesBtn, homeBtn, groupsBtn, clubsBtn, scheduleBtn;

    // available clubs list
    private ListView listClubs;
    private ProgressBar progressBar;
    private final ArrayList<ClubItem> clubList = new ArrayList<>();
    private final ArrayList<String> displayList = new ArrayList<>();
    private ArrayAdapter<String> clubsAdapter;

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs_menu);

        // username from previous screen
        username = getIntent().getStringExtra("username");

        // volley
        queue = Volley.newRequestQueue(this);

        // === top buttons ===
        backBtn = findViewById(R.id.back_btn);
        profileBtn = findViewById(R.id.profile_btn);
        btnEnrolledClubs = findViewById(R.id.btnEnrolledClubs);
        btnCreateClub    = findViewById(R.id.btnCreateClub);

        // === footer nav buttons ===
        classesBtn  = findViewById(R.id.classes_btn);
        homeBtn     = findViewById(R.id.home_btn);
        groupsBtn   = findViewById(R.id.groups_btn);
        clubsBtn    = findViewById(R.id.clubs);
        scheduleBtn = findViewById(R.id.create_schedule);

        // === list + progress ===
        listClubs   = findViewById(R.id.listClubs);
        progressBar = findViewById(R.id.progressBar);

        clubsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );
        listClubs.setAdapter(clubsAdapter);

        // ----------------- navigation listeners -----------------

        // back to home
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, NewHomeActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        // open profile
        profileBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, MenuProfileActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // enrolled clubs
        btnEnrolledClubs.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, EnrolledClubsActivity.class);
            intent.putExtra("netId", username);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // create club (for now reuses CreateGroupActivity)
        btnCreateClub.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, CreateGroupActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // footer nav
        classesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, ClassesPageActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        homeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, NewHomeActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        groupsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, GroupMenuActivity01.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        clubsBtn.setOnClickListener(v -> {
            // already here – no action
        });

        scheduleBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ClubsMenuActivity.this, CreateScheduleActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });

        // when you tap a club → ask to join
        listClubs.setOnItemClickListener((parent, view, position, id) -> {
            ClubItem item = clubList.get(position);
            showJoinDialog(item);
        });

        // finally, load all clubs from backend
        loadAllClubs();
    }

    // ----------------- network methods -----------------

    private void loadAllClubs() {
        String url = BASE_URL + "/clubs";

        progressBar.setVisibility(ProgressBar.VISIBLE);

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    handleClubsResponse(response);
                },
                error -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(this,
                            "Failed to load clubs: " + error.getMessage(),
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

    private void handleClubsResponse(JSONArray arr) {
        clubList.clear();
        displayList.clear();

        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject obj = arr.getJSONObject(i);
                int id = obj.getInt("id");
                String name = obj.optString("name", "Club " + id);
                String desc = obj.optString("description", "");

                ClubItem item = new ClubItem(id, name, desc);
                clubList.add(item);

                String display = name;
                if (!desc.isEmpty()) {
                    display += " – " + desc;
                }
                displayList.add(display);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        clubsAdapter.notifyDataSetChanged();
    }

    private void showJoinDialog(ClubItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Join club")
                .setMessage("Join \"" + item.name + "\"?")
                .setPositiveButton("Join", (dialog, which) -> joinClub(item.id))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------- fixed: use StringRequest, no JSON parsing -------------

    private void joinClub(int clubId) {
        String url = BASE_URL + "/clubs/" + clubId + "/join";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                // success
                response -> Toast.makeText(this,
                        "Joined club!",
                        Toast.LENGTH_SHORT).show(),
                // error
                error -> {
                    String msg = (error.getMessage() == null)
                            ? error.toString()
                            : error.getMessage();
                    Toast.makeText(this,
                            "Join failed: " + msg,
                            Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("X-NetID", username);
                return headers;
            }

            @Override
            public byte[] getBody() {
                // empty body
                return new byte[0];
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        queue.add(req);
    }
}
