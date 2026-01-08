package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JoinGroupActivity extends AppCompatActivity {

    private ListView groupListView;
    private TextView btnBack;

    private final ArrayList<String> groupNames = new ArrayList<>();
    private final ArrayList<String> groupIds   = new ArrayList<>();

    private RequestQueue queue;
    private static final String BASE_URL =
            "http://coms-3090-019.class.las.iastate.edu:8080/groups";

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_page);   // <-- new groups layout

        // get username that was passed into this Activity
        username = getIntent().getStringExtra("username");

        groupListView = findViewById(R.id.listGroups);   // <-- matches XML id
        btnBack       = findViewById(R.id.back_btn);     // <-- matches XML id
        queue         = Volley.newRequestQueue(this);

        // Load all available groups from backend
        fetchGroups();

        // When user clicks on a group
        groupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                String selectedGroupId   = groupIds.get(position);
                String selectedGroupName = groupNames.get(position);
                joinGroup(selectedGroupId, selectedGroupName);
            }
        });

        // Back button
        btnBack.setOnClickListener(v -> finish());
    }

    /** GET /groups to list all existing groups. */
    private void fetchGroups() {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                BASE_URL,
                null,
                response -> {
                    groupNames.clear();
                    groupIds.clear();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject group = response.getJSONObject(i);
                            groupNames.add(group.getString("groupName"));
                            groupIds.add(group.getString("groupId"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_list_item_1,
                            groupNames
                    );
                    groupListView.setAdapter(adapter);
                },
                error -> Toast.makeText(this,
                        "Failed to load groups", Toast.LENGTH_SHORT).show()
        );
        queue.add(request);
    }

    /** POST /groups/{groupId}/join, then open GroupChatActivity. */
    private void joinGroup(String groupId, String groupName) {
        String url = BASE_URL + "/" + groupId + "/join";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    Toast.makeText(this,
                            "Joined " + groupName + "!", Toast.LENGTH_SHORT).show();

                    // Open group chat for this group
                    Intent intent = new Intent(JoinGroupActivity.this, GroupChatActivity.class);
                    intent.putExtra(GroupChatActivity.EXTRA_GROUP_ID, groupId);
                    intent.putExtra(GroupChatActivity.EXTRA_GROUP_NAME, groupName);
                    intent.putExtra(GroupChatActivity.EXTRA_USERNAME, username);
                    startActivity(intent);
                },
                error -> Toast.makeText(this,
                        "Error joining group!", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-NetID", username);   // backend uses this
                return headers;
            }
        };

        queue.add(request);
    }
}
