package com.la.eventsphere;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TicketActivity extends AppCompatActivity {

    private ImageView btnBack;

    private TextView tvEventTitle;
    private TextView tvEventDate;
    private TextView tvEventTime;
    private TextView tvEventVenue;
    private TextView tvEventCity;

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvRegistrationId;
    private TextView tvStatus;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String registrationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        initializeViews();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        registrationId = getIntent().getStringExtra("registrationId");

        btnBack.setOnClickListener(v -> finish());

        if (registrationId == null || registrationId.trim().isEmpty()) {
            Toast.makeText(this,
                    "Ticket ID is missing.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this,
                    "Please log in to view your ticket.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadTicket(currentUser.getUid());
    }

    private void initializeViews() {

        btnBack = findViewById(R.id.btnBack);

        tvEventTitle = findViewById(R.id.tvEventTitle);
        tvEventDate = findViewById(R.id.tvEventDate);
        tvEventTime = findViewById(R.id.tvEventTime);
        tvEventVenue = findViewById(R.id.tvEventVenue);
        tvEventCity = findViewById(R.id.tvEventCity);

        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvRegistrationId = findViewById(R.id.tvRegistrationId);
        tvStatus = findViewById(R.id.tvStatus);
    }

    private void loadTicket(String userId) {

        db.collection("registrations")
                .document(registrationId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        showErrorAndFinish("Ticket not found.");
                        return;
                    }

                    String registeredUserId =
                            document.getString("userId");

                    if (registeredUserId == null ||
                            !registeredUserId.equals(userId)) {

                        showErrorAndFinish(
                                "You are not authorized to view this ticket."
                        );
                        return;
                    }

                    displayTicket(document);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                TicketActivity.this,
                                "Unable to load ticket. Please try again.",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void displayTicket(DocumentSnapshot document) {

        String eventTitle = document.getString("eventTitle");
        String eventDate = document.getString("eventDate");
        String eventTime = document.getString("eventTime");
        String eventVenue = document.getString("eventVenue");
        String eventCity = document.getString("eventCity");

        String userName = document.getString("userName");
        String userEmail = document.getString("userEmail");
        String status = document.getString("status");

        // Use saved Ticket ID, or registration document ID.
        String ticketId = document.getString("ticketId");

        if (ticketId == null || ticketId.trim().isEmpty()) {
            ticketId = document.getId();
        }

        tvEventTitle.setText(
                eventTitle != null ? eventTitle : "Event"
        );

        tvEventDate.setText(
                "Date: " + (eventDate != null
                        ? eventDate : "Not available")
        );

        tvEventTime.setText(
                "Time: " + (eventTime != null
                        ? eventTime : "Not available")
        );

        tvEventVenue.setText(
                "Venue: " + (eventVenue != null
                        ? eventVenue : "Not available")
        );

        tvEventCity.setText(
                "City: " + (eventCity != null
                        ? eventCity : "Not available")
        );

        tvUserName.setText(
                userName != null ? userName : "User"
        );

        tvUserEmail.setText(
                userEmail != null ? userEmail : ""
        );

        tvRegistrationId.setText(ticketId);

        tvStatus.setText(
                status != null ? status.toUpperCase() : "CONFIRMED"
        );
    }

    private void showErrorAndFinish(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        finish();
    }
}
