
package com.la.eventsphere;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Connect XML views
        etOrganizerEmail = findViewById(R.id.etOrganizerEmail);
        etOrganizerPassword = findViewById(R.id.etOrganizerPassword);
        btnOrganizerLogin = findViewById(R.id.btnOrganizerLogin);

        TextView btnBack = findViewById(R.id.btnOrganizerBack);
        TextView tvOrganizerRegister =
                findViewById(R.id.tvOrganizerRegister);

        // Back button: close this activity and return immediately
        btnBack.setOnClickListener(v -> finish());

        // Organizer login
        btnOrganizerLogin.setOnClickListener(v -> loginOrganizer());

        // Open organizer registration screen
        tvOrganizerRegister.setOnClickListener(v -> {
            startActivity(
                    new android.content.Intent(
                            OrganizerLoginActivity.this,
                            OrganizerRegistrationActivity.class
                    )
            );
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

                        Toast.makeText(
                                OrganizerLoginActivity.this,
                                "Welcome, Organizer!",
                                Toast.LENGTH_SHORT
                        ).show();

                        android.content.Intent intent =
                                new android.content.Intent(
                                        OrganizerLoginActivity.this,
                                        OrganizerActivity.class
                                );

                        startActivity(intent);
                        finish();

                    } else {

                        mAuth.signOut();

                        showLoginError(
                                "This account is not registered as an organizer."
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    mAuth.signOut();

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
