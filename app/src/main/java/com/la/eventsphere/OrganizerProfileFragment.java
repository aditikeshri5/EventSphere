
package com.la.eventsphere;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class OrganizerProfileFragment extends Fragment {

    private TextView tvAvatar, tvProfileName, tvFullName;
    private TextView tvEmail, tvOrganization, tvPhone;

    private Button btnEditProfile, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String savedOrganization = "";
    private String savedPhone = "";

    public OrganizerProfileFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_organizer_profile,
                container,
                false
        );

        // Connect XML views
        tvAvatar = view.findViewById(R.id.tvOrganizerProfileAvatar);
        tvProfileName = view.findViewById(R.id.tvOrganizerProfileName);
        tvFullName = view.findViewById(R.id.tvOrganizerFullName);
        tvEmail = view.findViewById(R.id.tvOrganizerEmail);
        tvOrganization = view.findViewById(R.id.tvOrganizerOrganization);
        tvPhone = view.findViewById(R.id.tvOrganizerPhone);

        btnEditProfile = view.findViewById(R.id.btnEditOrganizerProfile);
        btnLogout = view.findViewById(R.id.btnOrganizerLogout);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Button listeners
        btnEditProfile.setOnClickListener(v -> showEditDialog());
        btnLogout.setOnClickListener(v -> logoutOrganizer());

        // Load organizer profile
        loadOrganizerProfile();

        return view;
    }

    // Load organizer profile from Firestore
    private void loadOrganizerProfile() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            showSignedOutState();
            return;
        }

        db.collection("organizers")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (!isAdded() || getView() == null) {
                        return;
                    }

                    String name = firstNonEmpty(
                            document.getString("name"),
                            document.getString("fullName"),
                            document.getString("organizerName"),
                            document.getString("displayName")
                    );

                    String email = firstNonEmpty(
                            document.getString("email"),
                            user.getEmail()
                    );

                    savedOrganization = firstNonEmpty(
                            document.getString("organization"),
                            document.getString("organizationName")
                    );

                    // Read older field names too, if they exist.
                    savedPhone = readPhone(document);

                    if (name.isEmpty()) {
                        name = email.contains("@")
                                ? email.substring(0, email.indexOf("@"))
                                : "Organizer";
                    }

                    tvProfileName.setText(name);
                    tvFullName.setText(name);
                    tvEmail.setText(displayValue(email));
                    tvOrganization.setText(displayValue(savedOrganization));
                    tvPhone.setText(displayValue(savedPhone));
                    tvAvatar.setText(name.substring(0, 1).toUpperCase());
                })
                .addOnFailureListener(error -> {

                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(
                            requireContext(),
                            "Could not load organizer profile.",
                            Toast.LENGTH_SHORT
                    ).show();

                    android.util.Log.e(
                            "OrganizerProfile",
                            "Profile loading failed",
                            error
                    );
                });
    }

    // Support different phone field names
    private String readPhone(DocumentSnapshot document) {

        String[] fields = {
                "phone",
                "Phone",
                "phoneNumber",
                "contactNumber",
                "Contact Number",
                "mobile"
        };

        for (String field : fields) {

            Object value = document.get(field);

            if (value instanceof String
                    && !((String) value).trim().isEmpty()) {

                return ((String) value).trim();
            }

            // Supports older documents where the phone was saved as a number.
            if (value instanceof Number) {

                long number = ((Number) value).longValue();

                if (number != 0) {
                    return String.valueOf(number);
                }
            }
        }

        return "";
    }

    private String firstNonEmpty(String... values) {

        for (String value : values) {

            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }

        return "";
    }

    private String displayValue(String value) {

        return value == null || value.trim().isEmpty()
                ? "Not provided"
                : value;
    }

    // Display Edit Organizer Profile dialog
    private void showEditDialog() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    requireContext(),
                    "Please log in again.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);

        layout.setPadding(padding, padding, padding, 0);

        // Organization input
        TextView organizationLabel = new TextView(requireContext());
        organizationLabel.setText("Organization");
        layout.addView(organizationLabel);

        EditText organizationInput = new EditText(requireContext());
        organizationInput.setSingleLine(true);
        organizationInput.setHint("Enter organization name");
        organizationInput.setText(savedOrganization);
        layout.addView(organizationInput);

        // Phone input
        TextView phoneLabel = new TextView(requireContext());
        phoneLabel.setText("Contact Number");

        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        labelParams.topMargin = padding / 2;
        layout.addView(phoneLabel, labelParams);

        EditText phoneInput = new EditText(requireContext());
        phoneInput.setSingleLine(true);
        phoneInput.setHint("Enter contact number");
        phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(savedPhone);
        layout.addView(phoneInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Edit Organizer Profile")
                .setView(layout)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
        ).setOnClickListener(v -> {

            String organization = organizationInput.getText()
                    .toString().trim();

            String phone = phoneInput.getText()
                    .toString().trim();

            if (!phone.isEmpty()
                    && !phone.matches("[0-9+() -]{7,20}")) {

                phoneInput.setError("Enter a valid contact number");
                return;
            }

            saveProfile(user.getUid(), organization, phone, dialog);
        }));

        dialog.show();
    }

    // Save organizer profile updates
    private void saveProfile(
            String uid,
            String organization,
            String phone,
            AlertDialog dialog) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("organization", organization);

        // Save phone as String to preserve leading zeros and country code.
        updates.put("phone", phone);

        db.collection("organizers")
                .document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    if (!isAdded() || getView() == null) {
                        return;
                    }

                    savedOrganization = organization;
                    savedPhone = phone;

                    tvOrganization.setText(displayValue(organization));
                    tvPhone.setText(displayValue(phone));

                    Toast.makeText(
                            requireContext(),
                            "Profile updated successfully!",
                            Toast.LENGTH_SHORT
                    ).show();

                    dialog.dismiss();
                })
                .addOnFailureListener(error -> {

                    if (!isAdded()) {
                        return;
                    }

                    Toast.makeText(
                            requireContext(),
                            "Could not save profile. Check Firestore rules.",
                            Toast.LENGTH_LONG
                    ).show();

                    android.util.Log.e(
                            "OrganizerProfile",
                            "Profile update failed",
                            error
                    );
                });
    }

    // Show signed-out state
    private void showSignedOutState() {

        tvProfileName.setText("Not signed in");
        tvFullName.setText("Not signed in");
        tvEmail.setText("Not provided");
        tvOrganization.setText("Not provided");
        tvPhone.setText("Not provided");
        tvAvatar.setText("O");
    }

    // Logout organizer
    private void logoutOrganizer() {

        // Clear SharedPreferences
        SessionManager sessionManager =
                new SessionManager(requireContext());

        sessionManager.clearSession();

        // Sign out from Firebase
        mAuth.signOut();

        // Navigate to MainActivity
        Intent intent = new Intent(
                requireActivity(),
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        requireActivity().finish();
    }
}
