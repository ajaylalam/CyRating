package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class ForgotPassword extends AppCompatActivity {
    public interface UsernameCallback {
        void onResult(boolean exists);
    }
    Button forgotPassword, login, signUp;
    EditText user, password, confirmPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        forgotPassword = findViewById(R.id.buttonForgotPassword);
        login = findViewById(R.id.btnLogin);
        signUp = findViewById(R.id.btnSignup);

        user = findViewById(R.id.editTextEmail);
        password = findViewById(R.id.editTextNewPassword);
        confirmPassword = findViewById(R.id.editTextConfirmPassword);

        forgotPassword.setOnClickListener(view -> {
            confirmUsername(user.getText().toString(), exists -> {
                if (exists && password.getText().toString().equals(confirmPassword.getText().toString())) {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.putExtra("username", user.getText().toString());
                    startActivity(intent);
                } else if (!password.getText().toString().equals(confirmPassword.getText().toString())) {
                    Toast.makeText(this, "Make sure passwords match.", Toast.LENGTH_SHORT).show();
                } else if (user.getText().toString().isEmpty() || password.getText().toString().isEmpty() || confirmPassword.getText().toString().isEmpty()) {
                    Toast.makeText(this, "Need to fill out all forms.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Username doesn't exist. Sign up instead.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        signUp.setOnClickListener(view -> {
            Intent intent = new Intent(this, SignupActivity.class);
            startActivity(intent);
        });

        login.setOnClickListener(view -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void confirmUsername(String username, UsernameCallback callback) {
        String url = "http://coms-3090-019.class.las.iastate.edu:8080/users/" + username;

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // If the user exists, backend should return 200 with JSON
                    System.out.println(response);
                    try {
                        // Replace old password
                        response.put("password", password.getText().toString());
                        JsonObjectRequest putRequest = new JsonObjectRequest(
                                Request.Method.PUT,
                                url,
                                response,
                                newResponse -> Log.d("UPDATE", "Password updated successfully!"),
                                error -> Log.e("UPDATE", "Error: " + error.toString())
                        );

                        Volley.newRequestQueue(this).add(putRequest);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    callback.onResult(true);
                },
                error -> {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                        // User NOT found
                        callback.onResult(false);
                    } else {
                        // Unexpected error (server down, CORS, etc.)
                        callback.onResult(false);
                    }
                }
        );

        queue.add(request);
    }
}
