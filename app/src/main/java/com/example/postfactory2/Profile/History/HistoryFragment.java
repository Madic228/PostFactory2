package com.example.postfactory2.Profile.History;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.postfactory2.R;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.rv_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация адаптера с шаблонными данными
        adapter = new HistoryAdapter(getDummyData());
        recyclerView.setAdapter(adapter);

        return view;
    }

    private List<Post> getDummyData() {
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("План на день", "Приходу с пар в 12, если себя чувствую хорошо...", "13 декабря 2024", "Не опубликовано"));
        posts.add(new Post("Покупки", "Сметана большая Башкирская...", "2 января 2024", "Опубликовано"));
        return posts;
    }
}
