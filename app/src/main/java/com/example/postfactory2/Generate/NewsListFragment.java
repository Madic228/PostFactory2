package com.example.postfactory2.Generate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NewsListFragment extends Fragment {

    private RecyclerView rvNewsList;
    private static final String TAG = "NewsListFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_news_list, container, false);
        rvNewsList = view.findViewById(R.id.rvNewsList);

        rvNewsList.setLayoutManager(new LinearLayoutManager(getContext()));

        if (getArguments() != null) {
            String response = getArguments().getString("response");
            if (response != null) {
                parseResponse(response);
            } else {
                Toast.makeText(getContext(), "Нет данных для отображения", Toast.LENGTH_SHORT).show();
            }
        }

        // Инициализация Toolbar и кнопки "Назад"
        // Инициализация Toolbar и кнопки "Назад"
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar); // Исправление типа
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);

        // Обработка клика на кнопку "Назад"
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void parseResponse(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            List<NewsItem> newsItems = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                NewsItem newsItem = new NewsItem(
                        item.getString("title"),
                        item.getString("publication_date"),
                        item.getString("source"),
                        item.getString("link"),
                        item.getString("summarized_text")
                );
                newsItems.add(newsItem);
            }

            NewsAdapter adapter = new NewsAdapter(newsItems, this::navigateToResultFragment);
            rvNewsList.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void navigateToResultFragment(NewsItem newsItem) {
        ResultFragment resultFragment = new ResultFragment();
        Bundle args = new Bundle();

        // Передаем данные новости в ResultFragment
        args.putString("post_theme", newsItem.getTitle());
        args.putString("publication_date", newsItem.getPublicationDate());
        args.putString("source", newsItem.getSource());
        args.putString("link", newsItem.getLink());
        args.putString("summarized_text", newsItem.getSummarizedText()); // Если требуется, можно передать суммаризированный текст

        resultFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, resultFragment)
                .addToBackStack(null)
                .commit();
    }

}
