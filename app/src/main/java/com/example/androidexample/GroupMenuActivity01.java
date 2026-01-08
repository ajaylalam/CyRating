package com.example.androidexample;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class GroupMenuActivity01 extends AppCompatActivity {

    private static final String BASE_URL = "http://coms-3090-019.class.las.iastate.edu:8080";

    // real logged-in username / NetId
    private String username;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ListView listView;
    private ProgressBar progressBar;
    private TextView btnEnrolledGroups;
    private TextView btnRefreshGroups;
    private TextView groupSubtitle;

    private final ArrayList<GroupItem> groups = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_groups);

        // Get username from Intent or SharedPreferences
        username = getIntent().getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("username", "");
        }
        Log.d("GroupMenu", "Logged in username = " + username);

        listView          = findViewById(R.id.listGroups);
        progressBar       = findViewById(R.id.progressBar);
        btnEnrolledGroups = findViewById(R.id.btnEnrolledGroups);
        btnRefreshGroups  = findViewById(R.id.btnRefreshGroups);
        groupSubtitle     = findViewById(R.id.group_subtitle);
        TextView backBtn  = findViewById(R.id.back_btn);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        listView.setAdapter(adapter);

        // Tabs
        btnRefreshGroups.setOnClickListener(v -> fetchGroups());
        btnEnrolledGroups.setOnClickListener(v -> fetchEnrolledGroups());

        backBtn.setOnClickListener(v -> finish());

        listView.setOnItemClickListener((parent, view, position, id) -> {
            GroupItem item = groups.get(position);
            new AlertDialog.Builder(GroupMenuActivity01.this)
                    .setTitle("Join Group")
                    .setMessage("Do you want to join " + item.name + " ?")
                    .setPositiveButton("Join", (d, w) -> joinGroup(item.groupId))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Load all groups first
        fetchGroups();

        // Bottom nav
        findViewById(R.id.classes_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, ClassesPageActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        findViewById(R.id.home_btn).setOnClickListener(v -> {
            Intent intent = new Intent(this, NewHomeActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        findViewById(R.id.create_schedule).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateScheduleActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        findViewById(R.id.profile_btn).setOnClickListener(view -> {
            Intent intent = new Intent(this, MenuProfileActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        TextView clubsBtn = findViewById(R.id.clubs);
        clubsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(GroupMenuActivity01.this, ClubsMenuActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
            finish();
        });
    }

    private void setLoading(boolean loading) {
        mainHandler.post(() -> {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            btnEnrolledGroups.setEnabled(!loading);
            btnRefreshGroups.setEnabled(!loading);
            listView.setEnabled(!loading);
        });
    }

    /** Refresh tab → all available groups */
    private void fetchGroups() {
        setLoading(true);
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "/groups");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int code = conn.getResponseCode();
                if (code != 200) throw new Exception("HTTP " + code);

                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONArray arr = new JSONArray(sb.toString());
                ArrayList<GroupItem> fetched = new ArrayList<>();
                ArrayList<String> labels = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    String id = o.optString("groupId");
                    if (id == null || id.isEmpty() || "null".equals(id)) {
                        id = o.optString("id");
                    }

                    String name = o.optString("name", id);
                    String owner = o.optString("ownerNetId", "");

                    fetched.add(new GroupItem(id, name, owner));
                    labels.add(name + " (" + id + ")");
                }

                mainHandler.post(() -> {
                    groups.clear();
                    groups.addAll(fetched);
                    adapter.clear();
                    adapter.addAll(labels);
                    adapter.notifyDataSetChanged();

                    groupSubtitle.setText("Available Course Groups");
                    listView.setSelection(0);

                    setLoading(false);
                    Log.i("GroupMenu", "Groups loaded: " + groups.size());
                });

            } catch (Exception e) {
                Log.e("GroupMenu", "fetchGroups failed", e);
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(GroupMenuActivity01.this,
                            "Failed to load groups " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /** Enrolled tab → only groups this user is a member of */
    private void fetchEnrolledGroups() {
        setLoading(true);
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "/groups/enrolled");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                // identify the caller
                conn.setRequestProperty("X-NetID", username);

                int code = conn.getResponseCode();

                // read from input or error stream depending on status
                BufferedReader br;
                if (code >= 200 && code < 300) {
                    br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                if (code != 200) {
                    // log full body so we can see backend’s error message
                    Log.e("GroupMenu", "/groups/enrolled HTTP " + code + " body=" + sb);
                    throw new Exception("HTTP " + code);
                }

                // normal success parsing
                JSONArray arr = new JSONArray(sb.toString());
                ArrayList<GroupItem> fetched = new ArrayList<>();
                ArrayList<String> labels = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    String id = o.optString("groupId");
                    if (id == null || id.isEmpty() || "null".equals(id)) {
                        id = o.optString("id");
                    }

                    String name = o.optString("name", id);
                    String owner = o.optString("ownerNetId", "");

                    fetched.add(new GroupItem(id, name, owner));
                    labels.add(name + " (" + id + ")");
                }

                mainHandler.post(() -> {
                    groups.clear();
                    groups.addAll(fetched);
                    adapter.clear();
                    adapter.addAll(labels);
                    adapter.notifyDataSetChanged();

                    groupSubtitle.setText("Your Enrolled Groups");
                    listView.setSelection(0);

                    setLoading(false);
                    Log.i("GroupMenu", "Enrolled groups loaded: " + groups.size());
                });

            } catch (Exception e) {
                Log.e("GroupMenu", "fetchEnrolledGroups failed", e);
                mainHandler.post(() -> {
                    setLoading(false);
                    Toast.makeText(GroupMenuActivity01.this,
                            "Failed to load enrolled groups " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void joinGroup(String groupId) {
        setLoading(true);
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + "/groups/" + groupId.replace(" ", "%20") + "/join");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-NetID", username);
                conn.setDoOutput(true);

                try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                    dos.flush();
                }

                int code = conn.getResponseCode();
                if (code != 200 && code != 201) throw new Exception("HTTP " + code);

                // read JSON body
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                boolean alreadyMember = false;
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(sb.toString());
                    alreadyMember = obj.optBoolean("alreadyMember", false);
                } catch (Exception ignore) { }

                boolean finalAlreadyMember = alreadyMember;

                mainHandler.post(() -> {
                    if (finalAlreadyMember) {
                        Toast.makeText(GroupMenuActivity01.this,
                                "You already joined this group",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(GroupMenuActivity01.this,
                                "Joined " + groupId,
                                Toast.LENGTH_SHORT).show();
                    }

                    // open chat either way
                    GroupItem item = null;
                    for (GroupItem g : groups) {
                        if (g.groupId.equals(groupId)) item = g;
                    }

                    Intent intent = new Intent(GroupMenuActivity01.this, GroupChatActivity.class);
                    intent.putExtra("groupId", groupId);
                    intent.putExtra("groupName", item != null ? item.name : "Group Chat");
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                Log.e("GroupMenu", "joinGroup failed", e);
                mainHandler.post(() -> {
                    Toast.makeText(GroupMenuActivity01.this,
                            "Join failed " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                setLoading(false);
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static class GroupItem {
        final String groupId;
        final String name;
        final String ownerNetId;

        GroupItem(String id, String name, String owner) {
            this.groupId = id;
            this.name = name;
            this.ownerNetId = owner;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(GroupMenuActivity01.this, NewHomeActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }
}