package com.example.postfactory2.Home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvPosts;
    private PostAdapter postAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Найти RecyclerView
        rvPosts = view.findViewById(R.id.rvPosts);
        rvPosts.setLayoutManager(new LinearLayoutManager(getContext()));

        // Заглушка данных
        List<String> postList = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            postList.add("Сгенерированный пост #" + i);
        }

        // Настроить адаптер
        postAdapter = new PostAdapter(postList, this::onPostClicked);
        rvPosts.setAdapter(postAdapter);

        return view;
    }

    // Обработчик клика по посту
    private void onPostClicked(String post) {
        // Здесь логика перехода на фрагмент с историей генераций
        // Отображение уведомления
        Toast.makeText(getContext(), "Вы выбрали пост: " + post, Toast.LENGTH_SHORT).show();

    }
}
