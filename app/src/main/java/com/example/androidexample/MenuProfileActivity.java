package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MenuProfileActivity extends AppCompatActivity {

    Button btnBack, btnEdit, btnDelete, btnLogOut, muteAll, muteSingle, unmuteSingle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_profile);

        // Connect XML to Java
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.buttonGoToEdit);
        btnDelete = findViewById(R.id.buttonGoToDelete);
        btnLogOut = findViewById(R.id.buttonLogOut);

        muteAll = findViewById(R.id.buttonMuteAll);
        muteSingle = findViewById(R.id.buttonMuteSingle);
        unmuteSingle = findViewById(R.id.buttonUnmuteSingle);

        muteAll.setOnClickListener(view -> {
            String txt = muteAll.getText().toString();
            if (txt.equals("Mute All")) {
                JSONObject body = new JSONObject();
                try {
                    body.put("message", "muteAll");
                    body.put("fromUser", getIntent().getStringExtra("username"));
                    WebSocketManager.getInstance().sendMessage(body.toString());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                muteAll.setText("Unmute All");
            } else {
                JSONObject body = new JSONObject();
                try {
                    body.put("message", "unmuteAll");
                    body.put("fromUser", getIntent().getStringExtra("username"));
                    WebSocketManager.getInstance().sendMessage(body.toString());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                muteAll.setText("Mute All");
            }
        });

        muteSingle.setOnClickListener(view -> {
            // Create an EditText for input
            EditText input = new EditText(this);
            input.setHint("Enter username to mute");
            input.setInputType(InputType.TYPE_CLASS_TEXT);

            // Build the dialog
            new AlertDialog.Builder(this)
                    .setTitle("Mute User")
                    .setMessage("Enter the username you want to mute:")
                    .setView(input)
                    .setPositiveButton("Mute", (dialog, which) -> {
                        String usernameToMute = input.getText().toString().trim();

                        if (!usernameToMute.isEmpty()) {
                            // Example: send mute command through WebSocket
                            try {
                                JSONObject json = new JSONObject();
                                json.put("message", "muteUser");
                                json.put("receiverNetId", usernameToMute);
                                json.put("fromUser", getIntent().getStringExtra("username"));

                                WebSocketManager.getInstance().sendMessage(json.toString());

                                Toast.makeText(this, "Muted " + usernameToMute, Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        unmuteSingle.setOnClickListener(view -> {
            EditText input = new EditText(this);
            input.setHint("Enter username to unmute");
            input.setInputType(InputType.TYPE_CLASS_TEXT);

            // Build the dialog
            new AlertDialog.Builder(this)
                    .setTitle("Unmute User")
                    .setMessage("Enter the username you want to unmute:")
                    .setView(input)
                    .setPositiveButton("Unmute", (dialog, which) -> {
                        String usernameToMute = input.getText().toString().trim();

                        if (!usernameToMute.isEmpty()) {
                            // Example: send mute command through WebSocket
                            try {
                                JSONObject json = new JSONObject();
                                json.put("message", "unmuteUser");
                                json.put("receiverNetId", usernameToMute);
                                json.put("fromUser", getIntent().getStringExtra("username"));

                                WebSocketManager.getInstance().sendMessage(json.toString());

                                Toast.makeText(this, "Unmuted " + usernameToMute, Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnEdit.setOnClickListener(v -> {

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

            Intent intent = new Intent(MenuProfileActivity.this, EditProfileActivity.class);
            intent.putExtra("firstName", prefs.getString("firstName", ""));
            intent.putExtra("lastName", prefs.getString("lastName", ""));
            intent.putExtra("email", prefs.getString("email", ""));
            intent.putExtra("major", prefs.getString("major", ""));
            intent.putExtra("minor", prefs.getString("minor", ""));
            intent.putExtra("gradYear", prefs.getString("gradYear", ""));
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> {
            RequestQueue queue = Volley.newRequestQueue(this);
            String username = getIntent().getStringExtra("username");
            String url = "http://coms-3090-019.class.las.iastate.edu:8080/users/" + username;

            JsonObjectRequest deleteReq = new JsonObjectRequest(
                    Request.Method.DELETE,
                    url,
                    null,
                    res -> Log.d("VOLLEY", "Deleted successfully"),
                    err -> {
                        NetworkResponse nr = err.networkResponse;
                        Log.e("VOLLEY", "HTTP " + (nr!=null?nr.statusCode:-1));
                    }) {
                @Override public Map<String,String> getHeaders() {
                    Map<String,String> h = new HashMap<>();
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };

            queue.add(deleteReq);
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });

        btnLogOut.setOnClickListener(view -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }
}