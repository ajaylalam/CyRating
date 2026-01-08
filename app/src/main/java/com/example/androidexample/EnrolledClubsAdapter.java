package com.example.androidexample;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EnrolledClubsAdapter extends RecyclerView.Adapter<EnrolledClubsAdapter.ClubViewHolder> {

    public interface OnClubClickListener {
        void onClubClick(ClubItem item);
    }

    private final List<ClubItem> items;
    private final OnClubClickListener listener;

    public EnrolledClubsAdapter(List<ClubItem> items, OnClubClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ClubViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        ClubItem item = items.get(position);
        holder.title.setText(item.name);
        holder.subtitle.setText(item.description);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClubClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ClubViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;

        public ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(android.R.id.text1);
            subtitle = itemView.findViewById(android.R.id.text2);
        }
    }
}
