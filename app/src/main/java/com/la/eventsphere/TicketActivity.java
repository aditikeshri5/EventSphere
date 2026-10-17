package com.la.eventsphere;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class TicketActivity extends AppCompatActivity {

    private ImageView btnBack;

    private TextView tvEventTitle;
    private TextView tvEventDate;
    private TextView tvEventVenue;

    private TextView tvUserName;
    private TextView tvUserEmail;

    private TextView tvRegistrationId;
    private TextView tvStatus;

    private FirebaseFirestore db;

    private String registrationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket);

        initializeViews();

        db = FirebaseFirestore.getInstance();

        registrationId =
                getIntent().getStringExtra("registrationId");

        if (registrationId == null
                || registrationId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to load ticket.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        tvRegistrationId.setText(registrationId);

        btnBack.setOnClickListener(v -> finish());

        loadTicket();
    }

    private void initializeViews() {

        btnBack =
                findViewById(R.id.btnBack);

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

        tvRegistrationId =
                findViewById(R.id.tvRegistrationId);

        tvStatus =
                findViewById(R.id.tvStatus);
    }

    private void loadTicket() {

        db.collection("registrations")
                .document(registrationId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        Toast.makeText(
                                TicketActivity.this,
                                "Ticket not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    displayTicket(document);
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            TicketActivity.this,
                            "Unable to load ticket.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void displayTicket(
            com.google.firebase.firestore.DocumentSnapshot document) {

        String eventTitle =
                document.getString("eventTitle");

        String eventDate =
                document.getString("eventDate");

        String eventVenue =
                document.getString("eventVenue");

        String userName =
                document.getString("userName");

        String userEmail =
                document.getString("userEmail");

        String status =
                document.getString("status");

        tvEventTitle.setText(
                eventTitle != null
                        ? eventTitle
                        : "Event"
        );

        tvEventDate.setText(
                eventDate != null
                        ? "📅 " + eventDate
                        : "📅 Date not available"
        );

        tvEventVenue.setText(
                eventVenue != null
                        ? "📍 " + eventVenue
                        : "📍 Venue not available"
        );

        tvUserName.setText(
                userName != null
                        ? userName
                        : "User"
        );

        tvUserEmail.setText(
                userEmail != null
                        ? userEmail
                        : ""
        );

        tvRegistrationId.setText(
                registrationId
        );

        if (status != null) {

            tvStatus.setText(
                    status.toUpperCase()
            );

        } else {

            tvStatus.setText(
                    "CONFIRMED"
            );
        }
    }
}