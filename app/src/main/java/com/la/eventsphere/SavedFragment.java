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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SavedFragment extends Fragment {

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

        eventAdapter =
                new EventAdapter(savedEventList);

        recyclerSavedEvents.setAdapter(eventAdapter);

        loadSavedEvents();

        return view;
    }

    private void loadSavedEvents() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser == null) {

            showEmptyMessage();

            return;
        }

        String uid =
                currentUser.getUid();

        savedEventsListener =
                db.collection("users")
                        .document(uid)
                        .collection("savedEvents")
                        .addSnapshotListener((snapshot, error) -> {

                            if (error != null) {

                                showEmptyMessage();

                                return;
                            }

                            savedEventList.clear();

                            if (snapshot != null) {

                                for (com.google.firebase.firestore.DocumentSnapshot document
                                        : snapshot.getDocuments()) {

                                    Event event =
                                            new Event();

                                    event.setEventId(
                                            document.getId()
                                    );

                                    event.setTitle(
                                            document.getString("title")
                                    );

                                    event.setCategory(
                                            document.getString("category")
                                    );

                                    event.setDate(
                                            document.getString("date")
                                    );

                                    event.setTime(
                                            document.getString("time")
                                    );

                                    event.setVenue(
                                            document.getString("venue")
                                    );

                                    event.setCity(
                                            document.getString("city")
                                    );

                                    event.setOrganizerName(
                                            document.getString("organizerName")
                                    );

                                    savedEventList.add(event);
                                }
                            }

                            eventAdapter.notifyDataSetChanged();

                            if (savedEventList.isEmpty()) {

                                showEmptyMessage();

                            } else {

                                recyclerSavedEvents
                                        .setVisibility(View.VISIBLE);

                                tvNoSavedEvents
                                        .setVisibility(View.GONE);
                            }
                        });
    }

    private void showEmptyMessage() {

        recyclerSavedEvents.setVisibility(
                View.GONE
        );

        tvNoSavedEvents.setVisibility(
                View.VISIBLE
        );
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        if (savedEventsListener != null) {

            savedEventsListener.remove();

            savedEventsListener = null;
        }
    }
}