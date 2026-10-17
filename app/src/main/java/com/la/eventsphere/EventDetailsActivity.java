package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class EventDetailsActivity extends AppCompatActivity {

    private ImageView ivEventImage;
    private ImageView btnBack;

    private TextView tvCategory;
    private TextView tvTitle;
    private TextView tvDate;
    private TextView tvTime;
    private TextView tvVenue;
    private TextView tvCity;
    private TextView tvOrganizer;
    private TextView tvDescription;

    private Button btnRegister;

    private FirebaseFirestore db;

    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        initializeViews();

        db = FirebaseFirestore.getInstance();

        // Get event ID sent from EventAdapter
        eventId = getIntent().getStringExtra("eventId");

        if (eventId == null || eventId.isEmpty()) {

            Toast.makeText(
                    this,
                    "Unable to open this event.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        loadEventDetails();

        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    EventDetailsActivity.this,
                    EventRegistrationActivity.class
            );

            intent.putExtra("eventId", eventId);

            startActivity(intent);
        });
    }

    private void initializeViews() {

        ivEventImage = findViewById(R.id.ivEventImage);
        btnBack = findViewById(R.id.btnBack);

        tvCategory = findViewById(R.id.tvCategory);
        tvTitle = findViewById(R.id.tvTitle);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvVenue = findViewById(R.id.tvVenue);
        tvCity = findViewById(R.id.tvCity);
        tvOrganizer = findViewById(R.id.tvOrganizer);
        tvDescription = findViewById(R.id.tvDescription);

        btnRegister = findViewById(R.id.btnRegister);
    }

    private void loadEventDetails() {

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        displayEvent(document);

                    } else {

                        Toast.makeText(
                                EventDetailsActivity.this,
                                "This event is no longer available.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            EventDetailsActivity.this,
                            "Unable to load event details.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void displayEvent(DocumentSnapshot document) {

        String category =
                document.getString("category");

        String title =
                document.getString("title");

        String date =
                document.getString("date");

        String time =
                document.getString("time");

        String venue =
                document.getString("venue");

        String city =
                document.getString("city");

        String organizerName =
                document.getString("organizerName");

        String description =
                document.getString("description");

        Boolean registrationRequired =
                document.getBoolean("registrationRequired");

        tvCategory.setText(
                category != null ? category : ""
        );

        tvTitle.setText(
                title != null ? title : "Event"
        );

        tvDate.setText(
                date != null
                        ? "📅 " + date
                        : "📅 Date not available"
        );

        tvTime.setText(
                time != null
                        ? "🕐 " + time
                        : "🕐 Time not available"
        );

        tvVenue.setText(
                venue != null
                        ? "📍 " + venue
                        : "📍 Venue not available"
        );

        tvCity.setText(
                city != null
                        ? city
                        : ""
        );

        tvOrganizer.setText(
                organizerName != null
                        ? "Organized by " + organizerName
                        : "Organizer information unavailable"
        );

        tvDescription.setText(
                description != null && !description.isEmpty()
                        ? description
                        : "No description available."
        );

        /*
         * Registration button depends on the
         * organizer's registrationRequired field.
         */
        if (registrationRequired != null
                && registrationRequired) {

            btnRegister.setVisibility(View.VISIBLE);
            btnRegister.setText("Register for Event");

        } else {

            btnRegister.setVisibility(View.GONE);
        }
    }
}