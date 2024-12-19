package com.example.postfactory2.Profile.History;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<Post> posts;

    public HistoryAdapter(List<Post> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_card, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.title.setText(post.getTitle());
        holder.excerpt.setText(post.getExcerpt());
        holder.date.setText(post.getDate());
        holder.status.setText(post.getStatus());
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView title, excerpt, date, status;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            excerpt = itemView.findViewById(R.id.tv_excerpt);
            date = itemView.findViewById(R.id.tv_date);
            status = itemView.findViewById(R.id.tv_status);
        }
    }
}
