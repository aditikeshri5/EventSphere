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
import com.google.firebase.firestore.DocumentSnapshot;

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


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MyEventsFragment() {
        // Required empty constructor
    }


    // =========================================================
    // CREATE VIEW
    // =========================================================

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


        // -----------------------------
        // INITIALIZE VIEWS
        // -----------------------------

        recyclerMyEvents =
                view.findViewById(R.id.recyclerMyEvents);

        tvNoMyEvents =
                view.findViewById(R.id.tvNoMyEvents);


        // -----------------------------
        // FIREBASE
        // -----------------------------

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // -----------------------------
        // RECYCLER VIEW
        // -----------------------------

        recyclerMyEvents.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        eventAdapter = new EventAdapter(myEventList);

        recyclerMyEvents.setAdapter(eventAdapter);


        // -----------------------------
        // LOAD REGISTERED EVENTS
        // -----------------------------

        loadMyEvents();


        return view;
    }


    // =========================================================
    // LOAD MY REGISTERED EVENTS
    // =========================================================

    private void loadMyEvents() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        // -----------------------------
        // USER NOT LOGGED IN
        // -----------------------------

        if (currentUser == null) {

            showEmptyMessage();

            return;
        }


        String uid = currentUser.getUid();


        /*
         * Our registrations collection is:
         *
         * registrations
         *      └── eventId_userId
         *
         * Each registration contains:
         *
         * eventId
         * userId
         * userName
         * userEmail
         * eventTitle
         * eventDate
         * eventVenue
         * status
         * registeredAt
         */


        registrationsListener =
                db.collection("registrations")
                        .whereEqualTo("userId", uid)
                        .addSnapshotListener(
                                (snapshot, error) -> {


                                    // -----------------------------
                                    // FIRESTORE ERROR
                                    // -----------------------------

                                    if (error != null) {

                                        showEmptyMessage();

                                        return;
                                    }


                                    // -----------------------------
                                    // CLEAR OLD DATA
                                    // -----------------------------

                                    myEventList.clear();


                                    // -----------------------------
                                    // READ REGISTRATIONS
                                    // -----------------------------

                                    if (snapshot != null) {

                                        for (
                                                DocumentSnapshot document
                                                : snapshot.getDocuments()
                                        ) {

                                            String eventId =
                                                    document.getString(
                                                            "eventId"
                                                    );

                                            String eventTitle =
                                                    document.getString(
                                                            "eventTitle"
                                                    );

                                            String eventDate =
                                                    document.getString(
                                                            "eventDate"
                                                    );

                                            String eventVenue =
                                                    document.getString(
                                                            "eventVenue"
                                                    );


                                            /*
                                             * Create Event object
                                             * using the information
                                             * stored in registration.
                                             */

                                            Event event =
                                                    new Event();

                                            event.setEventId(
                                                    eventId
                                            );

                                            event.setTitle(
                                                    eventTitle
                                            );

                                            event.setDate(
                                                    eventDate
                                            );

                                            event.setVenue(
                                                    eventVenue
                                            );


                                            /*
                                             * These fields are not
                                             * currently stored in the
                                             * registration document.
                                             *
                                             * They will remain empty
                                             * for now.
                                             */

                                            event.setCategory(
                                                    document.getString(
                                                            "category"
                                                    )
                                            );

                                            event.setTime(
                                                    document.getString(
                                                            "eventTime"
                                                    )
                                            );

                                            event.setCity(
                                                    document.getString(
                                                            "eventCity"
                                                    )
                                            );

                                            event.setOrganizerName(
                                                    document.getString(
                                                            "organizerName"
                                                    )
                                            );


                                            myEventList.add(event);
                                        }
                                    }


                                    // -----------------------------
                                    // UPDATE RECYCLER VIEW
                                    // -----------------------------

                                    eventAdapter.notifyDataSetChanged();


                                    // -----------------------------
                                    // EMPTY / NON-EMPTY STATE
                                    // -----------------------------

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
                                }
                        );
    }


    // =========================================================
    // SHOW EMPTY MESSAGE
    // =========================================================

    private void showEmptyMessage() {

        recyclerMyEvents.setVisibility(
                View.GONE
        );

        tvNoMyEvents.setVisibility(
                View.VISIBLE
        );
    }


    // =========================================================
    // REMOVE FIRESTORE LISTENER
    // =========================================================

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        if (registrationsListener != null) {

            registrationsListener.remove();

            registrationsListener = null;
        }
    }
}