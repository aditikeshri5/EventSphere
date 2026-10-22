
package com.la.eventsphere;

import android.os.Bundle;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class OrganizerActivity extends AppCompatActivity {

    private LinearLayout navDashboard;
    private LinearLayout navEvents;
    private LinearLayout navRegistrations;
    private LinearLayout navProfile;

    private ImageView iconDashboard;
    private ImageView iconEvents;
    private ImageView iconRegistrations;
    private ImageView iconProfile;

    private TextView textDashboard;
    private TextView textEvents;
    private TextView textRegistrations;
    private TextView textProfile;

    private final int PURPLE = 0xFF5033D8;
    private final int GREY = 0xFF777386;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer);

        initializeViews();
        setupNavigation();

        findViewById(R.id.btnOrganizerNotifications).setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "Organizer notifications will be added later.",
                        Toast.LENGTH_SHORT
                ).show()
        );

        if (savedInstanceState == null) {
            openFragment(new OrganizerDashboardFragment());
            selectNavigation(navDashboard);
        }
    }

    private void initializeViews() {

        navDashboard = findViewById(R.id.navOrganizerDashboard);
        navEvents = findViewById(R.id.navOrganizerEvents);
        navRegistrations = findViewById(R.id.navOrganizerRegistrations);
        navProfile = findViewById(R.id.navOrganizerProfile);

        iconDashboard = findViewById(R.id.iconOrganizerDashboard);
        iconEvents = findViewById(R.id.iconOrganizerEvents);
        iconRegistrations = findViewById(R.id.iconOrganizerRegistrations);
        iconProfile = findViewById(R.id.iconOrganizerProfile);

        textDashboard = findViewById(R.id.textOrganizerDashboard);
        textEvents = findViewById(R.id.textOrganizerEvents);
        textRegistrations = findViewById(R.id.textOrganizerRegistrations);
        textProfile = findViewById(R.id.textOrganizerProfile);
    }

    private void setupNavigation() {

        navDashboard.setOnClickListener(v -> {
            openFragment(new OrganizerDashboardFragment());
            selectNavigation(navDashboard);
        });

        navEvents.setOnClickListener(v -> {
            openFragment(new OrganizerEventsFragment());
            selectNavigation(navEvents);
        });

        navRegistrations.setOnClickListener(v -> {
            openFragment(new OrganizerRegistrationsFragment());
            selectNavigation(navRegistrations);
        });

        navProfile.setOnClickListener(v -> {
            openFragment(new OrganizerProfileFragment());
            selectNavigation(navProfile);
        });
    }

    private void openFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.organizerFragmentContainer, fragment)
                .commit();
    }

    private void selectNavigation(LinearLayout selectedNav) {

        iconDashboard.setColorFilter(GREY);
        iconEvents.setColorFilter(GREY);
        iconRegistrations.setColorFilter(GREY);
        iconProfile.setColorFilter(GREY);

        textDashboard.setTextColor(GREY);
        textEvents.setTextColor(GREY);
        textRegistrations.setTextColor(GREY);
        textProfile.setTextColor(GREY);

        textDashboard.setTypeface(null, Typeface.NORMAL);
        textEvents.setTypeface(null, Typeface.NORMAL);
        textRegistrations.setTypeface(null, Typeface.NORMAL);
        textProfile.setTypeface(null, Typeface.NORMAL);

        if (selectedNav == navDashboard) {

            iconDashboard.setColorFilter(PURPLE);
            textDashboard.setTextColor(PURPLE);
            textDashboard.setTypeface(null, Typeface.BOLD);

        } else if (selectedNav == navEvents) {

            iconEvents.setColorFilter(PURPLE);
            textEvents.setTextColor(PURPLE);
            textEvents.setTypeface(null, Typeface.BOLD);

        } else if (selectedNav == navRegistrations) {

            iconRegistrations.setColorFilter(PURPLE);
            textRegistrations.setTextColor(PURPLE);
            textRegistrations.setTypeface(null, Typeface.BOLD);

        } else if (selectedNav == navProfile) {

            iconProfile.setColorFilter(PURPLE);
            textProfile.setTextColor(PURPLE);
            textProfile.setTypeface(null, Typeface.BOLD);
        }
    }

    public void logout() {

        // Clear the saved SharedPreferences session
        SessionManager sessionManager = new SessionManager(this);
        sessionManager.clearSession();

        // Sign out from Firebase Authentication
        com.google.firebase.auth.FirebaseAuth
                .getInstance()
                .signOut();

        // Return to the role-selection screen
        android.content.Intent intent =
                new android.content.Intent(this, MainActivity.class);

        intent.addFlags(
                android.content.Intent.FLAG_ACTIVITY_NEW_TASK |
                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

}
