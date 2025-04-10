package com.example.postfactory2.Profile.History;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.postfactory2.R;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {
    private static final String TAG = "HistoryFragment";

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        recyclerView = view.findViewById(R.id.rv_history);
        progressBar = view.findViewById(R.id.progress_bar);
        
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView не найден в макете");
            Toast.makeText(requireContext(), "Ошибка инициализации интерфейса", Toast.LENGTH_SHORT).show();
            return view;
        }
        
        if (progressBar == null) {
            Log.e(TAG, "ProgressBar не найден в макете");
        }
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Инициализация адаптера с пустым списком и FragmentManager
        adapter = new HistoryAdapter(new ArrayList<>(), getParentFragmentManager());
        recyclerView.setAdapter(adapter);
        
        Log.d(TAG, "Фрагмент истории создан, начинаем загрузку данных");
        
        // Загрузка данных
        loadHistoryData();

        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Обновляем данные при возвращении к фрагменту
        loadHistoryData();
    }
    
    private void loadHistoryData() {
        Log.d(TAG, "Загрузка данных истории");
        
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        HistoryApi.getUserGenerations(requireContext(), new HistoryApi.HistoryCallback() {
            @Override
            public void onSuccess(List<Post> posts) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                
                Log.d(TAG, "Успешно получены данные истории: " + posts.size() + " записей");
                
                if (posts.isEmpty()) {
                    Toast.makeText(requireContext(), "История пуста", Toast.LENGTH_SHORT).show();
                }
                
                adapter.updateData(posts);
            }

            @Override
            public void onError(String errorMessage) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                
                Log.e(TAG, "Ошибка при загрузке истории: " + errorMessage);
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                
                // Если произошла ошибка, загружаем шаблонные данные для тестирования
                List<Post> dummyData = getDummyData();
                adapter.updateData(dummyData);
            }
        });
    }
    
    // Временный метод для тестирования интерфейса
    private List<Post> getDummyData() {
        Log.d(TAG, "Загрузка тестовых данных");
        
        List<Post> posts = new ArrayList<>();
        posts.add(new Post("Хуснуллин рассказал о 50 мерах поддержки стройотрасли", "Российское правительство рассматривает около 50 мер поддержки строительной отрасли и застройщиков. Среди таких мер — упрощенная выдача градостроительной документации, сокращение инвестиционного цикла строительства, сокращение инвестцикла строительства и принятие закона о потребительском терроризме. После окончания массовой льготной ипотеки на рынке новостроек наблюдается снижение продаж.", "13 декабря 2024", "Не опубликовано"));
        posts.add(new Post("Строительные организации дали оценку экономической ситуации в 2024 году", "В четвертом квартале 2024 года 66% руководителей строительных компаний в России считают экономическую ситуацию в России удовлетворительной. По данным Росстата, средняя обеспеченность заказами компаний строительной отрасли составила шесть месяцев, а средний уровень обеспеченности финансированием составил пять месяцев. В первом квартале 2025 года увеличения прибыли ожидают 18% компаний, 14% — 13% субъектов малого предпринимательства.", "2 января 2024", "Опубликовано"));
        return posts;
    }
}
