
package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnContinueAsUser;
    private Button btnContinueAsOrganizer;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private boolean isCheckingSession = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase and SharedPreferences
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Connect XML buttons
        btnContinueAsUser = findViewById(R.id.btnContinueAsUser);
        btnContinueAsOrganizer =
                findViewById(R.id.btnContinueAsOrganizer);

        // Check Firebase Authentication first
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {

            // A Firebase session exists.
            // Use Firestore to verify the account type and profile.
            setRoleButtonsEnabled(false);
            redirectLoggedInAccount(currentUser.getUid());
            return;

        } else {

            // Firebase has no signed-in user, so clear stale local data.
            sessionManager.clearSession();
        }

        // No signed-in account: show role selection normally.
        btnContinueAsUser.setOnClickListener(v -> {
            startActivity(new Intent(
                    MainActivity.this,
                    LoginActivity.class
            ));
        });

        btnContinueAsOrganizer.setOnClickListener(v -> {
            startActivity(new Intent(
                    MainActivity.this,
                    OrganizerLoginActivity.class
            ));
        });
    }

    private void redirectLoggedInAccount(String uid) {

        if (isCheckingSession) {
            return;
        }

        isCheckingSession = true;

        // First check whether this UID belongs to an organizer.
        db.collection("organizers")
                .document(uid)
                .get()
                .addOnSuccessListener(organizerDocument -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (organizerDocument.exists()) {

                        // Save or refresh the organizer session.
                        sessionManager.saveSession("organizer");

                        openDashboard(OrganizerActivity.class);

                    } else {

                        // Otherwise, check the regular-user profile.
                        checkRegularUserProfile(uid);
                    }
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    showSessionError();
                });
    }

    private void checkRegularUserProfile(String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(userDocument -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (userDocument.exists()) {

                        // Save or refresh the regular-user session.
                        sessionManager.saveSession("user");

                        redirectUserBasedOnInterests(userDocument);

                    } else {

                        // Neither profile exists for this UID.
                        // Do not automatically sign out because this
                        // could require manual account/profile recovery.
                        showSessionError();
                    }
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    showSessionError();
                });
    }

    private void redirectUserBasedOnInterests(
            DocumentSnapshot userDocument) {

        Object interestsObject = userDocument.get("interests");

        if (interestsObject instanceof List
                && ((List<?>) interestsObject).size() >= 3) {

            // The user has already selected three interests.
            openDashboard(HomeActivity.class);

        } else {

            // The user still needs to select their interests.
            openDashboard(InterestActivity.class);
        }
    }

    private void openDashboard(Class<?> activityClass) {

        Intent intent = new Intent(MainActivity.this, activityClass);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showSessionError() {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        isCheckingSession = false;

        setRoleButtonsEnabled(true);

        Toast.makeText(
                MainActivity.this,
                "Unable to verify your account. Check your internet connection and try again.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void setRoleButtonsEnabled(boolean enabled) {

        btnContinueAsUser.setEnabled(enabled);
        btnContinueAsOrganizer.setEnabled(enabled);
    }
}
