package com.la.eventsphere;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private EditText etTitle, etDate, etTime, etVenue;
    private EditText etCity, etOrganizerName, etDescription;
    private Spinner spCategory;
    private Switch switchRegistrationRequired;
    private Button btnUpdate;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String eventId;
    private boolean eventLoaded = false;

    private final String[] categories = {
            "Music", "Sports", "Cultural Events", "Hackathons",
            "Workshops", "Competitions", "Fests", "Concerts",
            "Career & Networking", "Arts & Creativity"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupCategorySpinner();
        setupDatePicker();
        setupTimePicker();

        eventId = getIntent().getStringExtra("eventId");

        findViewById(R.id.btnEditEventBack)
                .setOnClickListener(v -> finish());

        btnUpdate.setOnClickListener(v -> updateEvent());

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Event ID is missing.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        btnUpdate.setEnabled(false);
        loadEvent(user.getUid());
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.etEditEventTitle);
        etDate = findViewById(R.id.etEditEventDate);
        etTime = findViewById(R.id.etEditEventTime);
        etVenue = findViewById(R.id.etEditEventVenue);
        etCity = findViewById(R.id.etEditEventCity);
        etOrganizerName = findViewById(R.id.etEditOrganizerName);
        etDescription = findViewById(R.id.etEditEventDescription);

        spCategory = findViewById(R.id.spEditEventCategory);

        switchRegistrationRequired =
                findViewById(R.id.switchEditRegistrationRequired);

        btnUpdate = findViewById(R.id.btnUpdateEvent);
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
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            String current = etDate.getText().toString().trim();

            try {
                Calendar existing = Calendar.getInstance();

                existing.setTime(
                        new SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                        ).parse(current)
                );

                calendar = existing;

            } catch (Exception ignored) {
                // Use today's date if no valid date is selected.
            }

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        Calendar selected = Calendar.getInstance();

                        selected.set(year, month, day);

                        etDate.setText(
                                new SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.US
                                ).format(selected.getTime())
                        );
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.getDatePicker().setMinDate(
                    System.currentTimeMillis() - 1000
            );

            dialog.show();
        });
    }

    private void setupTimePicker() {
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            try {
                calendar.setTime(
                        new SimpleDateFormat(
                                "hh:mm a",
                                Locale.getDefault()
                        ).parse(
                                etTime.getText().toString().trim()
                        )
                );

            } catch (Exception ignored) {
                // Use the current time if no valid time is selected.
            }

            new TimePickerDialog(
                    this,
                    (picker, hour, minute) -> {
                        Calendar selected = Calendar.getInstance();

                        selected.set(Calendar.HOUR_OF_DAY, hour);
                        selected.set(Calendar.MINUTE, minute);

                        etTime.setText(
                                new SimpleDateFormat(
                                        "hh:mm a",
                                        Locale.getDefault()
                                ).format(selected.getTime())
                        );
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            ).show();
        });
    }

    private void loadEvent(String uid) {
        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (!document.exists()) {
                        Toast.makeText(
                                this,
                                "Event not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    // Verify that the current organizer owns this event.
                    String ownerId = document.getString("organizerID");

                    if (ownerId == null || !ownerId.equals(uid)) {
                        Toast.makeText(
                                this,
                                "You do not have permission to edit this event.",
                                Toast.LENGTH_LONG
                        ).show();

                        finish();
                        return;
                    }

                    // Load existing event details into the form.
                    etTitle.setText(document.getString("title"));
                    etDate.setText(document.getString("date"));
                    etTime.setText(document.getString("time"));
                    etVenue.setText(document.getString("venue"));
                    etCity.setText(document.getString("city"));

                    etOrganizerName.setText(
                            document.getString("organizerName")
                    );

                    etDescription.setText(
                            document.getString("description")
                    );

                    String category = document.getString("category");

                    int index = Arrays.asList(categories)
                            .indexOf(category);

                    if (index >= 0) {
                        spCategory.setSelection(index);
                    }

                    Boolean required =
                            document.getBoolean("registrationRequired");

                    switchRegistrationRequired.setChecked(
                            required == null || required
                    );

                    eventLoaded = true;
                    btnUpdate.setEnabled(true);
                })
                .addOnFailureListener(e -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    Toast.makeText(
                            this,
                            "Unable to load event details: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void updateEvent() {
        if (!eventLoaded) {
            Toast.makeText(
                    this,
                    "Event details are still loading.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in again.",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        String title = etTitle.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        String city = etCity.getText().toString().trim();

        String organizerName =
                etOrganizerName.getText().toString().trim();

        String description =
                etDescription.getText().toString().trim();

        if (title.isEmpty()
                || date.isEmpty()
                || time.isEmpty()
                || venue.isEmpty()
                || city.isEmpty()
                || organizerName.isEmpty()
                || description.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please complete all required fields.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        btnUpdate.setEnabled(false);
        btnUpdate.setText("Saving...");

        String category = spCategory.getSelectedItem().toString();

        Map<String, Object> updates = new HashMap<>();

        updates.put("title", title);
        updates.put("category", category);
        updates.put("date", date);
        updates.put("time", time);
        updates.put("venue", venue);
        updates.put("city", city);
        updates.put("organizerName", organizerName);
        updates.put("description", description);
        updates.put(
                "registrationRequired",
                switchRegistrationRequired.isChecked()
        );

        // Step 1: Update the main event document.
        db.collection("events")
                .document(eventId)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    // Step 2: Find existing registrations for this event.
                    db.collection("registrations")
                            .whereEqualTo("eventId", eventId)
                            .whereEqualTo("organizerID", user.getUid())
                            .get()
                            .addOnSuccessListener(querySnapshot -> {

                                if (isFinishing() || isDestroyed()) {
                                    return;
                                }

                                if (querySnapshot.isEmpty()) {
                                    showUpdateSuccess();
                                    return;
                                }

                                // Firestore batches support at most 500 writes.
                                if (querySnapshot.size() > 500) {
                                    btnUpdate.setEnabled(true);
                                    btnUpdate.setText("Save Changes");

                                    Toast.makeText(
                                            this,
                                            "Event updated, but there are more than 500 registrations. Registration updates need to be processed in batches.",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                // Step 3: Update copied details in registrations.
                                WriteBatch batch = db.batch();

                                for (DocumentSnapshot registration
                                        : querySnapshot.getDocuments()) {

                                    batch.update(
                                            registration.getReference(),
                                            "eventTitle", title,
                                            "eventDate", date,
                                            "eventTime", time,
                                            "eventVenue", venue,
                                            "eventCity", city,
                                            "category", category,
                                            "organizerName", organizerName,
                                            "description", description,
                                            "registrationRequired",
                                            switchRegistrationRequired.isChecked()
                                    );
                                }

                                batch.commit()
                                        .addOnSuccessListener(result -> {
                                            if (!isFinishing()
                                                    && !isDestroyed()) {
                                                showUpdateSuccess();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            if (!isFinishing()
                                                    && !isDestroyed()) {
                                                showUpdateFailure(
                                                        "The event was updated, but registration details could not be updated: "
                                                                + e.getMessage()
                                                );
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                if (!isFinishing() && !isDestroyed()) {
                                    showUpdateFailure(
                                            "The event was updated, but registrations could not be loaded: "
                                                    + e.getMessage()
                                    );
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showUpdateFailure(
                                "Could not update event: " + e.getMessage()
                        );
                    }
                });
    }

    private void showUpdateSuccess() {
        Toast.makeText(
                this,
                "Event and registration details updated successfully!",
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }

    private void showUpdateFailure(String message) {
        btnUpdate.setEnabled(true);
        btnUpdate.setText("Save Changes");

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
