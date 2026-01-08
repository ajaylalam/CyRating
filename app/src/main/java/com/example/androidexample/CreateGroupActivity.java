package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class CreateGroupActivity extends AppCompatActivity {

    private static final String BASE_URL =
            "http://coms-3090-019.class.las.iastate.edu:8080";


    private EditText editTextGroupName, editTextGroupDesc;
    private Button buttonCreateGroup, btnBack;
    private String username;

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // username coming from ClubsMenuActivity
        username = getIntent().getStringExtra("username");

        // init Volley
        queue = Volley.newRequestQueue(this);

        // link views (use your existing IDs from XML)
        editTextGroupName = findViewById(R.id.editTextGroupName);
        editTextGroupDesc = findViewById(R.id.editTextGroupDesc);
        buttonCreateGroup = findViewById(R.id.buttonCreateGroup);
        btnBack = findViewById(R.id.btnBack);

        // back goes to Clubs page
        btnBack.setOnClickListener(v -> {
            Intent i = new Intent(CreateGroupActivity.this, ClubsMenuActivity.class);
            i.putExtra("username", username);
            startActivity(i);
            finish();
        });

        // create button
        buttonCreateGroup.setOnClickListener(v -> createClub());
    }

    private void createClub() {
        String name = editTextGroupName.getText().toString().trim();
        String desc = editTextGroupDesc.getText().toString().trim();

        if (name.isEmpty()) {
            editTextGroupName.setError("Enter club name");
            return;
        }

        String url = BASE_URL + "/clubs";

        JSONObject body = new JSONObject();
        try {
            // keys should match what backend expects
            body.put("name", name);
            body.put("description", desc);
            // keep this only if your backend uses it
            body.put("ownerNetId", username);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Create failed: bad JSON", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    Toast.makeText(this, "Club created!", Toast.LENGTH_SHORT).show();

                    // After create, go back to Clubs menu
                    Intent intent = new Intent(CreateGroupActivity.this, ClubsMenuActivity.class);
                    intent.putExtra("username", username);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    String msg = "Create failed: network error";
                    if (error.networkResponse != null) {
                        msg += " (code " + error.networkResponse.statusCode + ")";
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("X-NetID", username);          // VERY IMPORTANT
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(req);
    }

}