package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Single group / course / club chat screen.
 */
public class GroupChatActivity extends AppCompatActivity {

    public static final String EXTRA_GROUP_ID    = "groupId";
    public static final String EXTRA_USERNAME    = "username";
    public static final String EXTRA_GROUP_NAME  = "groupName";
    public static final String EXTRA_BACK_TARGET = "backTarget";

    private RecyclerView rv;
    private EditText et;
    private Button btnSend;
    private TextView tvTyping;
    private TextView groupNameText;
    private Button btnBack;

    private final Handler ui = new Handler();
    private final WebSocketService ws = new WebSocketService();
    private ChatMessageAdapter adapter;

    private String groupId;
    private String username;
    private String backTarget;

    private final Runnable typingHide = () -> {
        tvTyping.setVisibility(TextView.GONE);
        tvTyping.setText("");
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        rv            = findViewById(R.id.rvChat);
        et            = findViewById(R.id.etMessage);
        btnSend       = findViewById(R.id.btnSend);
        tvTyping      = findViewById(R.id.tvTyping);
        btnBack       = findViewById(R.id.btnBack);
        groupNameText = findViewById(R.id.groupName);

        adapter = new ChatMessageAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // ----- read extras -----
        Intent intent = getIntent();
        groupId    = intent.getStringExtra(EXTRA_GROUP_ID);
        username   = intent.getStringExtra(EXTRA_USERNAME);
        String groupName = intent.getStringExtra(EXTRA_GROUP_NAME);
        backTarget = intent.getStringExtra(EXTRA_BACK_TARGET);

        if (groupId == null || groupId.isEmpty()) groupId = "COMS309";
        if (username == null || username.isEmpty()) username = "Student";

        if (groupName != null && !groupName.isEmpty()) {
            groupNameText.setText(groupName);
        } else {
            groupNameText.setText(groupId);
        }

        // 👆 NEW: tap the title to open details
        groupNameText.setOnClickListener(v -> openGroupDetails());

        // 1) Load club chat history IF this is a club (CLUB-*)
        loadClubChatHistoryIfNeeded();

        // 2) Then open websocket connection
        connectSocket();

        // ----- send button -----
        btnSend.setOnClickListener(v -> {
            String text = et.getText().toString().trim();
            if (text.isEmpty()) return;

            // send over WebSocket
            ws.sendMessage(text);

            // show immediately in UI as outgoing
            adapter.add(new ChatMessage(
                    username,
                    text,
                    ChatMessage.Kind.OUTGOING,
                    System.currentTimeMillis()
            ));
            rv.scrollToPosition(adapter.getItemCount() - 1);
            et.setText("");
        });

        // back button in UI
        btnBack.setOnClickListener(v -> handleBackNavigation());
    }

    /** Only clubs (groupId starting with "CLUB-") use REST history. */
    private void loadClubChatHistoryIfNeeded() {
        if (groupId == null || !groupId.startsWith("CLUB-")) {
            // not a club chat -> skip, don't affect groups/classes
            return;
        }

        // FIXED: ApiConfig.historyUrl(groupId, size, page)
        String url = ApiConfig.historyUrl(groupId, 50, 0); // size=50, page=0
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String sender = obj.optString("sender", "unknown");
                            String text   = obj.optString("content", "");
                            long timestamp = obj.optLong("timestamp",
                                    System.currentTimeMillis());

                            ChatMessage.Kind kind = sender.equals(username)
                                    ? ChatMessage.Kind.OUTGOING
                                    : ChatMessage.Kind.INCOMING;

                            ChatMessage msg = new ChatMessage(
                                    sender,
                                    text,
                                    kind,
                                    timestamp
                            );
                            adapter.add(msg);
                        }

                        rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // silent fail for demo
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("X-NetID", username);
                return headers;
            }
        };

        queue.add(request);
    }

    /** Open the WebSocket for this group. */
    private void connectSocket() {
        ws.connect(groupId, username, new WebSocketService.MessageListener() {
            @Override public void onOpen() {
                runOnUiThread(() ->
                        Toast.makeText(GroupChatActivity.this,
                                "Connected to chat", Toast.LENGTH_SHORT).show());
            }

            @Override public void onChat(ChatMessage message) {
                // server echoes everyone; skip our own OUTGOING (we already added it)
                if (message.kind == ChatMessage.Kind.OUTGOING) return;
                runOnUiThread(() -> {
                    adapter.add(message);
                    rv.scrollToPosition(adapter.getItemCount() - 1);
                });
            }

            @Override public void onTyping(String sender) {
                if (sender == null || sender.equals(username)) return;
                runOnUiThread(() -> {
                    tvTyping.setText(sender + " is typing...");
                    tvTyping.setVisibility(TextView.VISIBLE);
                    ui.removeCallbacks(typingHide);
                    ui.postDelayed(typingHide, 1500);
                });
            }

            @Override public void onSystem(String text, @Nullable Integer online) {
                runOnUiThread(() -> {
                    String msg = text;
                    if (online != null) {
                        msg = text + " (" + online + " online)";
                    }
                    adapter.add(new ChatMessage(
                            "system",
                            msg,
                            ChatMessage.Kind.SYSTEM,
                            System.currentTimeMillis()
                    ));
                });
            }

            @Override public void onClosed(int code, String reason) {
                runOnUiThread(() ->
                        Toast.makeText(GroupChatActivity.this,
                                "Chat disconnected", Toast.LENGTH_SHORT).show());
            }

            @Override public void onError(Throwable t) {
                runOnUiThread(() ->
                        Toast.makeText(GroupChatActivity.this,
                                "Chat error: " + t.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    /** Open the group / club details screen when title is tapped. */
    private void openGroupDetails() {
        Intent i = new Intent(GroupChatActivity.this, GroupDetailsActivity.class);
        i.putExtra("groupId", groupId);
        i.putExtra("groupName", groupNameText.getText().toString());
        i.putExtra("username", username);
        startActivity(i);
    }

    /** Decide where "back" should go based on backTarget. */
    private void handleBackNavigation() {
        Intent backIntent;
        if ("clubs_menu".equals(backTarget)) {
            backIntent = new Intent(GroupChatActivity.this, ClubsMenuActivity.class);
            backIntent.putExtra("username", username);
        } else {
            // default: groups
            backIntent = new Intent(GroupChatActivity.this, GroupMenuActivity01.class);
            backIntent.putExtra("username", username);
        }
        startActivity(backIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ws.close();
        ui.removeCallbacks(typingHide);
    }

    @Override
    public void onBackPressed() {
        handleBackNavigation();
    }
}
