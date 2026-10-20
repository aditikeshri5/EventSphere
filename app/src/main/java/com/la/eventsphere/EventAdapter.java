
package com.la.eventsphere;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EventAdapter
        extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    private static final int PURPLE = Color.rgb(80, 51, 216);
    private static final int GREY = Color.rgb(158, 158, 158);

    public EventAdapter(List<Event> eventList) {
        this.eventList = eventList;
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_item, parent, false);

        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull EventViewHolder holder,
            int position) {

        Event event = eventList.get(position);

        // Display event details
        holder.tvEventCategory.setText(
                nonEmpty(event.getCategory(), "Event"));

        holder.tvEventTitle.setText(
                nonEmpty(event.getTitle(), "Untitled event"));

        holder.tvEventDate.setText(
                "📅  " + nonEmpty(
                        event.getDate(),
                        "Date to be announced"));

        holder.tvEventTime.setText(
                "🕐  " + nonEmpty(
                        event.getTime(),
                        "Time to be announced"));

        holder.tvEventVenue.setText(
                "📍  " + nonEmpty(
                        event.getVenue(),
                        "Venue to be announced"));

        holder.tvEventCity.setText(
                nonEmpty(event.getCity(), ""));

        // Display registration count
        int registrationCount = Math.max(
                0, event.getRegisteredCount());

        holder.tvEventRegistrationCount.setText(
                registrationCount
                        + (registrationCount == 1
                        ? " Registration"
                        : " Registrations")
        );

        // Display organizer information
        String organizer = nonEmpty(
                event.getOrganizerName(),
                "Event Organizer");

        holder.tvOrganizerName.setText("By " + organizer);

        holder.tvOrganizerInitial.setText(
                organizer.substring(0, 1)
                        .toUpperCase(Locale.getDefault()));

        // Open event details
        holder.itemView.setOnClickListener(v -> {

            String eventId = event.getEventId();

            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(
                        v.getContext(),
                        "Unable to open event details.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(
                    v.getContext(),
                    EventDetailsActivity.class);

            intent.putExtra("eventId", eventId);
            v.getContext().startActivity(intent);
        });

        // Display saved or unsaved star
        checkSavedStatus(holder, event);

        // Save or unsave event
        holder.ivSave.setOnClickListener(v -> {

            FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser == null) {
                Toast.makeText(
                        v.getContext(),
                        "Please login to save events.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String eventId = event.getEventId();

            if (eventId == null || eventId.isEmpty()) {
                Toast.makeText(
                        v.getContext(),
                        "Unable to save this event.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String uid = currentUser.getUid();

            holder.ivSave.setEnabled(false);

            db.collection("users")
                    .document(uid)
                    .collection("savedEvents")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(snapshot -> {

                        if (snapshot.exists()) {

                            // UNSAVE EVENT
                            db.collection("users")
                                    .document(uid)
                                    .collection("savedEvents")
                                    .document(eventId)
                                    .delete()
                                    .addOnSuccessListener(unused -> {

                                        setStar(holder.ivSave, false);

                                        Toast.makeText(
                                                v.getContext(),
                                                "Event removed from saved.",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    v.getContext(),
                                                    "Unable to remove saved event.",
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                    )
                                    .addOnCompleteListener(task ->
                                            holder.ivSave.setEnabled(true));

                        } else {

                            // SAVE EVENT
                            Map<String, Object> savedEvent =
                                    new HashMap<>();

                            savedEvent.put("eventId", eventId);
                            savedEvent.put("title", event.getTitle());
                            savedEvent.put("category", event.getCategory());
                            savedEvent.put("date", event.getDate());
                            savedEvent.put("time", event.getTime());
                            savedEvent.put("venue", event.getVenue());
                            savedEvent.put("city", event.getCity());
                            savedEvent.put(
                                    "organizerName",
                                    event.getOrganizerName());

                            db.collection("users")
                                    .document(uid)
                                    .collection("savedEvents")
                                    .document(eventId)
                                    .set(savedEvent)
                                    .addOnSuccessListener(unused -> {

                                        setStar(holder.ivSave, true);

                                        Toast.makeText(
                                                v.getContext(),
                                                "Event saved!",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    v.getContext(),
                                                    "Unable to save event.",
                                                    Toast.LENGTH_SHORT
                                            ).show()
                                    )
                                    .addOnCompleteListener(task ->
                                            holder.ivSave.setEnabled(true));
                        }
                    })
                    .addOnFailureListener(e -> {

                        holder.ivSave.setEnabled(true);

                        Toast.makeText(
                                v.getContext(),
                                "Unable to check saved event.",
                                Toast.LENGTH_SHORT
                        ).show();
                    });
        });
    }

    // Change the star appearance
    private void setStar(
            ImageView imageView,
            boolean isSaved) {

        imageView.setImageResource(
                isSaved
                        ? android.R.drawable.btn_star_big_on
                        : android.R.drawable.btn_star_big_off
        );

        imageView.setColorFilter(isSaved ? PURPLE : GREY);

        imageView.setContentDescription(
                isSaved ? "Unsave event" : "Save event");
    }

    // Read saved state from Firestore
    private void checkSavedStatus(
            EventViewHolder holder,
            Event event) {

        // Reset recycled cards
        setStar(holder.ivSave, false);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        String eventId = event.getEventId();

        if (currentUser == null
                || eventId == null
                || eventId.isEmpty()) {
            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("savedEvents")
                .document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    int currentPosition =
                            holder.getBindingAdapterPosition();

                    if (currentPosition == RecyclerView.NO_POSITION
                            || currentPosition >= eventList.size()) {
                        return;
                    }

                    Event currentEvent =
                            eventList.get(currentPosition);

                    // Ensure this holder still represents the same event
                    if (!eventId.equals(currentEvent.getEventId())) {
                        return;
                    }

                    setStar(holder.ivSave, snapshot.exists());
                });
    }

    private String nonEmpty(
            String value,
            String fallback) {

        return value == null || value.trim().isEmpty()
                ? fallback
                : value.trim();
    }

    @Override
    public int getItemCount() {
        return eventList == null ? 0 : eventList.size();
    }

    public static class EventViewHolder
            extends RecyclerView.ViewHolder {

        ImageView ivSave;

        TextView tvEventCategory;
        TextView tvEventTitle;
        TextView tvEventDate;
        TextView tvEventTime;
        TextView tvEventVenue;
        TextView tvEventCity;
        TextView tvEventRegistrationCount;
        TextView tvOrganizerName;
        TextView tvOrganizerInitial;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            ivSave = itemView.findViewById(R.id.ivSave);

            tvEventCategory =
                    itemView.findViewById(R.id.tvEventCategory);

            tvEventTitle =
                    itemView.findViewById(R.id.tvEventTitle);

            tvEventDate =
                    itemView.findViewById(R.id.tvEventDate);

            tvEventTime =
                    itemView.findViewById(R.id.tvEventTime);

            tvEventVenue =
                    itemView.findViewById(R.id.tvEventVenue);

            tvEventCity =
                    itemView.findViewById(R.id.tvEventCity);

            tvEventRegistrationCount =
                    itemView.findViewById(
                            R.id.tvEventRegistrationCount);

            tvOrganizerName =
                    itemView.findViewById(R.id.tvOrganizerName);

            tvOrganizerInitial =
                    itemView.findViewById(R.id.tvOrganizerInitial);
        }
    }
}
