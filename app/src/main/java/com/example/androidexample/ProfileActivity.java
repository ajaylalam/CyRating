package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    EditText firstName, lastName, email, major, minor, gradYear;
    Button createProfileBtn;

    private SharedPreferences prefs;
    private String keyPrefix;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // connect XML with Java
        firstName = findViewById(R.id.editTextFirstName);
        lastName  = findViewById(R.id.editTextLastName);
        email     = findViewById(R.id.editTextEmail);
        major     = findViewById(R.id.editTextMajor);
        minor     = findViewById(R.id.editTextMinor);
        gradYear  = findViewById(R.id.editTextGradYear);
        createProfileBtn = findViewById(R.id.btnCreateProfile);

        // figure out which user is active (same logic as EditProfileActivity)
        currentUser = getIntent().getStringExtra("username");
        if (currentUser == null || currentUser.isEmpty()) {
            SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            currentUser = loginPrefs.getString("loggedInUsername", "");
        }

        keyPrefix = (currentUser == null ? "" : currentUser + "_");

        // pre-fill email with username / netid
        email.setText(currentUser);

        // if user already has some saved profile data, show it
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        firstName.setText(prefs.getString(keyPrefix + "firstName", ""));
        lastName.setText(prefs.getString(keyPrefix + "lastName", ""));
        major.setText(prefs.getString(keyPrefix + "major", ""));
        minor.setText(prefs.getString(keyPrefix + "minor", ""));
        gradYear.setText(prefs.getString(keyPrefix + "gradYear", ""));

        // when user clicks Create Profile button
        createProfileBtn.setOnClickListener(v -> {
            String fName = firstName.getText().toString().trim();
            String lName = lastName.getText().toString().trim();
            String eMail = email.getText().toString().trim();
            String maj   = major.getText().toString().trim();
            String min   = minor.getText().toString().trim();
            String grad  = gradYear.getText().toString().trim();

            // simple validation
            if (fName.isEmpty() || lName.isEmpty() || eMail.isEmpty() || maj.isEmpty()) {
                Toast.makeText(ProfileActivity.this,
                        "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // SAVE profile data for THIS user (same keys as EditProfileActivity)
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(keyPrefix + "firstName", fName);
            editor.putString(keyPrefix + "lastName",  lName);
            editor.putString(keyPrefix + "email",     eMail);
            editor.putString(keyPrefix + "major",     maj);
            editor.putString(keyPrefix + "minor",     min);
            editor.putString(keyPrefix + "gradYear",  grad);
            editor.putBoolean(keyPrefix + "profileCreated", true);
            editor.apply();

            Toast.makeText(ProfileActivity.this,
                    "Profile Created Successfully!", Toast.LENGTH_SHORT).show();

            // Go to Home page (change to ClassEnrollActivity if you want that instead)
            Intent intent = new Intent(ProfileActivity.this, ClassEnrollActivity.class);
            intent.putExtra("username", currentUser);
            startActivity(intent);
            finish();
        });
    }
}
