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

    private String organizerUid;

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

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "Please log in again to create an event.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        organizerUid = user.getUid();
        loadOrganizerName();

        TextView btnBack = findViewById(R.id.btnCreateEventBack);
        btnBack.setOnClickListener(v -> finish());

        btnPublish.setOnClickListener(v -> publishEvent());
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
                                new SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.US
                                );

                        etDate.setText(
                                formatter.format(selected.getTime())
                        );
                        etDate.setError(null);
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
        etDate.setFocusable(false);
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
                                new SimpleDateFormat(
                                        "hh:mm a",
                                        Locale.getDefault()
                                );

                        etTime.setText(
                                formatter.format(selected.getTime())
                        );
                        etTime.setError(null);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
            );

            dialog.show();
        });

        etTime.setFocusable(false);
    }

    private void loadOrganizerName() {
        db.collection("organizers")
                .document(organizerUid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String name = document.getString("name");

                        if (name != null && !name.trim().isEmpty()) {
                            etOrganizerName.setText(name.trim());
                        } else {
                            String organization =
                                    document.getString("organization");

                            if (organization != null
                                    && !organization.trim().isEmpty()) {
                                etOrganizerName.setText(
                                        organization.trim()
                                );
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                CreateEventActivity.this,
                                "Could not load organizer profile.",
                                Toast.LENGTH_SHORT
                        ).show()
                );
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
        String category = spCategory.getSelectedItem() == null
                ? ""
                : spCategory.getSelectedItem().toString();

        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        String city = etCity.getText().toString().trim();
        String organizerName =
                etOrganizerName.getText().toString().trim();
        String description =
                etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Enter the event title");
            etTitle.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(
                    this,
                    "Select an event category.",
                    Toast.LENGTH_SHORT
            ).show();
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
        event.put("description", description);

        // Match the field names used in your Firestore structure.
        event.put("imageURL", "");

        // IMPORTANT: Use the exact capitalization from your schema.
        event.put("organizerID", user.getUid());
        event.put("organizerName", organizerName);

        event.put(
                "registrationRequired",
                switchRegistrationRequired.isChecked()
        );

        event.put("createdAt", FieldValue.serverTimestamp());
        event.put("registeredCount", 0);

        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(
                            CreateEventActivity.this,
                            "Event published successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPublish.setEnabled(true);
                    btnPublish.setText("Publish Event");

                    Toast.makeText(
                            CreateEventActivity.this,
                            "Event could not be published: "
                                    + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}
