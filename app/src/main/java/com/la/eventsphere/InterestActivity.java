package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class InterestActivity extends AppCompatActivity {

    // Interest cards
    private MaterialCardView cardMusic;
    private MaterialCardView cardSports;
    private MaterialCardView cardCulture;
    private MaterialCardView cardHackathons;
    private MaterialCardView cardWorkshops;
    private MaterialCardView cardCompetitions;
    private MaterialCardView cardFests;
    private MaterialCardView cardConcerts;
    private MaterialCardView cardCareer;
    private MaterialCardView cardArts;

    // UI
    private TextView tvSelectedCount;
    private MaterialButton btnContinue;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Selected interests
    private final List<String> selectedInterests = new ArrayList<>();

    // Purple theme color
    private final int PURPLE = 0xFF5033D8;
    private final int WHITE = 0xFFFFFFFF;
    private final int LIGHT_PURPLE = 0xFFF0EDFF;
    private final int BORDER_COLOR = 0xFFE3E0EF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_interest);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Connect XML elements
        initializeViews();

        // Set card click listeners
        setupInterestListeners();

        // Continue button
        btnContinue.setOnClickListener(v -> saveInterests());

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> {

            // Don't allow first-time user to skip interest selection
            Toast.makeText(
                    InterestActivity.this,
                    "Please select your top 3 interests to continue.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void initializeViews() {

        cardMusic = findViewById(R.id.cardMusic);
        cardSports = findViewById(R.id.cardSports);
        cardCulture = findViewById(R.id.cardCulture);
        cardHackathons = findViewById(R.id.cardHackathons);
        cardWorkshops = findViewById(R.id.cardWorkshops);
        cardCompetitions = findViewById(R.id.cardCompetitions);
        cardFests = findViewById(R.id.cardFests);
        cardConcerts = findViewById(R.id.cardConcerts);
        cardCareer = findViewById(R.id.cardCareer);
        cardArts = findViewById(R.id.cardArts);

        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        btnContinue = findViewById(R.id.btnContinue);
    }

    private void setupInterestListeners() {

        cardMusic.setOnClickListener(v ->
                toggleInterest(cardMusic, "Music"));

        cardSports.setOnClickListener(v ->
                toggleInterest(cardSports, "Sports"));

        cardCulture.setOnClickListener(v ->
                toggleInterest(cardCulture, "Cultural Events"));

        cardHackathons.setOnClickListener(v ->
                toggleInterest(cardHackathons, "Hackathons"));

        cardWorkshops.setOnClickListener(v ->
                toggleInterest(cardWorkshops, "Workshops"));

        cardCompetitions.setOnClickListener(v ->
                toggleInterest(cardCompetitions, "Competitions"));

        cardFests.setOnClickListener(v ->
                toggleInterest(cardFests, "Fests"));

        cardConcerts.setOnClickListener(v ->
                toggleInterest(cardConcerts, "Concerts"));

        cardCareer.setOnClickListener(v ->
                toggleInterest(cardCareer, "Career & Networking"));

        cardArts.setOnClickListener(v ->
                toggleInterest(cardArts, "Arts & Creativity"));
    }

    private void toggleInterest(
            MaterialCardView card,
            String interest
    ) {

        // If already selected → remove it
        if (selectedInterests.contains(interest)) {

            selectedInterests.remove(interest);

            setCardUnselected(card);

        }

        // If not selected → add it
        else {

            // Maximum 3 selections
            if (selectedInterests.size() >= 3) {

                Toast.makeText(
                        InterestActivity.this,
                        "You can select only 3 interests.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            selectedInterests.add(interest);

            setCardSelected(card);
        }

        updateSelectionCount();
    }

    private void setCardSelected(MaterialCardView card) {

        card.setCardBackgroundColor(LIGHT_PURPLE);
        card.setStrokeColor(PURPLE);
        card.setStrokeWidth(3);
    }

    private void setCardUnselected(MaterialCardView card) {

        card.setCardBackgroundColor(WHITE);
        card.setStrokeColor(BORDER_COLOR);
        card.setStrokeWidth(1);
    }

    private void updateSelectionCount() {

        int count = selectedInterests.size();

        tvSelectedCount.setText(
                "Selected " + count + " / 3"
        );

        // Enable Continue only when exactly 3 are selected
        if (count == 3) {

            btnContinue.setEnabled(true);
            btnContinue.setAlpha(1.0f);

        } else {

            btnContinue.setEnabled(false);
            btnContinue.setAlpha(0.5f);
        }
    }

    private void saveInterests() {

        // Safety check
        if (selectedInterests.size() != 3) {

            Toast.makeText(
                    InterestActivity.this,
                    "Please select exactly 3 interests.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Make sure the user is logged in
        if (currentUser == null) {

            Toast.makeText(
                    InterestActivity.this,
                    "Session expired. Please login again.",
                    Toast.LENGTH_LONG
            ).show();

            Intent intent = new Intent(
                    InterestActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);
            finish();

            return;
        }

        String uid = currentUser.getUid();

        // Save interests to Firestore
        db.collection("users")
                .document(uid)
                .update("interests", selectedInterests)
                .addOnSuccessListener(aVoid -> {

                    Toast.makeText(
                            InterestActivity.this,
                            "Your interests have been saved!",
                            Toast.LENGTH_SHORT
                    ).show();

                    // Go to Home
                    Intent intent = new Intent(
                            InterestActivity.this,
                            HomeActivity.class
                    );

                    startActivity(intent);
                    finish();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            InterestActivity.this,
                            "Unable to save interests. Please try again.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}