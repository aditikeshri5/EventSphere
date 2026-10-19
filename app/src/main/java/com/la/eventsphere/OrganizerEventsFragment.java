package com.la.eventsphere;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEventsFragment extends Fragment {

    private RecyclerView recyclerEvents;
    private LinearLayout layoutNoEvents;
    private TextView tvEventsCount;

    private final List<Event> eventList = new ArrayList<>();
    private OrganizerEventAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration eventsListener;

    public OrganizerEventsFragment() {
        super(R.layout.fragment_organizer_events);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerEvents = view.findViewById(R.id.recyclerOrganizerEvents);
        layoutNoEvents = view.findViewById(R.id.layoutNoOrganizerEvents);
        tvEventsCount = view.findViewById(R.id.tvOrganizerEventsCount);

        recyclerEvents.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        adapter = new OrganizerEventAdapter(eventList);
        recyclerEvents.setAdapter(adapter);

        Button btnAdd = view.findViewById(R.id.btnAddOrganizerEvent);

        btnAdd.setOnClickListener(v -> {
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

        loadOrganizerEvents();
    }

    private void loadOrganizerEvents() {

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            showEmptyState();
            return;
        }

        String organizerId = user.getUid();

        // IMPORTANT: Firestore field is organizerID, not organizerId.
        eventsListener = db.collection("events")
                .whereEqualTo("organizerID", organizerId)
                .addSnapshotListener((snapshot, error) -> {

                    if (!isAdded() || recyclerEvents == null) {
                        return;
                    }

                    if (error != null) {
                        Toast.makeText(
                                requireContext(),
                                "Unable to load events: " + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    eventList.clear();

                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {

                            Event event = document.toObject(Event.class);

                            // Store the actual Firestore document ID.
                            event.setEventId(document.getId());

                            eventList.add(event);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    tvEventsCount.setText(
                            "Your events (" + eventList.size() + ")"
                    );

                    if (eventList.isEmpty()) {
                        showEmptyState();
                    } else {
                        recyclerEvents.setVisibility(View.VISIBLE);
                        layoutNoEvents.setVisibility(View.GONE);
                    }
                });
    }

    private void showEmptyState() {

        if (recyclerEvents != null && layoutNoEvents != null) {
            recyclerEvents.setVisibility(View.GONE);
            layoutNoEvents.setVisibility(View.VISIBLE);
        }

        if (tvEventsCount != null) {
            tvEventsCount.setText("Your events (0)");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (eventsListener != null) {
            eventsListener.remove();
            eventsListener = null;
        }

        recyclerEvents = null;
        layoutNoEvents = null;
        tvEventsCount = null;
    }
}
