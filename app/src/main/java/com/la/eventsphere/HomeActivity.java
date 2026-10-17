package com.la.eventsphere;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout navHome;
    private LinearLayout navSaved;
    private LinearLayout navMyEvents;
    private LinearLayout navProfile;

    private ImageView iconHome;
    private ImageView iconSaved;
    private ImageView iconMyEvents;
    private ImageView iconProfile;

    private TextView textHome;
    private TextView textSaved;
    private TextView textMyEvents;
    private TextView textProfile;

    private final int PURPLE = 0xFF5033D8;
    private final int GREY = 0xFF777777;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeViews();
        setupNavigation();

        // Home is selected by default
        if (savedInstanceState == null) {
            openFragment(new HomeFragment());
            selectNavigation(navHome);
        }
    }

    private void initializeViews() {

        navHome = findViewById(R.id.navHome);
        navSaved = findViewById(R.id.navSaved);
        navMyEvents = findViewById(R.id.navMyEvents);
        navProfile = findViewById(R.id.navProfile);

        iconHome = findViewById(R.id.iconHome);
        iconSaved = findViewById(R.id.iconSaved);
        iconMyEvents = findViewById(R.id.iconMyEvents);
        iconProfile = findViewById(R.id.iconProfile);

        textHome = findViewById(R.id.textHome);
        textSaved = findViewById(R.id.textSaved);
        textMyEvents = findViewById(R.id.textMyEvents);
        textProfile = findViewById(R.id.textProfile);
    }

    private void setupNavigation() {

        navHome.setOnClickListener(v -> {

            openFragment(new HomeFragment());
            selectNavigation(navHome);

        });

        navSaved.setOnClickListener(v -> {

            openFragment(new SavedFragment());
            selectNavigation(navSaved);

        });

        navMyEvents.setOnClickListener(v -> {

            openFragment(new MyEventsFragment());
            selectNavigation(navMyEvents);

        });

        navProfile.setOnClickListener(v -> {

            openFragment(new ProfileFragment());
            selectNavigation(navProfile);

        });
    }

    private void openFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void selectNavigation(LinearLayout selectedNav) {

        // Reset everything to grey
        iconHome.setColorFilter(GREY);
        iconSaved.setColorFilter(GREY);
        iconMyEvents.setColorFilter(GREY);
        iconProfile.setColorFilter(GREY);

        textHome.setTextColor(GREY);
        textSaved.setTextColor(GREY);
        textMyEvents.setTextColor(GREY);
        textProfile.setTextColor(GREY);

        textHome.setTypeface(null, android.graphics.Typeface.NORMAL);
        textSaved.setTypeface(null, android.graphics.Typeface.NORMAL);
        textMyEvents.setTypeface(null, android.graphics.Typeface.NORMAL);
        textProfile.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Highlight selected item
        if (selectedNav == navHome) {

            iconHome.setColorFilter(PURPLE);
            textHome.setTextColor(PURPLE);
            textHome.setTypeface(null, android.graphics.Typeface.BOLD);

        } else if (selectedNav == navSaved) {

            iconSaved.setColorFilter(PURPLE);
            textSaved.setTextColor(PURPLE);
            textSaved.setTypeface(null, android.graphics.Typeface.BOLD);

        } else if (selectedNav == navMyEvents) {

            iconMyEvents.setColorFilter(PURPLE);
            textMyEvents.setTextColor(PURPLE);
            textMyEvents.setTypeface(null, android.graphics.Typeface.BOLD);

        } else if (selectedNav == navProfile) {

            iconProfile.setColorFilter(PURPLE);
            textProfile.setTextColor(PURPLE);
            textProfile.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }
}