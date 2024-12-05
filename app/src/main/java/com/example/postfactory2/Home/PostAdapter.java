package com.example.postfactory2.Home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.R;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<String> postList;
    private final PostClickListener clickListener;

    public PostAdapter(List<String> postList, PostClickListener clickListener) {
        this.postList = postList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        String post = postList.get(position);
        holder.tvPost.setText(post);
        holder.itemView.setOnClickListener(v -> clickListener.onPostClick(post));
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public interface PostClickListener {
        void onPostClick(String post);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvPost;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPost = itemView.findViewById(R.id.tvPost);
        }
    }
}
