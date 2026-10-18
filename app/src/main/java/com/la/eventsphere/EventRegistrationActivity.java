package com.la.eventsphere;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class EventRegistrationActivity extends AppCompatActivity {

    private ImageView btnBack;

    private TextView tvRegistrationEventTitle;
    private TextView tvRegistrationEventDate;
    private TextView tvRegistrationEventVenue;
    private TextView tvRegistrationUserName;
    private TextView tvRegistrationUserEmail;

    private Button btnConfirmRegistration;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String eventId;
    private String userId;
    private String userName = "";
    private String userEmail = "";

    private DocumentSnapshot eventDocument;

    private boolean eventLoaded = false;
    private boolean registrationChecked = false;
    private boolean registrationInProgress = false;
    private boolean alreadyRegistered = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_registration);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initializeViews();

        btnBack.setOnClickListener(v -> finish());

        btnConfirmRegistration.setEnabled(false);
        btnConfirmRegistration.setOnClickListener(v -> registerForEvent());

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this,
                    "Please log in to register.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        userId = user.getUid();
        userEmail = user.getEmail() == null ? "" : user.getEmail();
        userName = user.getDisplayName() == null
                ? "" : user.getDisplayName();

        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(this,
                    "Event information is missing. Please open the event again.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadUserDetails(user);
        loadEventDetails();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);

        tvRegistrationEventTitle =
                findViewById(R.id.tvRegistrationEventTitle);

        tvRegistrationEventDate =
                findViewById(R.id.tvRegistrationEventDate);

        tvRegistrationEventVenue =
                findViewById(R.id.tvRegistrationEventVenue);

        tvRegistrationUserName =
                findViewById(R.id.tvRegistrationUserName);

        tvRegistrationUserEmail =
                findViewById(R.id.tvRegistrationUserEmail);

        btnConfirmRegistration =
                findViewById(R.id.btnConfirmRegistration);
    }

    private void loadUserDetails(FirebaseUser user) {
        // Show available Authentication details immediately.
        tvRegistrationUserName.setText(
                userName.isEmpty() ? "Loading profile..." : userName
        );

        tvRegistrationUserEmail.setText(
                userEmail.isEmpty() ? "Email not available" : userEmail
        );

        // Then try to load the full user profile from Firestore.
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(profile -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (profile.exists()) {
                        String profileName = firstNonEmpty(
                                profile.getString("name"),
                                profile.getString("fullName"),
                                profile.getString("userName"),
                                profile.getString("displayName")
                        );

                        String profileEmail =
                                profile.getString("email");

                        if (!profileName.isEmpty()) {
                            userName = profileName;
                        }

                        if (profileEmail != null
                                && !profileEmail.trim().isEmpty()) {
                            userEmail = profileEmail.trim();
                        }
                    }

                    tvRegistrationUserName.setText(
                            userName.isEmpty()
                                    ? "Name not available in your profile"
                                    : userName
                    );

                    tvRegistrationUserEmail.setText(
                            userEmail.isEmpty()
                                    ? "Email not available"
                                    : userEmail
                    );
                })
                .addOnFailureListener(e -> {
                    // Authentication details remain usable if the
                    // Firestore profile cannot be read.
                    tvRegistrationUserName.setText(
                            userName.isEmpty()
                                    ? "Name not available"
                                    : userName
                    );

                    tvRegistrationUserEmail.setText(
                            userEmail.isEmpty()
                                    ? "Email not available"
                                    : userEmail
                    );
                });
    }

    private void loadEventDetails() {
        tvRegistrationEventTitle.setText("Loading event...");
        tvRegistrationEventDate.setText("Loading date and time...");
        tvRegistrationEventVenue.setText("Loading venue...");

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(event -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!event.exists()) {
                        Toast.makeText(this,
                                "This event is no longer available.",
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    eventDocument = event;

                    String title = value(event, "title");
                    String date = value(event, "date");
                    String time = value(event, "time");
                    String venue = value(event, "venue");
                    String city = value(event, "city");
                    String category = value(event, "category");
                    String organizerName =
                            value(event, "organizerName");

                    tvRegistrationEventTitle.setText(
                            title.isEmpty() ? "Event" : title
                    );

                    String dateAndTime = "Date: "
                            + (date.isEmpty() ? "Not available" : date)
                            + "\nTime: "
                            + (time.isEmpty() ? "Not available" : time);

                    if (!category.isEmpty()) {
                        dateAndTime += "\nCategory: " + category;
                    }

                    tvRegistrationEventDate.setText(dateAndTime);

                    String venueDetails = "Venue: "
                            + (venue.isEmpty() ? "Not available" : venue);

                    if (!city.isEmpty()) {
                        venueDetails += "\nCity: " + city;
                    }

                    if (!organizerName.isEmpty()) {
                        venueDetails += "\nOrganized by: " + organizerName;
                    }

                    tvRegistrationEventVenue.setText(venueDetails);

                    eventLoaded = true;

                    Boolean required =
                            event.getBoolean("registrationRequired");

                    if (Boolean.FALSE.equals(required)) {
                        btnConfirmRegistration.setText(
                                "Registration not required"
                        );
                        btnConfirmRegistration.setEnabled(false);
                        return;
                    }

                    checkExistingRegistration();
                })
                .addOnFailureListener(e -> {
                    tvRegistrationEventTitle.setText(
                            "Unable to load event"
                    );
                    tvRegistrationEventDate.setText(
                            "Please go back and try again."
                    );
                    tvRegistrationEventVenue.setText("");

                    btnConfirmRegistration.setEnabled(false);

                    Toast.makeText(this,
                            "Could not load event details: "
                                    + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void checkExistingRegistration() {
        // Query only this user's registrations. This works with
        // rules that allow users to read their own registration records.
        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    alreadyRegistered = false;

                    for (DocumentSnapshot registration
                            : snapshot.getDocuments()) {

                        if ("registered".equals(
                                registration.getString("status"))) {
                            alreadyRegistered = true;
                            break;
                        }
                    }

                    registrationChecked = true;

                    if (alreadyRegistered) {
                        btnConfirmRegistration.setText(
                                "Already Registered"
                        );
                        btnConfirmRegistration.setEnabled(false);
                    } else {
                        btnConfirmRegistration.setText(
                                "Confirm Registration"
                        );
                        btnConfirmRegistration.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    registrationChecked = false;
                    btnConfirmRegistration.setEnabled(false);

                    Toast.makeText(this,
                            "Could not check registration. "
                                    + "Please check your Firestore rules.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void registerForEvent() {
        if (registrationInProgress) {
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this,
                    "Please log in to register.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (!eventLoaded || eventDocument == null) {
            Toast.makeText(this,
                    "Event details are still loading. Please wait.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!registrationChecked) {
            Toast.makeText(this,
                    "Please wait while we verify your registration.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (alreadyRegistered) {
            Toast.makeText(this,
                    "You have already registered for this event.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        registrationInProgress = true;
        btnConfirmRegistration.setEnabled(false);
        btnConfirmRegistration.setText("Registering...");

        // Recheck immediately before writing to avoid ordinary
        // duplicate taps or repeat registrations.
        db.collection("registrations")
                .whereEqualTo("userId", userId)
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean found = false;

                    for (DocumentSnapshot registration
                            : snapshot.getDocuments()) {
                        if ("registered".equals(
                                registration.getString("status"))) {
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        registrationInProgress = false;
                        alreadyRegistered = true;
                        btnConfirmRegistration.setText(
                                "Already Registered"
                        );
                        btnConfirmRegistration.setEnabled(false);

                        Toast.makeText(this,
                                "You have already registered.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveRegistration();
                })
                .addOnFailureListener(e -> {
                    resetButton();

                    Toast.makeText(this,
                            "Unable to verify registration. "
                                    + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveRegistration() {
        Map<String, Object> registration = new HashMap<>();

        registration.put("eventId", eventId);
        registration.put("eventTitle", value(eventDocument, "title"));
        registration.put("eventDate", value(eventDocument, "date"));
        registration.put("eventTime", value(eventDocument, "time"));
        registration.put("eventVenue", value(eventDocument, "venue"));
        registration.put("eventCity", value(eventDocument, "city"));
        registration.put("category", value(eventDocument, "category"));

        registration.put(
                "organizerID",
                value(eventDocument, "organizerID")
        );

        registration.put(
                "organizerName",
                value(eventDocument, "organizerName")
        );

        registration.put("userId", userId);
        registration.put("userName", userName);
        registration.put("userEmail", userEmail);
        registration.put("status", "registered");
        registration.put("registeredAt", FieldValue.serverTimestamp());

        // Use add() so each registration has its own generated document ID.
        // The duplicate check above prevents ordinary repeat registrations.
        db.collection("registrations")
                .add(registration)
                .addOnSuccessListener(reference -> {
                    registrationInProgress = false;
                    alreadyRegistered = true;

                    btnConfirmRegistration.setText("Registered ✓");
                    btnConfirmRegistration.setEnabled(false);

                    Toast.makeText(this,
                            "Registration successful!",
                            Toast.LENGTH_LONG).show();

                    // Return to the previous screen after successful save.
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetButton();

                    Toast.makeText(this,
                            "Registration failed: "
                                    + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String value(DocumentSnapshot document, String field) {
        String result = document.getString(field);
        return result == null ? "" : result;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private void resetButton() {
        registrationInProgress = false;

        if (alreadyRegistered) {
            btnConfirmRegistration.setText("Already Registered");
            btnConfirmRegistration.setEnabled(false);
        } else {
            btnConfirmRegistration.setText("Confirm Registration");
            btnConfirmRegistration.setEnabled(
                    eventLoaded && registrationChecked
            );
        }
    }
}
