package com.la.eventsphere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class OrganizerRegistrationsFragment extends Fragment {

    private RecyclerView recyclerRegistrations;
    private TextView tvNoRegistrations;
    private RegistrationAdapter adapter;

    private final List<RegistrationItem> registrationList =
            new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    // Listeners for the latest event details.
    private final List<ListenerRegistration> eventListeners =
            new ArrayList<>();

    public OrganizerRegistrationsFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_organizer_registrations,
                container,
                false
        );

        recyclerRegistrations =
                view.findViewById(R.id.recyclerRegistrations);

        tvNoRegistrations =
                view.findViewById(R.id.tvNoRegistrations);

        db = FirebaseFirestore.getInstance();

        recyclerRegistrations.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new RegistrationAdapter(registrationList);
        recyclerRegistrations.setAdapter(adapter);

        loadRegistrations();

        return view;
    }

    private void loadRegistrations() {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            showEmpty("Please log in to view registrations.");
            return;
        }

        String organizerId = user.getUid();

        listener = db.collection("registrations")
                .whereEqualTo("organizerID", organizerId)
                .addSnapshotListener((snapshot, error) -> {

                    if (!isAdded()
                            || recyclerRegistrations == null
                            || tvNoRegistrations == null) {
                        return;
                    }

                    if (error != null) {
                        showEmpty(
                                "Unable to load registrations. "
                                        + "Please check Firestore rules."
                        );
                        return;
                    }

                    // Remove old event listeners before rebuilding the list.
                    removeEventListeners();

                    registrationList.clear();

                    if (snapshot == null || snapshot.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        showEmpty(
                                "No registrations for your events yet."
                        );
                        return;
                    }

                    recyclerRegistrations.setVisibility(View.VISIBLE);
                    tvNoRegistrations.setVisibility(View.GONE);

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        RegistrationItem item =
                                new RegistrationItem();

                        item.registrationId = doc.getId();
                        item.eventId = getValue(doc, "eventId", "");

                        item.userName = getValue(
                                doc, "userName", "Attendee"
                        );

                        item.userEmail = getValue(
                                doc, "userEmail", "Email unavailable"
                        );

                        item.eventTitle = getValue(
                                doc, "eventTitle", "Event"
                        );

                        // Initial fallback from the registration document.
                        item.eventDate = getValue(
                                doc, "eventDate", ""
                        );

                        item.status = getValue(
                                doc, "status", "registered"
                        );

                        registrationList.add(item);

                        // Fetch and listen to the current event document.
                        if (!item.eventId.isEmpty()) {
                            listenToLatestEvent(item);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (registrationList.isEmpty()) {
                        showEmpty(
                                "No registrations for your events yet."
                        );
                    }
                });
    }

    private void listenToLatestEvent(RegistrationItem item) {

        ListenerRegistration eventListener =
                db.collection("events")
                        .document(item.eventId)
                        .addSnapshotListener((eventDoc, error) -> {

                            if (!isAdded()
                                    || recyclerRegistrations == null
                                    || adapter == null) {
                                return;
                            }

                            if (error != null
                                    || eventDoc == null
                                    || !eventDoc.exists()) {
                                // Keep the saved registration details
                                // if the event cannot be loaded.
                                return;
                            }

                            // Read the latest values from the events
                            // collection, not the old registration copy.
                            String latestDate =
                                    eventDoc.getString("date");

                            String latestTitle =
                                    eventDoc.getString("title");

                            if (latestDate != null
                                    && !latestDate.trim().isEmpty()) {
                                item.eventDate = latestDate;
                            }

                            if (latestTitle != null
                                    && !latestTitle.trim().isEmpty()) {
                                item.eventTitle = latestTitle;
                            }

                            int index = registrationList.indexOf(item);

                            if (index >= 0) {
                                adapter.notifyItemChanged(index);
                            }
                        });

        eventListeners.add(eventListener);
    }

    private String getValue(
            DocumentSnapshot doc,
            String field,
            String fallback) {

        String value = doc.getString(field);

        return value == null || value.trim().isEmpty()
                ? fallback
                : value;
    }

    private void showEmpty(String message) {

        if (tvNoRegistrations != null) {
            tvNoRegistrations.setText(message);
            tvNoRegistrations.setVisibility(View.VISIBLE);
        }

        if (recyclerRegistrations != null) {
            recyclerRegistrations.setVisibility(View.GONE);
        }
    }

    private void removeEventListeners() {

        for (ListenerRegistration eventListener : eventListeners) {
            eventListener.remove();
        }

        eventListeners.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (listener != null) {
            listener.remove();
            listener = null;
        }

        removeEventListeners();

        recyclerRegistrations = null;
        tvNoRegistrations = null;
        adapter = null;
    }

    public static class RegistrationItem {

        String registrationId;
        String eventId;
        String userName;
        String userEmail;
        String eventTitle;
        String eventDate;
        String status;
    }

    private static class RegistrationAdapter
            extends RecyclerView.Adapter<
            RegistrationAdapter.RegistrationViewHolder> {

        private final List<RegistrationItem> items;

        RegistrationAdapter(List<RegistrationItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public RegistrationViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(
                            R.layout.item_organizer_registration,
                            parent,
                            false
                    );

            return new RegistrationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull RegistrationViewHolder holder,
                int position) {

            RegistrationItem item = items.get(position);

            holder.tvAttendeeName.setText(item.userName);
            holder.tvAttendeeEmail.setText(item.userEmail);
            holder.tvRegisteredEvent.setText(item.eventTitle);

            String dateText = item.eventDate == null
                    || item.eventDate.isEmpty()
                    ? "Date unavailable"
                    : item.eventDate;

            holder.tvRegistrationDate.setText(dateText);

            holder.tvRegistrationStatus.setText(
                    "Status: " + item.status
            );
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class RegistrationViewHolder
                extends RecyclerView.ViewHolder {

            TextView tvAttendeeName;
            TextView tvAttendeeEmail;
            TextView tvRegisteredEvent;
            TextView tvRegistrationDate;
            TextView tvRegistrationStatus;

            RegistrationViewHolder(@NonNull View itemView) {
                super(itemView);

                tvAttendeeName =
                        itemView.findViewById(R.id.tvAttendeeName);

                tvAttendeeEmail =
                        itemView.findViewById(R.id.tvAttendeeEmail);

                tvRegisteredEvent =
                        itemView.findViewById(R.id.tvRegisteredEvent);

                tvRegistrationDate =
                        itemView.findViewById(R.id.tvRegistrationDate);

                tvRegistrationStatus =
                        itemView.findViewById(R.id.tvRegistrationStatus);
            }
        }
    }
}
