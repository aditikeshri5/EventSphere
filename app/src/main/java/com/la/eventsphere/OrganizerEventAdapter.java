package com.la.eventsphere;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class OrganizerEventAdapter
        extends RecyclerView.Adapter<OrganizerEventAdapter.EventViewHolder> {

    private final List<Event> eventList;
    private final FirebaseFirestore db;
    private final FirebaseAuth mAuth;

    public OrganizerEventAdapter(List<Event> eventList) {
        this.eventList = eventList;
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.organizer_event_item, parent, false);

        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull EventViewHolder holder, int position) {

        Event event = eventList.get(position);

        // Event category
        holder.tvCategory.setText(
                event.getCategory() != null
                        ? event.getCategory()
                        : "");

        // Event title
        holder.tvTitle.setText(
                event.getTitle() != null
                        ? event.getTitle()
                        : "Untitled Event");

        // Event date and time
        String date = event.getDate() != null
                ? event.getDate()
                : "Date unavailable";

        String time = event.getTime() != null
                ? event.getTime()
                : "";

        holder.tvDate.setText(
                time.isEmpty()
                        ? date
                        : date + " • " + time);

        // Event venue and city
        String venue = event.getVenue() != null
                ? event.getVenue()
                : "";

        String city = event.getCity() != null
                ? event.getCity()
                : "";

        holder.tvVenue.setText(
                city.isEmpty()
                        ? venue
                        : (venue.isEmpty()
                           ? city
                           : venue + ", " + city));

        // Registration count
        int count = event.getRegisteredCount();

        holder.tvRegistrationCount.setText(
                count + (count == 1
                        ? " Registration"
                        : " Registrations"));

        // Edit event
        holder.btnEdit.setOnClickListener(v -> {

            String eventId = event.getEventId();

            if (eventId == null || eventId.trim().isEmpty()) {
                Toast.makeText(
                        v.getContext(),
                        "Event ID is missing.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            Intent intent = new Intent(
                    v.getContext(),
                    EditEventActivity.class
            );

            intent.putExtra("eventId", eventId);
            v.getContext().startActivity(intent);
        });

        // Delete event
        holder.btnDelete.setOnClickListener(v -> {

            String eventId = event.getEventId();

            if (eventId == null || eventId.trim().isEmpty()) {
                Toast.makeText(
                        v.getContext(),
                        "Event ID is missing.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Event")
                    .setMessage(
                            "Are you sure you want to delete \""
                                    + event.getTitle() + "\"?"
                    )
                    .setNegativeButton(
                            "Cancel",
                            (dialog, which) -> dialog.dismiss()
                    )
                    .setPositiveButton(
                            "Delete",
                            (dialog, which) ->
                                    deleteEvent(eventId, holder)
                    )
                    .show();
        });
    }

    private void deleteEvent(
            String eventId,
            EventViewHolder holder) {

        String uid = mAuth.getUid();

        if (uid == null) {
            Toast.makeText(
                    holder.itemView.getContext(),
                    "Please log in again.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(
                                holder.itemView.getContext(),
                                "This event no longer exists.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    // Verify event ownership.
                    // Firestore field: organizerID
                    String organizerId =
                            document.getString("organizerID");

                    if (organizerId == null
                            || !organizerId.equals(uid)) {

                        Toast.makeText(
                                holder.itemView.getContext(),
                                "You cannot delete this event.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    // Delete event from Firestore
                    db.collection("events")
                            .document(eventId)
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        holder.itemView.getContext(),
                                        "Event deleted successfully.",
                                        Toast.LENGTH_SHORT
                                ).show();

                            })
                            .addOnFailureListener(e -> {

                                Toast.makeText(
                                        holder.itemView.getContext(),
                                        "Unable to delete event: "
                                                + e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();

                            });
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            holder.itemView.getContext(),
                            "Unable to verify event ownership: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvCategory;
        TextView tvTitle;
        TextView tvDate;
        TextView tvVenue;
        TextView tvRegistrationCount;

        Button btnEdit;
        Button btnDelete;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);

            tvCategory = itemView.findViewById(
                    R.id.tvOrganizerEventCategory);

            tvTitle = itemView.findViewById(
                    R.id.tvOrganizerEventTitle);

            tvDate = itemView.findViewById(
                    R.id.tvOrganizerEventDate);

            tvVenue = itemView.findViewById(
                    R.id.tvOrganizerEventVenue);

            tvRegistrationCount = itemView.findViewById(
                    R.id.tvOrganizerRegistrationCount);

            btnEdit = itemView.findViewById(
                    R.id.btnEditOrganizerEvent);

            btnDelete = itemView.findViewById(
                    R.id.btnDeleteOrganizerEvent);
        }
    }
}
