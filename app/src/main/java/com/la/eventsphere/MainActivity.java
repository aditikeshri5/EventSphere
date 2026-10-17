package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnContinueAsUser;
    private Button btnContinueAsOrganizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Connect the buttons
        btnContinueAsUser = findViewById(R.id.btnContinueAsUser);
        btnContinueAsOrganizer =
                findViewById(R.id.btnContinueAsOrganizer);

        // Open user login
        btnContinueAsUser.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    LoginActivity.class
            );
            startActivity(intent);
        });

        // Open organizer login
        btnContinueAsOrganizer.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    OrganizerLoginActivity.class
            );
            startActivity(intent);
        });
    }
}