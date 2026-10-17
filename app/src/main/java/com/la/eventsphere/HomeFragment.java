package com.la.eventsphere;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvGreetingSmall;
    private TextView tvWelcome;
    private TextView tvNoEvents;

    private RecyclerView recyclerEvents;

    private CardView categoryAll;
    private CardView categoryMusic;
    private CardView categorySports;
    private CardView categoryCulture;
    private CardView categoryHackathons;
    private CardView categoryWorkshops;
    private CardView categoryCompetitions;
    private CardView categoryFests;
    private CardView categoryConcerts;
    private CardView categoryCareer;
    private CardView categoryArts;

    private FirebaseFirestore db;
    private ListenerRegistration eventsListener;

    private EventAdapter eventAdapter;

    private final List<Event> allEvents = new ArrayList<>();
    private final List<Event> displayedEvents = new ArrayList<>();

    private String selectedCategory = "All";

    private final int PURPLE = 0xFF5033D8;
    private final int WHITE = 0xFFFFFFFF;
    private final int GREY_TEXT = 0xFF444444;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_home,
                container,
                false
        );

        initializeViews(view);
        setupRecyclerView();
        setupCategoryListeners();
        loadEvents();

        return view;
    }

    private void initializeViews(View view) {

        tvGreetingSmall =
                view.findViewById(R.id.tvGreetingSmall);

        tvWelcome =
                view.findViewById(R.id.tvWelcome);

        tvNoEvents =
                view.findViewById(R.id.tvNoEvents);

        recyclerEvents =
                view.findViewById(R.id.recyclerEvents);

        categoryAll =
                view.findViewById(R.id.categoryAll);

        categoryMusic =
                view.findViewById(R.id.categoryMusic);

        categorySports =
                view.findViewById(R.id.categorySports);

        categoryCulture =
                view.findViewById(R.id.categoryCulture);

        categoryHackathons =
                view.findViewById(R.id.categoryHackathons);

        categoryWorkshops =
                view.findViewById(R.id.categoryWorkshops);

        categoryCompetitions =
                view.findViewById(R.id.categoryCompetitions);

        categoryFests =
                view.findViewById(R.id.categoryFests);

        categoryConcerts =
                view.findViewById(R.id.categoryConcerts);

        categoryCareer =
                view.findViewById(R.id.categoryCareer);

        categoryArts =
                view.findViewById(R.id.categoryArts);
    }

    private void setupRecyclerView() {

        recyclerEvents.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        recyclerEvents.setNestedScrollingEnabled(false);

        eventAdapter =
                new EventAdapter(displayedEvents);

        recyclerEvents.setAdapter(eventAdapter);
    }

    private void setupCategoryListeners() {

        categoryAll.setOnClickListener(
                v -> selectCategory("All")
        );

        categoryMusic.setOnClickListener(
                v -> selectCategory("Music")
        );

        categorySports.setOnClickListener(
                v -> selectCategory("Sports")
        );

        categoryCulture.setOnClickListener(
                v -> selectCategory("Cultural Events")
        );

        categoryHackathons.setOnClickListener(
                v -> selectCategory("Hackathons")
        );

        categoryWorkshops.setOnClickListener(
                v -> selectCategory("Workshops")
        );

        categoryCompetitions.setOnClickListener(
                v -> selectCategory("Competitions")
        );

        categoryFests.setOnClickListener(
                v -> selectCategory("Fests")
        );

        categoryConcerts.setOnClickListener(
                v -> selectCategory("Concerts")
        );

        categoryCareer.setOnClickListener(
                v -> selectCategory("Career & Networking")
        );

        categoryArts.setOnClickListener(
                v -> selectCategory("Arts & Creativity")
        );
    }

    private void selectCategory(String category) {

        selectedCategory = category;

        updateCategoryAppearance();

        filterEvents();
    }

    private void updateCategoryAppearance() {

        CardView[] cards = {
                categoryAll,
                categoryMusic,
                categorySports,
                categoryCulture,
                categoryHackathons,
                categoryWorkshops,
                categoryCompetitions,
                categoryFests,
                categoryConcerts,
                categoryCareer,
                categoryArts
        };

        for (CardView card : cards) {

            card.setCardBackgroundColor(WHITE);

            if (card.getChildCount() > 0) {

                View child = card.getChildAt(0);

                if (child instanceof TextView) {
                    ((TextView) child)
                            .setTextColor(GREY_TEXT);
                }
            }
        }

        CardView selectedCard =
                getSelectedCategoryCard();

        if (selectedCard != null) {

            selectedCard.setCardBackgroundColor(PURPLE);

            if (selectedCard.getChildCount() > 0) {

                View child =
                        selectedCard.getChildAt(0);

                if (child instanceof TextView) {
                    ((TextView) child)
                            .setTextColor(WHITE);
                }
            }
        }
    }

    private CardView getSelectedCategoryCard() {

        switch (selectedCategory) {

            case "Music":
                return categoryMusic;

            case "Sports":
                return categorySports;

            case "Cultural Events":
                return categoryCulture;

            case "Hackathons":
                return categoryHackathons;

            case "Workshops":
                return categoryWorkshops;

            case "Competitions":
                return categoryCompetitions;

            case "Fests":
                return categoryFests;

            case "Concerts":
                return categoryConcerts;

            case "Career & Networking":
                return categoryCareer;

            case "Arts & Creativity":
                return categoryArts;

            default:
                return categoryAll;
        }
    }

    private void loadEvents() {

        db = FirebaseFirestore.getInstance();

        /*
         * Real-time listener.
         *
         * Whenever an organizer adds, edits or removes
         * an event in Firestore, this listener receives
         * the updated data.
         */
        eventsListener = db.collection("events")
                .addSnapshotListener((snapshots, error) -> {

                    if (!isAdded()) {
                        return;
                    }

                    if (error != null) {

                        recyclerEvents.setVisibility(
                                View.GONE
                        );

                        tvNoEvents.setVisibility(
                                View.VISIBLE
                        );

                        tvNoEvents.setText(
                                "Unable to load events right now."
                        );

                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    allEvents.clear();

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        try {

                            Event event =
                                    document.toObject(Event.class);

                            if (event == null) {
                                continue;
                            }

                            /*
                             * Firestore document ID is stored
                             * separately as eventId.
                             */
                            event.setEventId(
                                    document.getId()
                            );

                            allEvents.add(event);

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }

                    /*
                     * Try to arrange events by date.
                     *
                     * Since date is currently stored as a String,
                     * this is only a basic text comparison.
                     *
                     * We can improve date sorting later by changing
                     * the Firestore date field to Timestamp.
                     */
                    Collections.sort(
                            allEvents,
                            new Comparator<Event>() {

                                @Override
                                public int compare(
                                        Event first,
                                        Event second) {

                                    if (first.getDate() == null) {
                                        return 1;
                                    }

                                    if (second.getDate() == null) {
                                        return -1;
                                    }

                                    return first.getDate()
                                            .compareToIgnoreCase(
                                                    second.getDate()
                                            );
                                }
                            }
                    );

                    filterEvents();
                });
    }

    private void filterEvents() {

        displayedEvents.clear();

        for (Event event : allEvents) {

            if (selectedCategory.equals("All")) {

                displayedEvents.add(event);

            } else if (
                    event.getCategory() != null
                            && event.getCategory()
                            .equalsIgnoreCase(
                                    selectedCategory
                            )
            ) {

                displayedEvents.add(event);
            }
        }

        eventAdapter.notifyDataSetChanged();

        if (displayedEvents.isEmpty()) {

            recyclerEvents.setVisibility(
                    View.GONE
            );

            tvNoEvents.setVisibility(
                    View.VISIBLE
            );

            if (selectedCategory.equals("All")) {

                tvNoEvents.setText(
                        "No events available right now."
                );

            } else {

                tvNoEvents.setText(
                        "No "
                                + selectedCategory
                                + " events available right now."
                );
            }

        } else {

            recyclerEvents.setVisibility(
                    View.VISIBLE
            );

            tvNoEvents.setVisibility(
                    View.GONE
            );
        }
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        if (eventsListener != null) {

            eventsListener.remove();

            eventsListener = null;
        }
    }
}