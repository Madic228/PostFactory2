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
        posts.add(new Post("Хуснуллин рассказал о 50 мерах поддержки стройотрасли", "Российское правительство рассматривает около 50 мер поддержки строительной отрасли и застройщиков. Среди таких мер — упрощенная выдача градостроительной документации, сокращение инвестиционного цикла строительства, сокращение инвестцикла строительства и принятие закона о потребительском терроризме. После окончания массовой льготной ипотеки на рынке новостроек наблюдается снижение продаж.", "13 декабря 2024", "Не опубликовано"));
        posts.add(new Post("Строительные организации дали оценку экономической ситуации в 2024 году", "В четвертом квартале 2024 года 66% руководителей строительных компаний в России считают экономическую ситуацию в России удовлетворительной. По данным Росстата, средняя обеспеченность заказами компаний строительной отрасли составила шесть месяцев, а средний уровень обеспеченности финансированием составил пять месяцев. В первом квартале 2025 года увеличения прибыли ожидают 18% компаний, 14% — 13% субъектов малого предпринимательства.", "2 января 2024", "Опубликовано"));
        return posts;
    }
}
