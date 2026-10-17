package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileInterests;

    private MaterialButton btnEditInterests;
    private MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ProfileFragment() {
        // Required empty public constructor
    }


    // =========================================================
    // CREATE VIEW
    // =========================================================

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_profile,
                container,
                false
        );


        // -----------------------------
        // INITIALIZE VIEWS
        // -----------------------------

        tvProfileAvatar =
                view.findViewById(R.id.tvProfileAvatar);

        tvProfileName =
                view.findViewById(R.id.tvProfileName);

        tvProfileEmail =
                view.findViewById(R.id.tvProfileEmail);

        tvProfileInterests =
                view.findViewById(R.id.tvProfileInterests);

        btnEditInterests =
                view.findViewById(R.id.btnEditInterests);

        btnLogout =
                view.findViewById(R.id.btnLogout);


        // -----------------------------
        // FIREBASE
        // -----------------------------

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // -----------------------------
        // LOAD PROFILE
        // -----------------------------

        loadProfile();


        // -----------------------------
        // EDIT INTERESTS
        // -----------------------------

        btnEditInterests.setOnClickListener(v -> {

            Intent intent = new Intent(
                    requireContext(),
                    InterestActivity.class
            );

            startActivity(intent);
        });


        // -----------------------------
        // LOGOUT
        // -----------------------------

        btnLogout.setOnClickListener(v -> logoutUser());


        return view;
    }


    // =========================================================
    // LOAD USER PROFILE
    // =========================================================

    private void loadProfile() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        if (currentUser == null) {

            return;
        }


        String uid = currentUser.getUid();


        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        Toast.makeText(
                                requireContext(),
                                "Profile information not found.",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }


                    // -----------------------------
                    // NAME
                    // -----------------------------

                    String name =
                            document.getString("name");


                    if (name != null && !name.isEmpty()) {

                        tvProfileName.setText(name);

                        tvProfileAvatar.setText(
                                name.substring(0, 1).toUpperCase()
                        );

                    } else {

                        tvProfileName.setText("User");
                        tvProfileAvatar.setText("U");
                    }


                    // -----------------------------
                    // EMAIL
                    // -----------------------------

                    String email =
                            document.getString("email");


                    if (email != null) {

                        tvProfileEmail.setText(email);

                    } else {

                        tvProfileEmail.setText(
                                currentUser.getEmail()
                        );
                    }


                    // -----------------------------
                    // INTERESTS
                    // -----------------------------

                    List<String> interests =
                            (List<String>) document.get("interests");


                    if (interests == null ||
                            interests.isEmpty()) {

                        tvProfileInterests.setText(
                                "No interests selected."
                        );

                    } else {

                        StringBuilder interestText =
                                new StringBuilder();

                        for (int i = 0;
                             i < interests.size();
                             i++) {

                            interestText.append("• ")
                                    .append(interests.get(i));

                            if (i < interests.size() - 1) {

                                interestText.append("\n");
                            }
                        }

                        tvProfileInterests.setText(
                                interestText.toString()
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            requireContext(),
                            "Unable to load profile.",
                            Toast.LENGTH_SHORT
                    ).show();

                });
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    private void logoutUser() {

        mAuth.signOut();


        Intent intent = new Intent(
                requireContext(),
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );


        startActivity(intent);
    }


    // =========================================================
    // REFRESH PROFILE WHEN RETURNING
    // =========================================================

    @Override
    public void onResume() {

        super.onResume();

        if (mAuth != null) {

            loadProfile();
        }
    }
}