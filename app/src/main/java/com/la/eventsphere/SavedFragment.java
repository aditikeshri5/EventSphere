package com.la.eventsphere;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SavedFragment extends Fragment {

    private static final String TAG = "SavedFragment";

    private RecyclerView recyclerSavedEvents;
    private TextView tvNoSavedEvents;

    private EventAdapter eventAdapter;
    private final List<Event> savedEventList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListenerRegistration savedEventsListener;

    public SavedFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_saved,
                container,
                false
        );

        recyclerSavedEvents =
                view.findViewById(R.id.recyclerSavedEvents);

        tvNoSavedEvents =
                view.findViewById(R.id.tvNoSavedEvents);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerSavedEvents.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        eventAdapter = new EventAdapter(savedEventList);
        recyclerSavedEvents.setAdapter(eventAdapter);

        loadSavedEvents();

        return view;
    }

    private void loadSavedEvents() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            savedEventList.clear();
            eventAdapter.notifyDataSetChanged();
            showEmptyMessage();
            return;
        }

        String uid = currentUser.getUid();

        if (savedEventsListener != null) {
            savedEventsListener.remove();
        }

        savedEventsListener = db.collection("users")
                .document(uid)
                .collection("savedEvents")
                .addSnapshotListener((snapshot, error) -> {

                    if (!isAdded() || eventAdapter == null) {
                        return;
                    }

                    if (error != null || snapshot == null) {
                        Log.e(TAG, "Failed to load saved events", error);
                        showEmptyMessage();
                        return;
                    }

                    savedEventList.clear();

                    if (snapshot.isEmpty()) {
                        eventAdapter.notifyDataSetChanged();
                        showEmptyMessage();
                        return;
                    }

                    // Display saved copies temporarily while fetching
                    // the latest data from the main events collection.
                    for (DocumentSnapshot savedDoc : snapshot.getDocuments()) {
                        Event savedEvent =
                                createEventFromDocument(savedDoc);

                        savedEventList.add(savedEvent);
                    }

                    eventAdapter.notifyDataSetChanged();
                    showSavedEvents();

                    // Fetch the latest event document for each saved event.
                    for (DocumentSnapshot savedDoc : snapshot.getDocuments()) {

                        String eventId = savedDoc.getId();

                        db.collection("events")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {

                                    if (!isAdded()
                                            || eventAdapter == null
                                            || !eventDoc.exists()) {
                                        return;
                                    }

                                    // Build the event using the main
                                    // event document, including its count.
                                    Event latestEvent =
                                            createEventFromDocument(eventDoc);

                                    latestEvent.setEventId(eventId);

                                    // Replace the matching saved card.
                                    for (int i = 0;
                                         i < savedEventList.size();
                                         i++) {

                                        if (eventId.equals(
                                                savedEventList.get(i)
                                                        .getEventId())) {

                                            savedEventList.set(i, latestEvent);
                                            break;
                                        }
                                    }

                                    eventAdapter.notifyDataSetChanged();

                                    // Sync updated details into savedEvents.
                                    syncSavedEvent(
                                            uid,
                                            eventId,
                                            savedDoc,
                                            eventDoc
                                    );
                                })
                                .addOnFailureListener(refresherror ->
                                        Log.e(
                                                TAG,
                                                "Failed to refresh event: "
                                                        + eventId,
                                                refresherror
                                        ));
                    }
                });
    }

    private Event createEventFromDocument(
            DocumentSnapshot document) {

        Event event = new Event();

        event.setEventId(document.getId());
        event.setTitle(document.getString("title"));
        event.setCategory(document.getString("category"));
        event.setDate(document.getString("date"));
        event.setTime(document.getString("time"));
        event.setVenue(document.getString("venue"));
        event.setCity(document.getString("city"));
        event.setDescription(document.getString("description"));
        event.setImageURL(document.getString("imageURL"));
        event.setOrganizerID(document.getString("organizerID"));
        event.setOrganizerName(document.getString("organizerName"));

        Boolean registrationRequired =
                document.getBoolean("registrationRequired");

        event.setRegistrationRequired(
                registrationRequired != null && registrationRequired
        );

        Long registeredCount = document.getLong("registeredCount");

        event.setRegisteredCount(
                registeredCount != null
                        ? Math.max(0, registeredCount.intValue())
                        : 0
        );

        return event;
    }

    private void syncSavedEvent(
            String uid,
            String eventId,
            DocumentSnapshot savedDoc,
            DocumentSnapshot eventDoc) {

        String[] fields = {
                "title",
                "category",
                "date",
                "time",
                "venue",
                "city",
                "description",
                "imageURL",
                "organizerID",
                "organizerName",
                "registrationRequired",
                "registeredCount"
        };

        Map<String, Object> updates = new HashMap<>();

        for (String field : fields) {

            Object latestValue = eventDoc.get(field);
            Object savedValue = savedDoc.get(field);

            // Sync the latest count even if it is zero.
            if (eventDoc.contains(field)
                    && !Objects.equals(latestValue, savedValue)) {

                updates.put(field, latestValue);
            }
        }

        if (updates.isEmpty()) {
            return;
        }

        updates.put("syncedAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .collection("savedEvents")
                .document(eventId)
                .update(updates)
                .addOnFailureListener(error ->
                        Log.e(
                                TAG,
                                "Could not sync saved event " + eventId,
                                error
                        ));
    }

    private void showSavedEvents() {
        if (recyclerSavedEvents != null && tvNoSavedEvents != null) {
            recyclerSavedEvents.setVisibility(View.VISIBLE);
            tvNoSavedEvents.setVisibility(View.GONE);
        }
    }

    private void showEmptyMessage() {
        if (recyclerSavedEvents != null && tvNoSavedEvents != null) {
            recyclerSavedEvents.setVisibility(View.GONE);
            tvNoSavedEvents.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        if (savedEventsListener != null) {
            savedEventsListener.remove();
            savedEventsListener = null;
        }

        recyclerSavedEvents = null;
        tvNoSavedEvents = null;
        eventAdapter = null;

        super.onDestroyView();
    }
}
