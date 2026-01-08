package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

public class GroupMenuActivity extends AppCompatActivity {

    private Button btnBack, buttonGoToCreateGroup, buttonGoToJoinGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_menu);

        // Get username from previous screen
        String username = getIntent().getStringExtra("username");

        // Link XML with Java
        btnBack = findViewById(R.id.btnBack);
        buttonGoToCreateGroup = findViewById(R.id.buttonGoToCreateGroup);
        buttonGoToJoinGroup = findViewById(R.id.buttonGoToJoinGroup);

        // Back Button
        btnBack.setOnClickListener(v -> finish());

        //  Create Group Button
        buttonGoToCreateGroup.setOnClickListener(v -> {
            Intent intent = new Intent(GroupMenuActivity.this, CreateGroupActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        // Join Group Button
        buttonGoToJoinGroup.setOnClickListener(v -> {
            Intent intent = new Intent(GroupMenuActivity.this, JoinGroupActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });
    }
}
