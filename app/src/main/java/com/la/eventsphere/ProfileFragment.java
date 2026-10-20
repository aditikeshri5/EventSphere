package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvProfileAvatar;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileInterests;

    private TextView tvUserFullName;
    private TextView tvUserAccountEmail;
    private TextView tvUserPhone;

    private MaterialButton btnEditUserProfile;
    private MaterialButton btnEditInterests;
    private MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public ProfileFragment() {
        // Required empty public constructor
    }

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

        // Initialize existing profile views
        tvProfileAvatar = view.findViewById(R.id.tvProfileAvatar);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        tvProfileInterests = view.findViewById(R.id.tvProfileInterests);

        // Initialize account information views
        tvUserFullName = view.findViewById(R.id.tvUserFullName);
        tvUserAccountEmail = view.findViewById(R.id.tvUserAccountEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);

        // Initialize buttons
        btnEditUserProfile =
                view.findViewById(R.id.btnEditUserProfile);

        btnEditInterests =
                view.findViewById(R.id.btnEditInterests);

        btnLogout =
                view.findViewById(R.id.btnLogout);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Load user profile
        loadProfile();

        // Edit profile button
        btnEditUserProfile.setOnClickListener(v ->
                showEditProfileDialog()
        );

        // Keep existing Edit Interests functionality
        btnEditInterests.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    InterestActivity.class
            );
            startActivity(intent);
        });

        // Keep existing Logout functionality
        btnLogout.setOnClickListener(v -> logoutUser());

        return view;
    }

    // Load profile information from Firestore
    private void loadProfile() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || !isAdded()) {
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!isAdded()) {
                        return;
                    }

                    if (!document.exists()) {
                        Toast.makeText(
                                requireContext(),
                                "Profile information not found.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // Name
                    String name = document.getString("name");

                    if (name == null || name.trim().isEmpty()) {
                        name = "User";
                    }

                    tvProfileName.setText(name);
                    tvUserFullName.setText(name);

                    String firstLetter =
                            name.substring(0, 1).toUpperCase();

                    tvProfileAvatar.setText(firstLetter);

                    // Email
                    String email = document.getString("email");

                    if (email == null || email.trim().isEmpty()) {
                        email = currentUser.getEmail();
                    }

                    if (email == null) {
                        email = "Not provided";
                    }

                    tvProfileEmail.setText(email);
                    tvUserAccountEmail.setText(email);

                    // Phone field uses the exact Firestore key "Phone"
                    String phone = document.getString("Phone");

                    if (phone == null || phone.trim().isEmpty()) {
                        phone = "Not provided";
                    }

                    tvUserPhone.setText(phone);

                    // Interests
                    List<String> interests =
                            (List<String>) document.get("interests");

                    if (interests == null || interests.isEmpty()) {

                        tvProfileInterests.setText(
                                "No interests selected."
                        );

                    } else {

                        StringBuilder interestText =
                                new StringBuilder();

                        for (int i = 0; i < interests.size(); i++) {

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

                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(
                            requireContext(),
                            "Unable to load profile: "
                                    + e.getLocalizedMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Display Edit Profile dialog
    private void showEditProfileDialog() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    requireContext(),
                    "Please log in again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Name input
        EditText inputName = new EditText(requireContext());
        inputName.setHint("Full Name");
        inputName.setSingleLine(true);
        inputName.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS
        );
        inputName.setText(tvUserFullName.getText().toString());

        // Phone input
        EditText inputPhone = new EditText(requireContext());
        inputPhone.setHint("Contact Number");
        inputPhone.setSingleLine(true);
        inputPhone.setInputType(InputType.TYPE_CLASS_PHONE);

        String existingPhone = tvUserPhone.getText().toString();

        if (!existingPhone.equals("Not provided")) {
            inputPhone.setText(existingPhone);
        }

        // Dialog form
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (24 * getResources()
                .getDisplayMetrics().density);

        form.setPadding(padding, 0, padding, 0);

        form.addView(inputName);

        LinearLayout.LayoutParams phoneParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        phoneParams.topMargin = (int) (12 * getResources()
                .getDisplayMetrics().density);

        form.addView(inputPhone, phoneParams);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(ignored -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {

                        String updatedName =
                                inputName.getText()
                                        .toString()
                                        .trim();

                        String updatedPhone =
                                inputPhone.getText()
                                        .toString()
                                        .trim();

                        if (updatedName.isEmpty()) {
                            inputName.setError("Enter your name");
                            return;
                        }

                        String uid = currentUser.getUid();

                        // Save fields to users/{uid}
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("name", updatedName);
                        updates.put("Phone", updatedPhone);

                        db.collection("users")
                                .document(uid)
                                .update(updates)
                                .addOnSuccessListener(unused -> {

                                    if (!isAdded()) {
                                        return;
                                    }

                                    Toast.makeText(
                                            requireContext(),
                                            "Profile updated successfully!",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    // Refresh displayed information
                                    loadProfile();

                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {

                                    if (!isAdded()) {
                                        return;
                                    }

                                    Toast.makeText(
                                            requireContext(),
                                            "Failed to update profile: "
                                                    + e.getLocalizedMessage(),
                                            Toast.LENGTH_LONG
                                    ).show();
                                });
                    });
        });

        dialog.show();
    }

    // Logout
    private void logoutUser() {

        mAuth.signOut();

        Intent intent = new Intent(
                requireContext(),
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
    }

    // Refresh profile when returning from Edit Interests
    @Override
    public void onResume() {
        super.onResume();

        if (mAuth != null) {
            loadProfile();
        }
    }
}
