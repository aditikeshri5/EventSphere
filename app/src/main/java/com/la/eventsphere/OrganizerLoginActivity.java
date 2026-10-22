
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

public class OrganizerLoginActivity extends AppCompatActivity {

    private EditText etOrganizerEmail, etOrganizerPassword;
    private Button btnOrganizerLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_login);

        // Initialize Firebase and session manager
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Connect XML elements
        etOrganizerEmail = findViewById(R.id.etOrganizerEmail);
        etOrganizerPassword = findViewById(R.id.etOrganizerPassword);
        btnOrganizerLogin = findViewById(R.id.btnOrganizerLogin);

        TextView btnBack = findViewById(R.id.btnOrganizerBack);
        TextView tvOrganizerRegister =
                findViewById(R.id.tvOrganizerRegister);

        // Back button: return to the previous screen
        btnBack.setOnClickListener(v -> finish());

        // Organizer login button
        btnOrganizerLogin.setOnClickListener(v -> loginOrganizer());

        // Open organizer registration
        tvOrganizerRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    OrganizerLoginActivity.this,
                    OrganizerRegistrationActivity.class
            );
            startActivity(intent);
        });
    }

    private void loginOrganizer() {

        String email = etOrganizerEmail.getText().toString().trim();
        String password = etOrganizerPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etOrganizerEmail.setError("Enter your email");
            etOrganizerEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etOrganizerPassword.setError("Enter your password");
            etOrganizerPassword.requestFocus();
            return;
        }

        btnOrganizerLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            verifyOrganizer(user);
                        } else {
                            showLoginError(
                                    "Unable to verify your account."
                            );
                        }

                    } else {

                        String message =
                                "Login failed. Please check your credentials.";

                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }

                        showLoginError(message);
                    }
                });
    }

    private void verifyOrganizer(FirebaseUser user) {

        String uid = user.getUid();

        db.collection("organizers")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        // Save the organizer session only after
                        // verifying the organizer's Firestore profile.
                        sessionManager.saveSession("organizer");

                        Toast.makeText(
                                OrganizerLoginActivity.this,
                                "Welcome, Organizer!",
                                Toast.LENGTH_SHORT
                        ).show();

                        Intent intent = new Intent(
                                OrganizerLoginActivity.this,
                                OrganizerActivity.class
                        );

                        // Prevent Back from returning to login screens.
                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();

                    } else {

                        // The account exists in Firebase Authentication,
                        // but it is not registered as an organizer.
                        mAuth.signOut();
                        sessionManager.clearSession();

                        showLoginError(
                                "This account is not registered as an organizer."
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    // A Firestore/network error is not proof that
                    // the account is invalid, so don't sign out.
                    showLoginError(
                            "Unable to verify organizer access. Please try again."
                    );
                });
    }

    private void showLoginError(String message) {

        btnOrganizerLogin.setEnabled(true);

        Toast.makeText(
                OrganizerLoginActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
