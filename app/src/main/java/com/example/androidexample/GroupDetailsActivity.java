package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class GroupDetailsActivity extends AppCompatActivity {

    private EditText editGroupName, editGroupDesc;
    private Button btnSaveChanges, btnBack, btnClearChat, btnExitGroup;
    private Button btnMeetingMember;

    private static final String SP_NAME = "GroupData";

    private String groupId = "demo-1";
    private String username;        // netId
    private boolean isClub = false; // true if groupId like "CLUB-3"
    private int clubId = -1;        // numeric club id for club endpoints

    // per-group SharedPreferences keys
    private String spKeyName;
    private String spKeyDesc;
    private String spKeyClearFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        // find views
        editGroupName   = findViewById(R.id.editGroupName);
        editGroupDesc   = findViewById(R.id.editGroupDesc);
        btnSaveChanges  = findViewById(R.id.btnSaveChanges);
        btnBack         = findViewById(R.id.btnBack);
        btnClearChat    = findViewById(R.id.btnClearChat);
        btnExitGroup    = findViewById(R.id.btnExitGroup);
        btnMeetingMember = findViewById(R.id.btnMeetingMember);

        // -------- read extras --------
        Intent intent = getIntent();

        // groupId (can be "CLUB-3" or normal)
        String gidStr = intent.getStringExtra("groupId");
        if (gidStr != null && !gidStr.equals("null") && !gidStr.isEmpty()) {
            groupId = gidStr;
        } else {
            int gidInt = intent.getIntExtra("groupId", -1);
            if (gidInt != -1) groupId = String.valueOf(gidInt);
        }

        // username / netId
        username = intent.getStringExtra("username");
        if (username == null || username.isEmpty()) {
            username = intent.getStringExtra("netId");
        }

        // detect if this is a club (groupId starts with "CLUB-")
        if (groupId != null && groupId.startsWith("CLUB-")) {
            isClub = true;
            try {
                String num = groupId.substring("CLUB-".length());
                clubId = Integer.parseInt(num);
            } catch (Exception e) {
                clubId = -1;
            }
        } else {
            // fallback: maybe clubId passed separately
            int cid = intent.getIntExtra("clubId", -1);
            if (cid > 0) {
                isClub = true;
                clubId = cid;
            }
        }

        // ----- per-group preference keys -----
        spKeyName      = "groupName_" + groupId;
        spKeyDesc      = "groupDesc_" + groupId;
        spKeyClearFlag = "clearChatRequested_" + groupId;

        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);

        // default values from Intent (what you passed from chat / list)
        String defaultName = intent.getStringExtra("groupName");
        String defaultDesc = intent.getStringExtra("groupDesc");

        // load saved name/desc for THIS groupId (if we saved before)
        String name  = sp.getString(spKeyName, defaultName);
        String desc  = sp.getString(spKeyDesc, defaultDesc);

        if (name != null) editGroupName.setText(name);
        if (desc != null) editGroupDesc.setText(desc);

        // -------- SAVE CHANGES (per-group) --------
        btnSaveChanges.setOnClickListener(v -> {
            String newName = editGroupName.getText().toString().trim();
            String newDesc = editGroupDesc.getText().toString().trim();

            if (newName.isEmpty() || newDesc.isEmpty()) {
                Toast.makeText(this, "Enter both name and description", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor ed = sp.edit();
            ed.putString(spKeyName, newName);   // only for this groupId
            ed.putString(spKeyDesc, newDesc);   // only for this groupId
            ed.apply();

            Toast.makeText(this, "Group details saved", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });

        // BACK (no save)
        btnBack.setOnClickListener(v -> finish());

        // CLEAR CHAT (flag per group)
        btnClearChat.setOnClickListener(v -> {
            SharedPreferences.Editor ed = sp.edit();
            ed.putBoolean(spKeyClearFlag, true);
            ed.apply();
            Toast.makeText(this, "Chat cleared (local demo)", Toast.LENGTH_SHORT).show();
        });

        // EXIT GROUP
        btnExitGroup.setOnClickListener(v -> {
            if (isClub && clubId > 0 && username != null && !username.isEmpty()) {
                leaveClub();   // call backend DELETE /clubs/{clubId}/leave
            } else {
                goHome();      // fallback: old behavior
            }
        });

        // MEETINGS button
        btnMeetingMember.setOnClickListener(v -> {
            if (isClub && clubId > 0) {
                Intent i2 = new Intent(GroupDetailsActivity.this, ClubMeetingsActivity.class);
                i2.putExtra("clubId", clubId);
                i2.putExtra("username", username);
                i2.putExtra("groupName", editGroupName.getText().toString());
                startActivity(i2);
            } else {
                Toast.makeText(this, "Meetings are only available for clubs.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // -------- helpers --------

    private void goHome() {
        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    /** Call DELETE /clubs/{clubId}/leave with X-NetID header */
    private void leaveClub() {
        if (clubId <= 0) {
            goHome();
            return;
        }

        String url = ApiConfig.BASE_HTTP + "/clubs/" + clubId + "/leave";
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest req = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Toast.makeText(this, "Left club", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, ClubsMenuActivity.class);
                    i.putExtra("username", username);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();
                },
                error -> {
                    Toast.makeText(this, "Failed to leave club", Toast.LENGTH_SHORT).show();
                    goHome();
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
}
