package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {
    public interface ProfessorCheckCallback {
        void onResult(boolean isProfessor);
    }

    private static final String TAG = "LoginActivity";

    private EditText etUsername, etPassword;
    private Button btnLogin, btnSignup, btnForgotPassword;
    private CheckBox cbShowPassword, cbRememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        // ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate: setContentView done");

        // connect XML to Java
        etUsername      = findViewById(R.id.editTextUsername);
        etPassword      = findViewById(R.id.editTextPassword);
        btnLogin        = findViewById(R.id.buttonLogin);
        btnSignup       = findViewById(R.id.buttonSignUp);
        btnForgotPassword = findViewById(R.id.buttonForgotPassword);
        cbShowPassword  = findViewById(R.id.checkBoxShowPassword);
        cbRememberMe    = findViewById(R.id.checkBoxRememberMe);

        Log.d(TAG, "onCreate: views bound " +
                "etUsername=" + (etUsername != null) +
                ", etPassword=" + (etPassword != null) +
                ", btnLogin=" + (btnLogin != null) +
                ", btnSignup=" + (btnSignup != null) +
                ", cbShowPassword=" + (cbShowPassword != null) +
                ", cbRememberMe=" + (cbRememberMe != null));

        // remember username
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String savedUser = prefs.getString("username", "");
        String loggedInUser = prefs.getString("loggedInUsername", "");
        Log.d(TAG, "onCreate: SharedPreferences loaded. savedUser=" + savedUser
                + ", loggedInUser=" + loggedInUser);

        if (!savedUser.isEmpty()) {
            Log.d(TAG, "onCreate: pre-filling username with savedUser");
            etUsername.setText(savedUser);
            cbRememberMe.setChecked(true);
        } else {
            Log.d(TAG, "onCreate: no saved username in preferences");
        }

        // show / hide password
        cbShowPassword.setOnCheckedChangeListener((button, checked) -> {
            Log.d(TAG, "cbShowPassword onCheckedChanged: checked=" + checked);
            if (checked) {
                etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                Log.d(TAG, "cbShowPassword: setInputType VISIBLE_PASSWORD");
            } else {
                etPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                Log.d(TAG, "cbShowPassword: setInputType PASSWORD");
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // login
        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "btnLogin onClick: triggered");
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            Log.d(TAG, "btnLogin onClick: user=\"" + user + "\", passLength=" + pass.length());

            if (user.isEmpty() || pass.isEmpty()) {
                Log.w(TAG, "btnLogin onClick: empty fields. userEmpty=" +
                        user.isEmpty() + ", passEmpty=" + pass.isEmpty());
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // save remember-me username + ALWAYS remember who is logged in
            SharedPreferences.Editor editor = prefs.edit();

            if (cbRememberMe.isChecked()) {
                Log.d(TAG, "btnLogin onClick: RememberMe checked. Storing username=" + user);
                // for pre-fill on login screen
                editor.putString("username", user);
            } else {
                Log.d(TAG, "btnLogin onClick: RememberMe NOT checked. Removing saved username");
                editor.remove("username");
            }

            // always store current logged in user
            Log.d(TAG, "btnLogin onClick: Storing loggedInUsername=" + user);
            editor.putString("loggedInUsername", user);
            editor.apply();
            Log.d(TAG, "btnLogin onClick: SharedPreferences apply() complete");

            // log what we are doing
            Log.d(TAG, "btnLogin onClick: Attempting login for user=" + user);

            checkUser(user, pass, ok -> {
                Log.d(TAG, "checkUser callback: ok=" + ok);
                if (ok) {
                    Log.d(TAG, "checkUser callback: Password match, login success for user=" + user);
                    Toast.makeText(this, "Login success", Toast.LENGTH_SHORT).show();

                    // Now chain the second async call
                    isProfessor(user, isProf -> {
                        Intent intent;
                        if (isProf) {
                            Log.d(TAG, "User is professor, going to ProfessorHome");
                            intent = new Intent(this, ProfessorHome.class);
                            intent.putExtra("username", user);
                        } else {
                            Log.d(TAG, "User is student, going to NewHomeActivity with username=" + user);
                            intent = new Intent(this, NewHomeActivity.class);
                            intent.putExtra("username", user);
                        }

                        startActivity(intent);
                    });

                } else {
                    Toast.makeText(
                            this,
                            "User or password incorrect. Please try again.",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });

        // signup
        btnSignup.setOnClickListener(v -> {
            Log.d(TAG, "btnSignup onClick: Starting SignupActivity");
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        });

        btnForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(this, ForgotPassword.class);
            startActivity(intent);
        });

        Log.d(TAG, "onCreate: finished");
    }

    private void isProfessor(String username, ProfessorCheckCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/courses";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        boolean found = false;

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            String profNetId = obj.optString("professor_net_id", "");

                            if (profNetId.equals(username)) {
                                found = true;
                                break;
                            }
                        }

                        callback.onResult(found);

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onResult(false);
                    }
                },
                error -> {
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null) {
                        Log.e("VOLLEY", "GET err " + nr.statusCode +
                                " body=" + new String(nr.data));
                        Toast.makeText(this,
                                "Load failed: " + nr.statusCode,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("VOLLEY", "GET err " + error);
                        Toast.makeText(this,
                                "Network error: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    // On error, safely report "not professor"
                    callback.onResult(false);
                }
        );

        request.setShouldCache(false);
        queue.getCache().clear();
        queue.add(request);
    }

    private void checkUser(String username, String password, java.util.function.Consumer<Boolean> callback) {
        Log.d(TAG, "checkUser: entered for username=" + username +
                ", passwordLength=" + (password != null ? password.length() : -1));

        RequestQueue q = Volley.newRequestQueue(this);
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/users/" + Uri.encode(username);
        Log.d(TAG, "checkUser: URL=" + url);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                userJson -> {
                    // success: server found a user object
                    Log.d(TAG, "checkUser onResponse: GET /users/" + username + " OK: " + userJson);

                    String serverPass = userJson.optString("password", "");
                    Log.d(TAG, "checkUser onResponse: serverPassLength=" + serverPass.length());
                    boolean ok = password.equals(serverPass);
                    Log.d(TAG, "checkUser onResponse: passwordMatch=" + ok);
                    callback.accept(ok);
                },
                err -> {
                    Log.e(TAG, "checkUser onErrorResponse: " + err.toString());
                    NetworkResponse nr = err.networkResponse;

                    if (nr != null) {
                        String body = "";
                        try {
                            body = new String(nr.data);
                        } catch (Exception e) {
                            Log.e(TAG, "checkUser onErrorResponse: Exception decoding body", e);
                        }

                        // log everything so we can debug
                        Log.e(TAG, "checkUser onErrorResponse: GET /users/" + username +
                                " error code=" + nr.statusCode + " body=" + body);

                        if (nr.statusCode == 404) {
                            // user does not exist
                            Log.w(TAG, "checkUser onErrorResponse: user not found (404)");
                            callback.accept(false);
                        } else {
                            // real server problem (500, 502, 503, etc.)
                            Log.e(TAG, "checkUser onErrorResponse: server error " + nr.statusCode);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Server error " + nr.statusCode + ". Try again later.",
                                    Toast.LENGTH_SHORT
                            ).show();
                            callback.accept(false);
                        }
                    } else {
                        // network / connection problem (no response from server)
                        Log.e(TAG, "checkUser onErrorResponse: Network error, no NetworkResponse");
                        Toast.makeText(
                                LoginActivity.this,
                                "Network error. Check Wi-Fi / data.",
                                Toast.LENGTH_SHORT
                        ).show();
                        callback.accept(false);
                    }
                }
        );

        Log.d(TAG, "checkUser: disabling cache and adding request to queue");
        req.setShouldCache(false);
        q.getCache().clear();
        q.add(req);
    }

    // Optional: add more lifecycle logs if you want even more detail
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }
}