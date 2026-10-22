
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase and session manager
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Connect XML elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        TextView btnBackToMain = findViewById(R.id.btnBackToMain);

        // Back button: return to role selection
        btnBackToMain.setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    MainActivity.class
            );

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // Login button
        btnLogin.setOnClickListener(v -> loginUser());

        // Create account link
        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }

    private void loginUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter your password");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser firebaseUser = mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            checkUserProfile(firebaseUser.getUid());
                        } else {
                            showLoginError(
                                    "Unable to retrieve your account."
                            );
                        }

                    } else {

                        String errorMessage =
                                "Login failed. Please check your credentials.";

                        if (task.getException() != null) {
                            errorMessage =
                                    task.getException().getMessage();
                        }

                        showLoginError(errorMessage);
                    }
                });
    }

    private void checkUserProfile(String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        // Save the regular-user session only after
                        // confirming that the Firestore profile exists.
                        sessionManager.saveSession("user");

                        checkInterests(document);

                    } else {

                        // This account has no regular-user profile.
                        mAuth.signOut();
                        sessionManager.clearSession();

                        showLoginError(
                                "No user profile found. Please register."
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    // Keep the Firebase session so the user can retry.
                    // Do not erase the session because of a network error.
                    showLoginError(
                            "Unable to check your profile. Please try again."
                    );
                });
    }

    private void checkInterests(DocumentSnapshot document) {

        List<String> interests = (List<String>) document.get("interests");

        Intent intent;

        if (interests == null || interests.size() < 3) {

            // First-time users or users who haven't selected
            // their three interests go to interest selection.
            intent = new Intent(
                    LoginActivity.this,
                    InterestActivity.class
            );

        } else {

            // Users with three interests go to the home dashboard.
            intent = new Intent(
                    LoginActivity.this,
                    HomeActivity.class
            );
        }

        // Remove login and earlier screens from the back stack.
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private void showLoginError(String message) {

        btnLogin.setEnabled(true);

        Toast.makeText(
                LoginActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
