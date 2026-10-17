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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.concurrent.atomic.AtomicInteger;

public class OrganizerDashboardFragment extends Fragment {

    private TextView tvTotalEvents;
    private TextView tvTotalRegistrations;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

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

        tvTotalEvents = view.findViewById(R.id.tvOrganizerTotalEvents);
        tvTotalRegistrations =
                view.findViewById(R.id.tvOrganizerTotalRegistrations);

        Button btnCreate =
                view.findViewById(R.id.btnCreateOrganizerEvent);
        Button btnManage =
                view.findViewById(R.id.btnManageOrganizerEvents);
        Button btnRegistrations =
                view.findViewById(R.id.btnViewOrganizerRegistrations);

        btnCreate.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();

            if (user == null) {
                Toast.makeText(
                        requireContext(),
                        "Please log in again.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            startActivity(
                    new Intent(requireContext(), CreateEventActivity.class)
            );
        });

        btnManage.setOnClickListener(v ->
                requireActivity()
                        .findViewById(R.id.navOrganizerEvents)
                        .performClick()
        );

        btnRegistrations.setOnClickListener(v ->
                requireActivity()
                        .findViewById(R.id.navOrganizerRegistrations)
                        .performClick()
        );

        loadDashboardStatistics();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (isAdded() && tvTotalEvents != null) {
            loadDashboardStatistics();
        }
    }

    private void loadDashboardStatistics() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            tvTotalEvents.setText("0");
            tvTotalRegistrations.setText("0");
            return;
        }

        String organizerId = user.getUid();

        tvTotalEvents.setText("...");
        tvTotalRegistrations.setText("...");

        // Only count events belonging to this organizer.
        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(eventDocuments -> {

                    if (!isAdded()) {
                        return;
                    }

                    tvTotalEvents.setText(
                            String.valueOf(eventDocuments.size())
                    );

                    if (eventDocuments.isEmpty()) {
                        tvTotalRegistrations.setText("0");
                        return;
                    }

                    AtomicInteger completedQueries = new AtomicInteger(0);
                    AtomicInteger registrationCount = new AtomicInteger(0);
                    int totalEvents = eventDocuments.size();

                    for (QueryDocumentSnapshot event : eventDocuments) {
                        String eventId = event.getId();

                        db.collection("registrations")
                                .whereEqualTo("eventId", eventId)
                                .get()
                                .addOnCompleteListener(task -> {

                                    if (task.isSuccessful()
                                            && task.getResult() != null) {
                                        registrationCount.addAndGet(
                                                task.getResult().size()
                                        );
                                    }

                                    int completed =
                                            completedQueries.incrementAndGet();

                                    if (completed == totalEvents && isAdded()) {
                                        tvTotalRegistrations.setText(
                                                String.valueOf(
                                                        registrationCount.get()
                                                )
                                        );
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        tvTotalEvents.setText("—");
                        tvTotalRegistrations.setText("—");

                        Toast.makeText(
                                requireContext(),
                                "Unable to load dashboard statistics.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
}
