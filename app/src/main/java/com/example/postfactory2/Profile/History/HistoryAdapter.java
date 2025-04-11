package com.example.postfactory2.Profile.History;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.Generate.ResultFragment;
import com.example.postfactory2.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    private static final String TAG = "HistoryAdapter";
    private List<Post> posts;
    private FragmentManager fragmentManager;

    public HistoryAdapter(List<Post> posts, FragmentManager fragmentManager) {
        this.posts = posts;
        this.fragmentManager = fragmentManager;
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
        holder.excerpt.setText(post.getContent());
        holder.date.setText(post.getDate());
        holder.status.setText(post.getStatus());
        
        // Настройка кнопки "Поделиться"
        holder.shareButton.setOnClickListener(v -> {
            String shareText = post.getContent();
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            
            Intent chooserIntent = Intent.createChooser(shareIntent, "Поделиться постом");
            v.getContext().startActivity(chooserIntent);
        });
        
        // Настройка кнопки "Доработка"
        holder.regenerateButton.setOnClickListener(v -> {
            // Создаем ResultFragment с готовым текстом
            ResultFragment resultFragment = new ResultFragment();
            Bundle args = new Bundle();
            
            // Передаем данные поста
            args.putString("post_theme", post.getTitle());
            args.putString("summarized_text", post.getContent());
            args.putBoolean("from_history", true); // Флаг, что открыто из истории
            
            resultFragment.setArguments(args);
            
            // Открываем ResultFragment
            FragmentManager fragmentManager = ((FragmentActivity) v.getContext()).getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, resultFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // Настройка кнопки "Удалить"
        holder.deleteButton.setOnClickListener(v -> {
            // Показываем диалог подтверждения
            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Удаление поста")
                    .setMessage("Вы уверены, что хотите удалить этот пост?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        // Удаляем пост
                        HistoryApi.deleteGeneration(v.getContext(), post.getId(), new HistoryApi.DeleteCallback() {
                            @Override
                            public void onSuccess() {
                                // Удаляем пост из списка
                                posts.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, posts.size());
                                Toast.makeText(v.getContext(), "Пост успешно удален", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(v.getContext(), errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView title, excerpt, date, status;
        MaterialButton shareButton, regenerateButton, deleteButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_title);
            excerpt = itemView.findViewById(R.id.tv_excerpt);
            date = itemView.findViewById(R.id.tv_date);
            status = itemView.findViewById(R.id.tv_status);
            shareButton = itemView.findViewById(R.id.btn_share);
            regenerateButton = itemView.findViewById(R.id.btn_regenerate);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}
