package com.example.postfactory2.Home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.Auth.TokenManager;
import com.example.postfactory2.Profile.History.HistoryApi;
import com.example.postfactory2.Profile.History.HistoryFragment;
import com.example.postfactory2.Profile.History.Post;
import com.example.postfactory2.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements PostAdapter.PostClickListener {

    private RecyclerView rvPosts;
    private PostAdapter postAdapter;
    private TextView tvGreeting;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Инициализация RecyclerView
        rvPosts = view.findViewById(R.id.rvPosts);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        // Создаем адаптер с пустым списком
        postAdapter = new PostAdapter(new ArrayList<>(), this);
        rvPosts.setAdapter(postAdapter);

        // Устанавливаем приветствие с именем пользователя
        updateGreeting();

        // Загружаем последние посты
        loadRecentPosts();

        return view;
    }

    private void updateGreeting() {
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        String username = tokenManager.getUsername();
        if (!username.isEmpty()) {
            tvGreeting.setText(String.format("Здравствуйте, %s!", username));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем приветствие при возвращении на экран
        updateGreeting();
    }

    private void loadRecentPosts() {
        // Используем существующий API для загрузки истории
        HistoryApi.getUserGenerations(requireContext(), new HistoryApi.HistoryCallback() {
            @Override
            public void onSuccess(List<Post> posts) {
                // Берем только последние 5 постов
                List<Post> recentPosts = posts.subList(0, Math.min(posts.size(), 5));
                postAdapter = new PostAdapter(recentPosts, HomeFragment.this);
                rvPosts.setAdapter(postAdapter);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPostClick(Post post) {
        // Переходим на экран истории
        HistoryFragment historyFragment = new HistoryFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, historyFragment)
                .addToBackStack(null)
                .commit();
    }
}
