package com.la.eventsphere;

import android.content.Intent;
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
import java.util.Map;

public class EventAdapter
        extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> eventList;

    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

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

        // -----------------------------
        // EVENT INFORMATION
        // -----------------------------

        holder.tvEventCategory.setText(
                event.getCategory() != null
                        ? event.getCategory()
                        : ""
        );

        holder.tvEventTitle.setText(
                event.getTitle() != null
                        ? event.getTitle()
                        : ""
        );

        holder.tvEventDate.setText(
                event.getDate() != null
                        ? "📅 " + event.getDate()
                        : ""
        );

        holder.tvEventTime.setText(
                event.getTime() != null
                        ? "🕐 " + event.getTime()
                        : ""
        );

        holder.tvEventVenue.setText(
                event.getVenue() != null
                        ? "📍 " + event.getVenue()
                        : ""
        );

        holder.tvEventCity.setText(
                event.getCity() != null
                        ? event.getCity()
                        : ""
        );

        holder.tvOrganizerName.setText(
                event.getOrganizerName() != null
                        ? "By " + event.getOrganizerName()
                        : ""
        );


        // -----------------------------
        // OPEN EVENT DETAILS
        // -----------------------------

        holder.itemView.setOnClickListener(v -> {

            Intent intent = new Intent(
                    v.getContext(),
                    EventDetailsActivity.class
            );

            intent.putExtra(
                    "eventId",
                    event.getEventId()
            );

            v.getContext().startActivity(intent);
        });


        // -----------------------------
        // CHECK SAVED STATUS
        // -----------------------------

        checkSavedStatus(holder, event);


        // -----------------------------
        // SAVE / UNSAVE EVENT
        // -----------------------------

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

            String uid = currentUser.getUid();
            String eventId = event.getEventId();

            if (eventId == null || eventId.isEmpty()) {

                Toast.makeText(
                        v.getContext(),
                        "Unable to save this event.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            db.collection("users")
                    .document(uid)
                    .collection("savedEvents")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        if (documentSnapshot.exists()) {

                            // -----------------------------
                            // UNSAVE EVENT
                            // -----------------------------

                            db.collection("users")
                                    .document(uid)
                                    .collection("savedEvents")
                                    .document(eventId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {

                                        holder.ivSave.setImageResource(
                                                android.R.drawable.btn_star_big_off
                                        );

                                        Toast.makeText(
                                                v.getContext(),
                                                "Event removed from saved.",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    })
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(
                                                v.getContext(),
                                                "Unable to remove saved event.",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    });

                        } else {

                            // -----------------------------
                            // SAVE EVENT
                            // -----------------------------

                            Map<String, Object> savedEvent =
                                    new HashMap<>();

                            savedEvent.put(
                                    "eventId",
                                    event.getEventId()
                            );

                            savedEvent.put(
                                    "title",
                                    event.getTitle()
                            );

                            savedEvent.put(
                                    "category",
                                    event.getCategory()
                            );

                            savedEvent.put(
                                    "date",
                                    event.getDate()
                            );

                            savedEvent.put(
                                    "time",
                                    event.getTime()
                            );

                            savedEvent.put(
                                    "venue",
                                    event.getVenue()
                            );

                            savedEvent.put(
                                    "city",
                                    event.getCity()
                            );

                            savedEvent.put(
                                    "organizerName",
                                    event.getOrganizerName()
                            );


                            db.collection("users")
                                    .document(uid)
                                    .collection("savedEvents")
                                    .document(eventId)
                                    .set(savedEvent)
                                    .addOnSuccessListener(aVoid -> {

                                        holder.ivSave.setImageResource(
                                                android.R.drawable.btn_star_big_on
                                        );

                                        Toast.makeText(
                                                v.getContext(),
                                                "Event saved!",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    })
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(
                                                v.getContext(),
                                                "Unable to save event.",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                    });
                        }

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(
                                v.getContext(),
                                "Unable to check saved event.",
                                Toast.LENGTH_SHORT
                        ).show();

                    });
        });
    }


    // =========================================================
    // CHECK WHETHER EVENT IS ALREADY SAVED
    // =========================================================

    private void checkSavedStatus(
            EventViewHolder holder,
            Event event) {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null ||
                event.getEventId() == null ||
                event.getEventId().isEmpty()) {

            holder.ivSave.setImageResource(
                    android.R.drawable.btn_star_big_off
            );

            return;
        }

        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("savedEvents")
                .document(event.getEventId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        holder.ivSave.setImageResource(
                                android.R.drawable.btn_star_big_on
                        );

                    } else {

                        holder.ivSave.setImageResource(
                                android.R.drawable.btn_star_big_off
                        );
                    }

                });
    }


    // =========================================================
    // ITEM COUNT
    // =========================================================

    @Override
    public int getItemCount() {
        return eventList.size();
    }


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    public static class EventViewHolder
            extends RecyclerView.ViewHolder {

        ImageView ivEventImage;
        ImageView ivSave;

        TextView tvEventCategory;
        TextView tvEventTitle;
        TextView tvEventDate;
        TextView tvEventTime;
        TextView tvEventVenue;
        TextView tvEventCity;
        TextView tvOrganizerName;


        public EventViewHolder(@NonNull View itemView) {

            super(itemView);

            ivEventImage =
                    itemView.findViewById(R.id.ivEventImage);

            ivSave =
                    itemView.findViewById(R.id.ivSave);

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

            tvOrganizerName =
                    itemView.findViewById(R.id.tvOrganizerName);
        }
    }
}