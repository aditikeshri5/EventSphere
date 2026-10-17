package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class OrganizerRegistrationActivity extends AppCompatActivity {

    private EditText etOrganizerName;
    private EditText etOrganizerEmail;
    private EditText etOrganizerPassword;
    private EditText etOrganizerConfirmPassword;

    private Button btnOrganizerRegister;
    private TextView tvOrganizerLogin;
    private TextView btnOrganizerBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isRegistering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_registration);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Connect XML views
        etOrganizerName = findViewById(R.id.etOrganizerName);
        etOrganizerEmail = findViewById(R.id.etOrganizerEmail);
        etOrganizerPassword = findViewById(R.id.etOrganizerPassword);
        etOrganizerConfirmPassword =
                findViewById(R.id.etOrganizerConfirmPassword);

        btnOrganizerRegister = findViewById(R.id.btnOrganizerRegister);
        tvOrganizerLogin = findViewById(R.id.tvOrganizerLogin);
        btnOrganizerBack = findViewById(R.id.btnOrganizerBack);

        // Create organizer account
        btnOrganizerRegister.setOnClickListener(v -> registerOrganizer());

        // Navigate to organizer login
        tvOrganizerLogin.setOnClickListener(v -> openOrganizerLogin());

        // Back button
        btnOrganizerBack.setOnClickListener(v -> finish());
    }

    private void registerOrganizer() {

        if (isRegistering) {
            return;
        }

        String name = etOrganizerName.getText().toString().trim();
        String email = etOrganizerEmail.getText().toString().trim();
        String password = etOrganizerPassword.getText().toString();
        String confirmPassword =
                etOrganizerConfirmPassword.getText().toString();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            etOrganizerName.setError("Enter organizer name");
            etOrganizerName.requestFocus();
            return;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            etOrganizerEmail.setError("Enter email address");
            etOrganizerEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etOrganizerEmail.setError("Enter a valid email address");
            etOrganizerEmail.requestFocus();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            etOrganizerPassword.setError("Create a password");
            etOrganizerPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etOrganizerPassword.setError(
                    "Password must contain at least 6 characters"
            );
            etOrganizerPassword.requestFocus();
            return;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            etOrganizerConfirmPassword.setError("Confirm your password");
            etOrganizerConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etOrganizerConfirmPassword.setError("Passwords do not match");
            etOrganizerConfirmPassword.requestFocus();
            return;
        }

        // Prevent multiple registration requests
        isRegistering = true;
        btnOrganizerRegister.setEnabled(false);

        // Create account in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user == null) {
                            isRegistering = false;
                            btnOrganizerRegister.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "Account created, but user details could not be retrieved.",
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }

                        saveOrganizerProfile(user, name, email);

                    } else {

                        isRegistering = false;
                        btnOrganizerRegister.setEnabled(true);

                        String message = "Registration failed.";

                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }


    private void saveOrganizerProfile(
            FirebaseUser user,
            String name,
            String email
    ) {
        String uid = user.getUid();

        Map<String, Object> organizer = new HashMap<>();
        organizer.put("uid", uid);
        organizer.put("name", name);
        organizer.put("email", email);
        organizer.put("role", "organizer");

        // These details can be completed later in Profile
        organizer.put("organization", "");
        organizer.put("city", "");
        organizer.put("state", "");
        organizer.put("description", "");

        organizer.put("createdAt", FieldValue.serverTimestamp());

        db.collection("organizers")
                .document(uid)
                .set(organizer)
                .addOnSuccessListener(unused -> {
                    mAuth.signOut();

                    Toast.makeText(
                            OrganizerRegistrationActivity.this,
                            "Organizer account created successfully. Please log in.",
                            Toast.LENGTH_LONG
                    ).show();

                    openOrganizerLogin();
                })
                .addOnFailureListener(e -> {
                    isRegistering = false;
                    btnOrganizerRegister.setEnabled(true);

                    Toast.makeText(
                            OrganizerRegistrationActivity.this,
                            "Account created, but saving the organizer profile failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void openOrganizerLogin() {

        Intent intent = new Intent(
                OrganizerRegistrationActivity.this,
                OrganizerLoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }
}