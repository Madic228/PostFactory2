package com.example.postfactory2.Generate;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.fragment.app.FragmentManager;
import androidx.activity.OnBackPressedCallback;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class    NewsListFragment extends Fragment {

    private RecyclerView rvNewsList;
    private static final String TAG = "NewsListFragment";
    private RequestQueue requestQueue;
    private List<NewsItem> newsItems;
    private NewsAdapter adapter;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestQueue = Volley.newRequestQueue(requireContext());
        
        // Добавляем обработчик для системных событий возврата
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Проверяем, есть ли фрагменты в стеке
                if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    // Если есть, проверяем предыдущий фрагмент
                    FragmentManager.BackStackEntry backEntry = requireActivity().getSupportFragmentManager()
                            .getBackStackEntryAt(requireActivity().getSupportFragmentManager().getBackStackEntryCount() - 1);
                    String fragmentTag = backEntry.getName();
                    
                    // Если возвращаемся из ResultFragment
                    if (fragmentTag != null && fragmentTag.contains("ResultFragment")) {
                        // Устанавливаем флаг from_result
                        Bundle args = getArguments();
                        if (args != null) {
                            args.putBoolean("from_result", true);
                        }
                    }
                }
                // Продолжаем стандартное поведение
                this.setEnabled(false);
                requireActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Проверяем, не возвращаемся ли мы из ResultFragment
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry backEntry = fragmentManager
                    .getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1);
            String fragmentTag = backEntry.getName();
            
            // Если предыдущий фрагмент был ResultFragment
            if (fragmentTag != null && fragmentTag.contains("ResultFragment")) {
                // Устанавливаем флаг from_result
                Bundle args = getArguments();
                if (args != null) {
                    args.putBoolean("from_result", true);
                }
            }
        }
    }

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
                // Проверяем, не возвращаемся ли мы из ResultFragment
                boolean isReturningFromResult = getArguments().getBoolean("from_result", false);
                if (!isReturningFromResult) {
                    checkAndUpdateSummarizedText();
                }
            } else {
                Toast.makeText(getContext(), "Нет данных для отображения", Toast.LENGTH_SHORT).show();
            }
        }

        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        ImageButton btnBack = toolbar.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void parseResponse(String response) {
        try {
            JSONArray jsonArray = new JSONArray(response);
            newsItems = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                NewsItem newsItem = new NewsItem(
                        item.getString("title"),
                        item.getString("publication_date"),
                        item.getString("source"),
                        item.getString("link"),
                        item.optString("summarized_text", "")
                );
                newsItems.add(newsItem);
            }

            adapter = new NewsAdapter(newsItems, this::navigateToResultFragment);
            rvNewsList.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Идет суммаризация статей...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
        
        // Добавляем таймаут для прогресс-диалога (30 секунд)
        new Handler().postDelayed(() -> {
            hideProgressDialog();
        }, 30000);
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void checkAndUpdateSummarizedText() {
        if (newsItems == null) return;

        // Проверяем, не возвращаемся ли мы из ResultFragment
        boolean isReturningFromResult = getArguments() != null && getArguments().getBoolean("from_result", false);
        
        // Собираем список статей без summarized_text
        List<NewsItem> articlesToUpdate = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        
        for (int i = 0; i < newsItems.size(); i++) {
            NewsItem newsItem = newsItems.get(i);
            // Проверяем, что summarized_text действительно пустой
            if (newsItem.getSummarizedText() == null || 
                newsItem.getSummarizedText().isEmpty() || 
                newsItem.getSummarizedText().equals("Сгенерированный текст")) {
                articlesToUpdate.add(newsItem);
                positions.add(i);
            }
        }

        // Запускаем суммаризацию только если есть статьи для обновления И мы не возвращаемся из ResultFragment
        if (!articlesToUpdate.isEmpty() && !isReturningFromResult) {
            showProgressDialog();
            Toast.makeText(requireContext(), "Проверка доступности сервера суммаризации...", Toast.LENGTH_SHORT).show();
            updateSummarizedTexts(articlesToUpdate, positions);
        }
    }

    private void updateSummarizedTexts(List<NewsItem> articles, List<Integer> positions) {
        String url = "http://192.168.31.252:8000/api/summarize/summarize/check-summaries";
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        String utf8Response = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.d(TAG, "API Response: " + utf8Response);
                        
                        JSONObject jsonResponse = new JSONObject(utf8Response);
                        String message = jsonResponse.getString("message");
                        Log.d(TAG, "API Message: " + message);
                        
                        // Всегда скрываем диалог, независимо от ответа
                        hideProgressDialog();
                        
                        if (message.equals("Суммаризация завершена")) {
                            boolean isReturningFromResult = getArguments() != null && getArguments().getBoolean("from_result", false);
                            if (!isReturningFromResult) {
                                Toast.makeText(requireContext(), "Суммаризация завершена!", Toast.LENGTH_SHORT).show();
                            }
                            loadUpdatedNews();
                        } else {
                            // Логируем другие сообщения
                            Log.d(TAG, "Получено сообщение от сервера: " + message);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing API response: " + e.getMessage(), e);
                        hideProgressDialog();
                    }
                },
                error -> {
                    Log.e(TAG, "Error making API request: " + error.getMessage(), error);
                    hideProgressDialog();
                    
                    // Показываем ошибку о недоступности сервера суммаризации
                    Toast.makeText(requireContext(), 
                        "Сервер для суммаризации недоступен. Вы можете продолжить работу с приложением.", 
                        Toast.LENGTH_LONG).show();
                    
                    // Обновляем данные из основного сервера, чтобы приложение продолжало работать
                    loadUpdatedNews();
                }
        ) {
            @Override
            public byte[] getBody() {
                try {
                    JSONArray titlesArray = new JSONArray();
                    for (NewsItem article : articles) {
                        titlesArray.put(article.getTitle());
                    }
                    return titlesArray.toString().getBytes("utf-8");
                } catch (Exception e) {
                    Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
                    hideProgressDialog();
                    return null;
                }
            }

            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }
        };

        // Уменьшаем таймаут для быстрой проверки доступности сервера
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                5000, // 5 секунд вместо 30
                1,    // Только 1 повторная попытка
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void startUpdateCheck() {
        new Handler().postDelayed(new Runnable() {
            private int attempts = 0;
            private static final int MAX_ATTEMPTS = 6;

            @Override
            public void run() {
                if (attempts < MAX_ATTEMPTS) {
                    loadUpdatedNews();
                    attempts++;
                    new Handler().postDelayed(this, 5000);
                }
            }
        }, 5000);
    }

    private void loadUpdatedNews() {
        // Получаем параметры из аргументов
        Bundle args = getArguments();
        if (args == null) return;
        
        int themeId = args.getInt("theme_id");
        int newsCount = args.getInt("news_count");
        
        String url = "http://2.59.40.125:8000/api/fill_news/news/";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("topic_id", themeId);
            requestBody.put("limit", newsCount);
        } catch (Exception e) {
            Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
            return;
        }
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        // Преобразование ответа в правильную кодировку
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.d(TAG, "Updated news response: " + decodedResponse);
                        
                        // Обновляем список новостей
                        parseResponse(decodedResponse);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing updated news response: " + e.getMessage(), e);
                    }
                },
                error -> Log.e(TAG, "Error making update request: " + error.getMessage(), error)
        ) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() {
                try {
                    return requestBody.toString().getBytes("utf-8");
                } catch (Exception e) {
                    Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
                    return null;
                }
            }
        };

        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000,
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void navigateToResultFragment(NewsItem newsItem) {
        // Проверяем, есть ли уже суммаризированный текст
        if (newsItem.getSummarizedText() != null && 
            !newsItem.getSummarizedText().isEmpty() && 
            !newsItem.getSummarizedText().equals("Сгенерированный текст")) {
            
            // Если текст уже есть, сразу переходим к результату
            ResultFragment resultFragment = new ResultFragment();
            Bundle resultArgs = new Bundle();
            
            resultArgs.putString("post_theme", newsItem.getTitle());
            resultArgs.putString("publication_date", newsItem.getPublicationDate());
            resultArgs.putString("source", newsItem.getSource());
            resultArgs.putString("link", newsItem.getLink());
            resultArgs.putString("summarized_text", newsItem.getSummarizedText());
            
            resultFragment.setArguments(resultArgs);
            
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, resultFragment)
                    .addToBackStack("ResultFragment")
                    .commit();
        } else {
            // Если текста нет, обновляем данные и затем переходим
            showProgressDialog();
            
            Bundle args = getArguments();
            if (args == null) return;
            
            int themeId = args.getInt("theme_id");
            int newsCount = args.getInt("news_count");
            
            String url = "http://2.59.40.125:8000/api/fill_news/news/";
            
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("topic_id", themeId);
                requestBody.put("limit", newsCount);
            } catch (Exception e) {
                Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
                hideProgressDialog();
                return;
            }
            
            StringRequest request = new StringRequest(Request.Method.POST, url,
                    response -> {
                        try {
                            String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            Log.d(TAG, "Updated news response: " + decodedResponse);
                            
                            parseResponse(decodedResponse);
                            
                            // Находим обновленную новость
                            NewsItem updatedNewsItem = null;
                            for (NewsItem item : newsItems) {
                                if (item.getTitle().equals(newsItem.getTitle())) {
                                    updatedNewsItem = item;
                                    break;
                                }
                            }
                            
                            hideProgressDialog();
                            
                            if (updatedNewsItem != null) {
                                ResultFragment resultFragment = new ResultFragment();
                                Bundle resultArgs = new Bundle();
                                
                                resultArgs.putString("post_theme", updatedNewsItem.getTitle());
                                resultArgs.putString("publication_date", updatedNewsItem.getPublicationDate());
                                resultArgs.putString("source", updatedNewsItem.getSource());
                                resultArgs.putString("link", updatedNewsItem.getLink());
                                resultArgs.putString("summarized_text", updatedNewsItem.getSummarizedText());
                                
                                resultFragment.setArguments(resultArgs);
                                
                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, resultFragment)
                                        .addToBackStack("ResultFragment")
                                        .commit();
                            }
                            
                        } catch (Exception e) {
                            hideProgressDialog();
                            Log.e(TAG, "Error processing updated news response: " + e.getMessage(), e);
                        }
                    },
                    error -> {
                        hideProgressDialog();
                        Log.e(TAG, "Error making update request: " + error.getMessage(), error);
                    }
            ) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    try {
                        return requestBody.toString().getBytes("utf-8");
                    } catch (Exception e) {
                        Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
                        return null;
                    }
                }
            };

            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    30000,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }

}
