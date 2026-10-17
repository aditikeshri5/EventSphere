
package com.la.eventsphere;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateEventActivity extends AppCompatActivity {

    private EditText etTitle, etDate, etTime, etVenue;
    private EditText etCity, etOrganizerName, etDescription;

    private Spinner spCategory;
    private Switch switchRegistrationRequired;
    private Button btnPublish;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String[] categories = {
            "Music",
            "Sports",
            "Cultural Events",
            "Hackathons",
            "Workshops",
            "Competitions",
            "Fests",
            "Concerts",
            "Career & Networking",
            "Arts & Creativity"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupCategorySpinner();
        setupDatePicker();
        setupTimePicker();

        findViewById(R.id.btnCreateEventBack).setOnClickListener(v -> finish());
        btnPublish.setOnClickListener(v -> publishEvent());

        loadOrganizerName();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etCreateEventTitle);
        etDate = findViewById(R.id.etCreateEventDate);
        etTime = findViewById(R.id.etCreateEventTime);
        etVenue = findViewById(R.id.etCreateEventVenue);
        etCity = findViewById(R.id.etCreateEventCity);
        etOrganizerName = findViewById(R.id.etCreateOrganizerName);
        etDescription = findViewById(R.id.etCreateEventDescription);

        spCategory = findViewById(R.id.spCreateEventCategory);
        switchRegistrationRequired =
                findViewById(R.id.switchRegistrationRequired);
        btnPublish = findViewById(R.id.btnPublishEvent);
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spCategory.setAdapter(adapter);
    }

    private void setupDatePicker() {
        View.OnClickListener listener = v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog dialog = new DatePickerDialog(
                    CreateEventActivity.this,
                    (picker, year, month, day) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, day);

                        SimpleDateFormat formatter =
                                new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                        etDate.setText(formatter.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.getDatePicker().setMinDate(
                    System.currentTimeMillis() - 1000
            );

            dialog.show();
        };

        etDate.setOnClickListener(listener);
    }

    private void setupTimePicker() {
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            TimePickerDialog dialog = new TimePickerDialog(
                    CreateEventActivity.this,
                    (picker, hour, minute) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(Calendar.HOUR_OF_DAY, hour);
                        selected.set(Calendar.MINUTE, minute);

                        SimpleDateFormat formatter =
                                new SimpleDateFormat("hh:mm a", Locale.getDefault());

                        etTime.setText(formatter.format(selected.getTime()));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            );

            dialog.show();
        });
    }

    private void loadOrganizerName() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        db.collection("organizers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");

                        if (name != null && !name.trim().isEmpty()) {
                            etOrganizerName.setText(name);
                        }
                    }
                });
    }

    private void publishEvent() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Your session has expired. Please log in again.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String category = spCategory.getSelectedItem().toString();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String organizerName = etOrganizerName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Enter the event title");
            etTitle.requestFocus();
            return;
        }

        if (date.isEmpty()) {
            etDate.setError("Select the event date");
            etDate.requestFocus();
            return;
        }

        if (time.isEmpty()) {
            etTime.setError("Select the event time");
            etTime.requestFocus();
            return;
        }

        if (venue.isEmpty()) {
            etVenue.setError("Enter the venue");
            etVenue.requestFocus();
            return;
        }

        if (city.isEmpty()) {
            etCity.setError("Enter the city");
            etCity.requestFocus();
            return;
        }

        if (organizerName.isEmpty()) {
            etOrganizerName.setError("Enter the organizer name");
            etOrganizerName.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Enter the event description");
            etDescription.requestFocus();
            return;
        }

        btnPublish.setEnabled(false);
        btnPublish.setText("Publishing...");

        Map<String, Object> event = new HashMap<>();

        event.put("title", title);
        event.put("category", category);
        event.put("date", date);
        event.put("time", time);
        event.put("venue", venue);
        event.put("city", city);
        event.put("organizerName", organizerName);
        event.put("description", description);
        event.put("registrationRequired",
                switchRegistrationRequired.isChecked());

        // Ownership: used to show this event only in its organizer's dashboard.
        event.put("organizerId", user.getUid());

        // Useful for ordering and future dashboard features.
        event.put("createdAt", FieldValue.serverTimestamp());

        // Initial count for future registration-count features.
        event.put("registeredCount", 0);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(
                            CreateEventActivity.this,
                            "Event published successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publish Event");

                    Toast.makeText(
                            CreateEventActivity.this,
                            "Could not publish event. Please check your connection and Firestore permissions.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
