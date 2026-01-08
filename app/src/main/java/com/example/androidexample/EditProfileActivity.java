package com.example.androidexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {

    EditText firstName, lastName, email, major, minor, gradYear;
    Button saveChangesBtn, backBtn;

    private SharedPreferences prefs;
    private String keyPrefix;
    private String currentUser;   // used in lambda + intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Connect XML with Java
        firstName = findViewById(R.id.editTextFirstName);
        lastName  = findViewById(R.id.editTextLastName);
        email     = findViewById(R.id.editTextEmail);
        major     = findViewById(R.id.editTextMajor);
        minor     = findViewById(R.id.editTextMinor);
        gradYear  = findViewById(R.id.editTextGradYear);
        saveChangesBtn = findViewById(R.id.btnSaveChanges);
        backBtn        = findViewById(R.id.btnBack);

        // figure out which user is active (username/netid from Intent or LoginPrefs)
        currentUser = getIntent().getStringExtra("username");
        if (currentUser == null || currentUser.isEmpty()) {
            SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            currentUser = loginPrefs.getString("loggedInUsername", "");
        }

        // prefix for this user's keys in UserPrefs
        keyPrefix = (currentUser == null ? "" : currentUser + "_");

        // set email box (read-only in XML)
        email.setText(currentUser);

        // Load saved profile data for THIS user
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        firstName.setText(prefs.getString(keyPrefix + "firstName", ""));
        lastName.setText(prefs.getString(keyPrefix + "lastName", ""));
        // email already set
        major.setText(prefs.getString(keyPrefix + "major", ""));
        minor.setText(prefs.getString(keyPrefix + "minor", ""));
        gradYear.setText(prefs.getString(keyPrefix + "gradYear", ""));

        // Save button
        saveChangesBtn.setOnClickListener(v -> {
            String fName = firstName.getText().toString().trim();
            String lName = lastName.getText().toString().trim();
            String eMail = email.getText().toString().trim();
            String maj   = major.getText().toString().trim();
            String min   = minor.getText().toString().trim();
            String grad  = gradYear.getText().toString().trim();

            if (fName.isEmpty() || lName.isEmpty() || eMail.isEmpty() || maj.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save updated info locally for THIS user
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(keyPrefix + "firstName", fName);
            editor.putString(keyPrefix + "lastName",  lName);
            editor.putString(keyPrefix + "email",     eMail);
            editor.putString(keyPrefix + "major",     maj);
            editor.putString(keyPrefix + "minor",     min);
            editor.putString(keyPrefix + "gradYear",  grad);
            editor.apply();

            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();

            // Go back to Menu Profile
            Intent intentBack = new Intent(EditProfileActivity.this, MenuProfileActivity.class);
            intentBack.putExtra("username", currentUser);
            startActivity(intentBack);
            finish();
        });

        // Back button
        backBtn.setOnClickListener(v -> finish());
    }
}
