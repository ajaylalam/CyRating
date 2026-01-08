package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    EditText etUsername, etPassword, etReEnterPassword;
    Button btnSignup, btnLogin, btnForgotPassword;
    CheckBox cbShowPassword, cbRememberMe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // link XML to Java
        etUsername        = findViewById(R.id.editTextEmail);
        etPassword        = findViewById(R.id.editTextNewPassword);
        etReEnterPassword = findViewById(R.id.editTextConfirmPassword);
        btnSignup         = findViewById(R.id.btnSignup);
        btnLogin          = findViewById(R.id.btnLogin);
        btnForgotPassword = findViewById(R.id.buttonForgotPassword);
        cbShowPassword    = findViewById(R.id.checkBoxShowPassword);
        cbRememberMe      = findViewById(R.id.checkBoxTerms);

        // show or hide password
        cbShowPassword.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                etReEnterPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                etReEnterPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            etPassword.setSelection(etPassword.getText().length());
            etReEnterPassword.setSelection(etReEnterPassword.getText().length());
        });

        // signup button
        btnSignup.setOnClickListener(v -> {
            String user   = etUsername.getText().toString().trim();
            String pass   = etPassword.getText().toString();
            String rePass = etReEnterPassword.getText().toString();

            if (user.isEmpty() || pass.isEmpty() || rePass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(rePass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!cbRememberMe.isChecked()) {
                Toast.makeText(this, "Accept Terms and Conditions", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("SIGNUP", "Attempting signup for user=" + user);
            isProfessor(user, isProfessor -> {
                if (isProfessor) {
                    makeUserPost(user, pass, "PROFESSOR");
                } else {
                    makeUserPost(user, pass, "STUDENT");
                }
            });
        });

        // login button (go back to LoginActivity)
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnForgotPassword.setOnClickListener(view -> {
            Intent intent = new Intent(this, ForgotPassword.class);
            startActivity(intent);
        });
    }

    private void isProfessor(String username, LoginActivity.ProfessorCheckCallback callback) {
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

    private void makeUserPost(String user, String pass, String role) {
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "http://coms-3090-019.class.las.iastate.edu:8080/users";

        JSONObject body = new JSONObject();
        try {
            body.put("netId", user);
            body.put("password", pass);
            body.put("grade", 3);
            body.put("major", "Software Engineering");
            body.put("role", role);
        } catch (Exception ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    Log.d("SIGNUP", "Success: " + response.toString());
                    Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show();

                    // remember this user as logged in for profile / edit profile
                    SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                    loginPrefs.edit()
                            .putString("loggedInUsername", user)
                            .apply();

                    // optional: seed UserPrefs with just the email so EditProfile shows it
                    SharedPreferences userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    userPrefs.edit()
                            .putString(user + "_email", user)
                            .apply();

                    Intent intent;
                    if (role.equals("PROFESSOR")) {
                        intent = new Intent(this, ProfessorHome.class);
                        intent.putExtra("username", user);
                    } else {
                        intent = new Intent(this, ProfileActivity.class);
                        intent.putExtra("username", user);
                    }

                    startActivity(intent);
                },
                error -> {
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null) {
                        String bodyText = "";
                        try {
                            bodyText = new String(nr.data);
                        } catch (Exception ignored) {}

                        Log.e("SIGNUP", "Error: status=" + nr.statusCode +
                                " body=" + bodyText);

                        if (nr.statusCode == 409) {
                            // user already exists
                            Toast.makeText(this,
                                    "User already exists. Please log in instead.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    "Signup failed: server error " + nr.statusCode,
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("SIGNUP", "Network error: " + error.toString());
                        Toast.makeText(this,
                                "Network error. Check Wi-Fi / data.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10_000,
                0,
                1.0f
        ));

        queue.add(request);
    }
}
