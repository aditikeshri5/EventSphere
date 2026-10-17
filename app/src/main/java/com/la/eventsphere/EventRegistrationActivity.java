package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EventRegistrationActivity extends AppCompatActivity {

    private ImageView btnBack;

    private TextView tvEventTitle;
    private TextView tvEventDate;
    private TextView tvEventVenue;

    private TextView tvUserName;
    private TextView tvUserEmail;

    private MaterialButton btnConfirmRegistration;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String eventId;

    private String eventTitle;
    private String eventDate;
    private String eventVenue;

    private String userName;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_registration);

        initializeViews();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to register for this event.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        userEmail = currentUser.getEmail();

        tvUserEmail.setText(
                userEmail != null ? userEmail : ""
        );

        loadEvent();
        loadUserProfile();

        btnBack.setOnClickListener(v -> finish());

        btnConfirmRegistration.setOnClickListener(
                v -> registerUser()
        );
    }

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);

        tvEventTitle =
                findViewById(R.id.tvEventTitle);

        tvEventDate =
                findViewById(R.id.tvEventDate);

        tvEventVenue =
                findViewById(R.id.tvEventVenue);

        tvUserName =
                findViewById(R.id.tvUserName);

        tvUserEmail =
                findViewById(R.id.tvUserEmail);

        btnConfirmRegistration =
                findViewById(R.id.btnConfirmRegistration);
    }

    private void loadEvent() {

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        Toast.makeText(
                                EventRegistrationActivity.this,
                                "This event is no longer available.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    eventTitle =
                            document.getString("title");

                    eventDate =
                            document.getString("date");

                    String eventTime =
                            document.getString("time");

                    String venue =
                            document.getString("venue");

                    String city =
                            document.getString("city");

                    eventVenue = venue;

                    tvEventTitle.setText(
                            eventTitle != null
                                    ? eventTitle
                                    : "Event"
                    );

                    tvEventDate.setText(
                            "📅 "
                                    + (eventDate != null
                                    ? eventDate
                                    : "Date not available")
                                    + "  "
                                    + (eventTime != null
                                    ? eventTime
                                    : "")
                    );

                    String locationText =
                            "📍 "
                                    + (venue != null
                                    ? venue
                                    : "");

                    if (city != null && !city.isEmpty()) {
                        locationText += ", " + city;
                    }

                    tvEventVenue.setText(locationText);

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            EventRegistrationActivity.this,
                            "Unable to load event.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadUserProfile() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {
            return;
        }

        String uid =
                currentUser.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        userName =
                                document.getString("name");

                        tvUserName.setText(
                                userName != null
                                        ? userName
                                        : "User"
                        );
                    }
                });
    }

    private void registerUser() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (eventTitle == null) {

            Toast.makeText(
                    this,
                    "Event information is still loading.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        btnConfirmRegistration.setEnabled(false);
        btnConfirmRegistration.setText(
                "Registering..."
        );

        String uid =
                currentUser.getUid();

        /*
         * We use:
         *
         * eventId + "_" + userId
         *
         * as the registration document ID.
         *
         * This prevents the same user from registering
         * multiple times for the same event.
         */
        String registrationId =
                eventId + "_" + uid;

        Map<String, Object> registration =
                new HashMap<>();

        registration.put(
                "eventId",
                eventId
        );

        registration.put(
                "userId",
                uid
        );

        registration.put(
                "userName",
                userName != null
                        ? userName
                        : ""
        );

        registration.put(
                "userEmail",
                userEmail != null
                        ? userEmail
                        : ""
        );

        registration.put(
                "eventTitle",
                eventTitle
        );

        registration.put(
                "eventDate",
                eventDate != null
                        ? eventDate
                        : ""
        );

        registration.put(
                "eventVenue",
                eventVenue != null
                        ? eventVenue
                        : ""
        );

        registration.put(
                "status",
                "confirmed"
        );

        registration.put(
                "registeredAt",
                FieldValue.serverTimestamp()
        );

        db.collection("registrations")
                .document(registrationId)
                .set(registration)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(
                            EventRegistrationActivity.this,
                            "Registration successful!",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent =
                            new Intent(
                                    EventRegistrationActivity.this,
                                    TicketActivity.class
                            );

                    intent.putExtra(
                            "eventId",
                            eventId
                    );

                    intent.putExtra(
                            "registrationId",
                            registrationId
                    );

                    startActivity(intent);

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnConfirmRegistration.setEnabled(
                            true
                    );

                    btnConfirmRegistration.setText(
                            "Confirm Registration"
                    );

                    Toast.makeText(
                            EventRegistrationActivity.this,
                            "Registration failed. Please try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}