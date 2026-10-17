
package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText etName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private TextInputEditText etConfirmPassword;
    private Button btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private boolean isRegistering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Connect XML fields
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Register button
        btnRegister.setOnClickListener(v -> registerUser());

        // Return to Login
        findViewById(R.id.tvLogin).setOnClickListener(v -> finish());

        // Back arrow
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void registerUser() {

        if (isRegistering) {
            return;
        }

        String name = etName.getText() == null
                ? ""
                : etName.getText().toString().trim();

        String email = etEmail.getText() == null
                ? ""
                : etEmail.getText().toString().trim();

        String password = etPassword.getText() == null
                ? ""
                : etPassword.getText().toString();

        String confirmPassword = etConfirmPassword.getText() == null
                ? ""
                : etConfirmPassword.getText().toString();

        // Validate name
        if (TextUtils.isEmpty(name)) {
            etName.setError("Please enter your full name.");
            etName.requestFocus();
            return;
        }

        // Validate email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Please enter your email address.");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address.");
            etEmail.requestFocus();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter a password.");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError(
                    "Your password must contain at least 6 characters."
            );
            etPassword.requestFocus();
            return;
        }

        // Validate confirmation
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your password.");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("The passwords do not match.");
            etConfirmPassword.requestFocus();
            return;
        }

        // Prevent repeated registration requests
        isRegistering = true;
        btnRegister.setEnabled(false);
        btnRegister.setText("Creating account...");

        // Create Firebase Authentication account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        String message = "Unable to create your account.";

                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }

                        Log.e(TAG, "Authentication registration failed",
                                task.getException());

                        showRegistrationError(message);
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();

                    if (firebaseUser == null) {
                        showRegistrationError(
                                "Your account was created, but the user session could not be loaded."
                        );
                        return;
                    }

                    String uid = firebaseUser.getUid();

                    // Prepare Firestore user profile
                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("name", name);
                    user.put("email", email);
                    user.put("interests", new ArrayList<String>());

                    // Save profile to users/{uid}
                    db.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(unused -> {

                                // Registration is complete.
                                // Require the user to log in manually.
                                mAuth.signOut();

                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Account created successfully! Please log in.",
                                        Toast.LENGTH_LONG
                                ).show();

                                // Clear registration from the back stack
                                Intent intent = new Intent(
                                        RegisterActivity.this,
                                        LoginActivity.class
                                );

                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                | Intent.FLAG_ACTIVITY_NEW_TASK
                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );

                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {

                                Log.e(TAG,
                                        "Failed to save Firestore user profile",
                                        e);

                                /*
                                 * Authentication account already exists.
                                 * Keep the error visible so the problem
                                 * can be diagnosed instead of pretending
                                 * registration fully succeeded.
                                 */
                                showRegistrationError(
                                        "Your account was created, but your profile could not be saved. "
                                                + e.getLocalizedMessage()
                                );
                            });
                });
    }

    private void showRegistrationError(String message) {

        isRegistering = false;
        btnRegister.setEnabled(true);
        btnRegister.setText("Create account");

        Toast.makeText(
                RegisterActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
