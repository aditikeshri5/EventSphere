
package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class OrganizerDashboardFragment extends Fragment {

    private TextView tvTotalEvents;
    private TextView tvTotalRegistrations;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ListenerRegistration eventsListener;
    private ListenerRegistration registrationsListener;

    public OrganizerDashboardFragment() {
        super(R.layout.fragment_organizer_dashboard);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTotalEvents =
                view.findViewById(R.id.tvOrganizerTotalEvents);

        tvTotalRegistrations =
                view.findViewById(R.id.tvOrganizerTotalRegistrations);

        Button btnCreate =
                view.findViewById(R.id.btnCreateOrganizerEvent);

        Button btnManage =
                view.findViewById(R.id.btnManageOrganizerEvents);

        Button btnRegistrations =
                view.findViewById(R.id.btnViewOrganizerRegistrations);

        btnCreate.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(
                        requireContext(),
                        "Please log in again.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            startActivity(
                    new Intent(
                            requireContext(),
                            CreateEventActivity.class
                    )
            );
        });

        btnManage.setOnClickListener(v -> {
            View nav = requireActivity().findViewById(
                    R.id.navOrganizerEvents
            );

            if (nav != null) {
                nav.performClick();
            }
        });

        btnRegistrations.setOnClickListener(v -> {
            View nav = requireActivity().findViewById(
                    R.id.navOrganizerRegistrations
            );

            if (nav != null) {
                nav.performClick();
            }
        });

        loadDashboardStatistics();
    }

    private void loadDashboardStatistics() {

        removeListeners();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            tvTotalEvents.setText("0");
            tvTotalRegistrations.setText("0");
            return;
        }

        String organizerId = user.getUid();

        tvTotalEvents.setText("...");
        tvTotalRegistrations.setText("...");

        // Count this organizer's events in real time.
        eventsListener = db.collection("events")
                .whereEqualTo("organizerID", organizerId)
                .addSnapshotListener((snapshot, error) -> {

                    if (!isAdded() || tvTotalEvents == null) {
                        return;
                    }

                    if (error != null) {
                        tvTotalEvents.setText("—");
                        Toast.makeText(
                                requireContext(),
                                "Unable to load events count.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (snapshot != null) {
                        tvTotalEvents.setText(
                                String.valueOf(snapshot.size())
                        );
                    }
                });

        // Count successful registrations for this organizer in real time.
        registrationsListener = db.collection("registrations")
                .whereEqualTo("organizerID", organizerId)
                .whereEqualTo("status", "registered")
                .addSnapshotListener((snapshot, error) -> {

                    if (!isAdded() || tvTotalRegistrations == null) {
                        return;
                    }

                    if (error != null) {
                        tvTotalRegistrations.setText("—");
                        Toast.makeText(
                                requireContext(),
                                "Unable to load registrations count.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (snapshot != null) {
                        tvTotalRegistrations.setText(
                                String.valueOf(snapshot.size())
                        );
                    }
                });
    }

    private void removeListeners() {
        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }

        if (registrationsListener != null) {
            registrationsListener.remove();
            registrationsListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        removeListeners();

        tvTotalEvents = null;
        tvTotalRegistrations = null;

        super.onDestroyView();
    }
}
