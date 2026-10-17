package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Connect XML elements
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // Back button: return to role selection
        TextView btnBackToMain = findViewById(R.id.btnBackToMain);

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

        // Validate email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email");
            etEmail.requestFocus();
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter your password");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);

        // Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser firebaseUser =
                                mAuth.getCurrentUser();

                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();

                            // Check Firestore user profile
                            checkUserProfile(uid);
                        } else {
                            showLoginError(
                                    "Unable to retrieve your account."
                            );
                        }

                    } else {

                        String errorMessage = "Login failed.";

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
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        DocumentSnapshot document = task.getResult();

                        if (document != null && document.exists()) {

                            // User profile exists
                            checkInterests(document);

                        } else {

                            // Account has no user profile
                            mAuth.signOut();

                            showLoginError(
                                    "No user profile found. Please register."
                            );
                        }

                    } else {

                        mAuth.signOut();

                        showLoginError(
                                "Unable to check your profile. Please try again."
                        );
                    }
                });
    }

    private void checkInterests(DocumentSnapshot document) {

        List<String> interests =
                (List<String>) document.get("interests");

        if (interests == null || interests.size() < 3) {

            // User has not completed interest selection
            Intent intent = new Intent(
                    LoginActivity.this,
                    InterestActivity.class
            );

            startActivity(intent);
            finish();

        } else {

            // User has already selected three interests
            Intent intent = new Intent(
                    LoginActivity.this,
                    HomeActivity.class
            );

            startActivity(intent);
            finish();
        }
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