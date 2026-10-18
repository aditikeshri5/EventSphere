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
                false);

        recyclerRegistrations =
                view.findViewById(R.id.recyclerRegistrations);

        tvNoRegistrations =
                view.findViewById(R.id.tvNoRegistrations);

        db = FirebaseFirestore.getInstance();

        recyclerRegistrations.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        adapter = new RegistrationAdapter(registrationList);
        recyclerRegistrations.setAdapter(adapter);

        loadRegistrations();

        return view;
    }

    private void loadRegistrations() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            showEmpty("Please log in to view registrations.");
            return;
        }

        listener = db.collection("registrations")
                .whereEqualTo("organizerID", user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (!isAdded()) return;

                    if (error != null) {
                        showEmpty("Unable to load registrations. Check Firestore rules.");
                        return;
                    }

                    registrationList.clear();

                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            RegistrationItem item = new RegistrationItem();

                            item.userName = getValue(doc, "userName", "Attendee");
                            item.userEmail = getValue(doc, "userEmail", "Email unavailable");
                            item.eventTitle = getValue(doc, "eventTitle", "Event");
                            item.eventDate = getValue(doc, "eventDate", "");
                            item.status = getValue(doc, "status", "registered");

                            registrationList.add(item);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    if (registrationList.isEmpty()) {
                        showEmpty("No registrations for your events yet.");
                    } else {
                        tvNoRegistrations.setVisibility(View.GONE);
                        recyclerRegistrations.setVisibility(View.VISIBLE);
                    }
                });
    }

    private String getValue(DocumentSnapshot doc,
                            String field,
                            String fallback) {
        String value = doc.getString(field);
        return value == null || value.trim().isEmpty()
                ? fallback : value;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (listener != null) {
            listener.remove();
            listener = null;
        }
    }

    public static class RegistrationItem {
        String userName;
        String userEmail;
        String eventTitle;
        String eventDate;
        String status;
    }

    private static class RegistrationAdapter
            extends RecyclerView.Adapter<RegistrationAdapter.RegistrationViewHolder> {

        private final List<RegistrationItem> items;

        RegistrationAdapter(List<RegistrationItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public RegistrationViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_organizer_registration,
                    parent,
                    false);

            return new RegistrationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                @NonNull RegistrationViewHolder holder, int position) {

            RegistrationItem item = items.get(position);

            holder.tvAttendeeName.setText(item.userName);
            holder.tvAttendeeEmail.setText(item.userEmail);
            holder.tvRegisteredEvent.setText(item.eventTitle);

            String dateText = item.eventDate.isEmpty()
                    ? "Date unavailable" : item.eventDate;

            holder.tvRegistrationDate.setText(dateText);
            holder.tvRegistrationStatus.setText(
                    "Status: " + item.status);
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
