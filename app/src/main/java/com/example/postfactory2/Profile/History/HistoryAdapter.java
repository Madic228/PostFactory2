package com.example.postfactory2.Profile.History;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private static final String TAG = "HistoryAdapter";
    private List<Post> posts;

    public HistoryAdapter(List<Post> posts) {
        this.posts = posts;
        Log.d(TAG, "Создан адаптер истории с " + posts.size() + " элементами");
    }
    
    public void updateData(List<Post> newPosts) {
        this.posts = newPosts;
        Log.d(TAG, "Обновлены данные адаптера, элементов: " + newPosts.size());
        notifyDataSetChanged();
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
        Log.d(TAG, "Отображение элемента #" + position + ": " + post.getTitle());
        
        holder.title.setText(post.getTitle());
        
        // Используем content вместо excerpt
        holder.excerpt.setText(post.getContent());
        
        holder.date.setText(post.getDate());
        holder.status.setText(post.getStatus());
        
        // Настройка кнопок в зависимости от статуса публикации
        if ("Опубликовано".equals(post.getStatus()) && post.getSocialNetworkUrl() != null) {
            holder.shareButton.setText("Открыть");
            holder.shareButton.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(post.getSocialNetworkUrl()));
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при открытии ссылки: " + e.getMessage());
                    Toast.makeText(v.getContext(), "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.shareButton.setText("Поделиться");
            holder.shareButton.setOnClickListener(v -> {
                // Здесь будет логика для публикации поста
                Toast.makeText(v.getContext(), "Публикация недоступна", Toast.LENGTH_SHORT).show();
            });
        }
        
        // Обработчик для кнопки доработки
        holder.regenerateButton.setOnClickListener(v -> {
            // Здесь будет логика для доработки поста
            Toast.makeText(v.getContext(), "Функция доработки будет доступна позже", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView title, excerpt, date, status;
        Button shareButton, regenerateButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            excerpt = itemView.findViewById(R.id.tv_excerpt);
            date = itemView.findViewById(R.id.tv_date);
            status = itemView.findViewById(R.id.tv_status);
            shareButton = itemView.findViewById(R.id.btn_share);
            regenerateButton = itemView.findViewById(R.id.btn_regenerate);
        }
    }
}
