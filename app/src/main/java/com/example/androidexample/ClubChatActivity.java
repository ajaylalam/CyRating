package com.example.androidexample;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class ClubChatActivity extends AppCompatActivity {

    public static final String EXTRA_CLUB_ID = "clubId";
    public static final String EXTRA_USER_ID = "userId";

    public static final String EXTRA_CLUB_NAME = "clubName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        int clubId = getIntent().getIntExtra(EXTRA_CLUB_ID, -1);
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        String clubName = getIntent().getStringExtra(EXTRA_CLUB_NAME);  // <--- NEW

        String groupId = "CLUB-" + clubId;  // backend group id

        Intent i = new Intent(ClubChatActivity.this, GroupChatActivity.class);
        i.putExtra(GroupChatActivity.EXTRA_GROUP_ID, groupId);
        i.putExtra(GroupChatActivity.EXTRA_USERNAME, userId);

        // if we got a real name, use it; otherwise fall back to "Club 8"
        if (clubName != null && !clubName.isEmpty()) {
            i.putExtra(GroupChatActivity.EXTRA_GROUP_NAME, clubName);
        } else {
            i.putExtra(GroupChatActivity.EXTRA_GROUP_NAME, "Club " + clubId);
        }

        i.putExtra(GroupChatActivity.EXTRA_BACK_TARGET, "clubs_menu");
        startActivity(i);
        finish();
    }

}
