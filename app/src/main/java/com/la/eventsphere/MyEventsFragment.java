
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

public class MyEventsFragment extends Fragment {

    private RecyclerView recyclerMyEvents;
    private TextView tvNoMyEvents;

    private EventAdapter eventAdapter;
    private final List<Event> myEventList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListenerRegistration registrationsListener;

    // Listeners for the latest details of registered events.
    private final List<ListenerRegistration> eventDetailListeners =
            new ArrayList<>();

    public MyEventsFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_my_events,
                container,
                false
        );

        recyclerMyEvents = view.findViewById(R.id.recyclerMyEvents);
        tvNoMyEvents = view.findViewById(R.id.tvNoMyEvents);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerMyEvents.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        eventAdapter = new EventAdapter(myEventList);
        recyclerMyEvents.setAdapter(eventAdapter);

        loadMyEvents();

        return view;
    }

    private void loadMyEvents() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            showEmptyMessage();
            return;
        }

        String uid = currentUser.getUid();

        registrationsListener = db.collection("registrations")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((snapshot, error) -> {

                    if (!isAdded() || recyclerMyEvents == null) {
                        return;
                    }

                    if (error != null) {
                        myEventList.clear();
                        eventAdapter.notifyDataSetChanged();

                        tvNoMyEvents.setText(
                                "Unable to load your registered events."
                        );

                        showEmptyMessage();
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    // Remove listeners created for the previous snapshot.
                    for (ListenerRegistration listener :
                            eventDetailListeners) {
                        listener.remove();
                    }

                    eventDetailListeners.clear();
                    myEventList.clear();
                    eventAdapter.notifyDataSetChanged();

                    if (snapshot.isEmpty()) {
                        showEmptyMessage();
                        return;
                    }

                    recyclerMyEvents.setVisibility(View.VISIBLE);
                    tvNoMyEvents.setVisibility(View.GONE);

                    // Load the latest event document for each registration.
                    for (DocumentSnapshot registration :
                            snapshot.getDocuments()) {

                        String eventId =
                                registration.getString("eventId");

                        if (eventId == null || eventId.trim().isEmpty()) {
                            continue;
                        }

                        // Keep a copy of registration data as a fallback.
                        String savedTitle =
                                registration.getString("eventTitle");

                        String savedDate =
                                registration.getString("eventDate");

                        String savedTime =
                                registration.getString("eventTime");

                        String savedVenue =
                                registration.getString("eventVenue");

                        String savedCity =
                                registration.getString("eventCity");

                        String savedCategory =
                                registration.getString("category");

                        String savedOrganizer =
                                registration.getString("organizerName");

                        ListenerRegistration eventListener =
                                db.collection("events")
                                        .document(eventId)
                                        .addSnapshotListener(
                                                (eventDocument, eventError) -> {

                                                    if (!isAdded()
                                                            || recyclerMyEvents == null) {
                                                        return;
                                                    }

                                                    Event event = null;

                                                    // Prefer current event details.
                                                    if (eventError == null
                                                            && eventDocument != null
                                                            && eventDocument.exists()) {

                                                        event = eventDocument.toObject(
                                                                Event.class
                                                        );

                                                        if (event != null) {
                                                            event.setEventId(
                                                                    eventDocument.getId()
                                                            );
                                                        }
                                                    }

                                                    // If the event was deleted or
                                                    // cannot be loaded, show saved
                                                    // registration details instead.
                                                    if (event == null) {
                                                        event = new Event();

                                                        event.setEventId(eventId);
                                                        event.setTitle(savedTitle);
                                                        event.setDate(savedDate);
                                                        event.setTime(savedTime);
                                                        event.setVenue(savedVenue);
                                                        event.setCity(savedCity);
                                                        event.setCategory(savedCategory);
                                                        event.setOrganizerName(
                                                                savedOrganizer
                                                        );
                                                    }

                                                    // Replace the card for this event
                                                    // instead of adding duplicates.
                                                    int existingIndex = -1;

                                                    for (int i = 0;
                                                         i < myEventList.size();
                                                         i++) {

                                                        if (eventId.equals(
                                                                myEventList.get(i)
                                                                        .getEventId())) {

                                                            existingIndex = i;
                                                            break;
                                                        }
                                                    }

                                                    if (existingIndex >= 0) {
                                                        myEventList.set(
                                                                existingIndex,
                                                                event
                                                        );
                                                    } else {
                                                        myEventList.add(event);
                                                    }

                                                    eventAdapter.notifyDataSetChanged();

                                                    if (myEventList.isEmpty()) {
                                                        showEmptyMessage();
                                                    } else {
                                                        recyclerMyEvents.setVisibility(
                                                                View.VISIBLE
                                                        );

                                                        tvNoMyEvents.setVisibility(
                                                                View.GONE
                                                        );
                                                    }
                                                });

                        eventDetailListeners.add(eventListener);
                    }
                });
    }

    private void showEmptyMessage() {
        if (recyclerMyEvents != null && tvNoMyEvents != null) {
            recyclerMyEvents.setVisibility(View.GONE);
            tvNoMyEvents.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (registrationsListener != null) {
            registrationsListener.remove();
            registrationsListener = null;
        }

        for (ListenerRegistration listener : eventDetailListeners) {
            listener.remove();
        }

        eventDetailListeners.clear();

        recyclerMyEvents = null;
        tvNoMyEvents = null;
    }
}
