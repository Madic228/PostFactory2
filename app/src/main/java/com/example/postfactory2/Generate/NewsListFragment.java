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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.R;
import com.android.volley.DefaultRetryPolicy;
import org.json.JSONException;

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

            Log.d(TAG, "Parsing response with " + jsonArray.length() + " items");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                NewsItem newsItem = new NewsItem(
                    item.getString("title"),
                    item.getString("publication_date"),
                    item.getString("source"),
                    item.getString("link"),
                    item.optString("summarized_text", ""),
                    item.getInt("topic_id"),
                    item.optString("content", "")
                );
                newsItems.add(newsItem);
                Log.d(TAG, "Added news item: " + newsItem.getTitle() + " with topic_id: " + newsItem.getTopicId());
            }

            if (newsItems.isEmpty()) {
                Toast.makeText(getContext(), "Новости за выбранный период не найдены", Toast.LENGTH_LONG).show();
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }

            // Получаем выбранный topic_id из аргументов
            Bundle args = getArguments();
            if (args != null && args.containsKey("theme_id")) {
                int selectedTopicId = Integer.parseInt(args.getString("theme_id"));
                Log.d(TAG, "Filtering news for topic_id: " + selectedTopicId);
                
                // Фильтруем новости по выбранной теме
                List<NewsItem> filteredNews = new ArrayList<>();
                for (NewsItem newsItem : newsItems) {
                    if (newsItem.getTopicId() == selectedTopicId) {
                        filteredNews.add(newsItem);
                        Log.d(TAG, "Added filtered news item: " + newsItem.getTitle());
                    }
                }
                
                if (filteredNews.isEmpty()) {
                    Toast.makeText(getContext(), "Новости по выбранной теме не найдены", Toast.LENGTH_LONG).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
                
                newsItems = filteredNews;
            }

            adapter = new NewsAdapter(newsItems, this::navigateToResultFragment);
            rvNewsList.setAdapter(adapter);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
            requireActivity().getSupportFragmentManager().popBackStack();
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

    private void startSummarization(List<NewsItem> articles, List<Integer> positions) {
        String url;
        String regionCode = getArguments() != null ? getArguments().getString("region_code", "ekb") : "ekb";
        String themeId = getArguments() != null ? getArguments().getString("theme_id") : null;

        if (regionCode.equals("ekb")) {
            url = "http://192.168.0.103:8000/summarize/check-summaries";
        } else {
            // Получаем ID региона из кода
            String regionId = getRegionId(regionCode);
            if (regionId == null) {
                Toast.makeText(requireContext(), "Ошибка: регион не найден", Toast.LENGTH_LONG).show();
                return;
            }
            url = String.format("http://192.168.0.103:8000/api/v1/regions/%s/summarize/?topic_id=%s", 
                regionId, themeId);
        }
        Log.d(TAG, "Starting summarization with URL: " + url);
        
        try {
            // Создаем массив заголовков
            JSONArray titlesArray = new JSONArray();
            for (NewsItem article : articles) {
                titlesArray.put(article.getTitle());
            }
            
            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        try {
                            // Преобразуем ответ в правильную кодировку
                            String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            Log.d(TAG, "Ответ от сервера суммаризации: " + decodedResponse);
                            
                            // Проверяем статус ответа
                            if (decodedResponse.contains("Суммаризация завершена") || 
                                decodedResponse.contains("Суммаризация для региона")) {
                                // Запускаем проверку обновленных данных
                                startUpdateCheck();
                            } else {
                                hideProgressDialog();
                                Toast.makeText(requireContext(), "Ошибка при суммаризации: " + decodedResponse, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при обработке ответа суммаризации: " + e.getMessage());
                            hideProgressDialog();
                            Toast.makeText(requireContext(), "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Ошибка при запросе суммаризации: " + error.toString());
                        if (error.networkResponse != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                Log.e(TAG, "Server error details: " + error.networkResponse.statusCode);
                                Log.e(TAG, "Server error data: " + responseBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error response: " + e.getMessage());
                            }
                        }
                        hideProgressDialog();
                        Toast.makeText(requireContext(), "Ошибка при запросе суммаризации", Toast.LENGTH_SHORT).show();
                    }
            ) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    return titlesArray.toString().getBytes();
                }
            };
            
            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    3,      // 3 попытки
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));
            
            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при подготовке запроса суммаризации: " + e.getMessage());
            hideProgressDialog();
            Toast.makeText(requireContext(), "Ошибка подготовки запроса", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRegionId(String regionCode) {
        // Получаем ID региона из аргументов фрагмента
        Bundle args = getArguments();
        if (args != null && args.containsKey("region_id")) {
            return args.getString("region_id");
        }
        return null;
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
                } else {
                    hideProgressDialog();
                    Toast.makeText(requireContext(), "Не удалось получить суммаризированный текст", Toast.LENGTH_SHORT).show();
                }
            }
        }, 5000);
    }

    private void loadUpdatedNews() {
        // Получаем параметры из аргументов
        Bundle args = getArguments();
        if (args == null) return;
        
        String url = "http://2.59.40.125:8000/api/fill_news/news/by-date/";
        
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("start_date", args.getString("start_date"));
            requestBody.put("end_date", args.getString("end_date"));
            
            // Логируем параметры запроса
            Log.d(TAG, "Request body: " + requestBody.toString());
            Log.d(TAG, "Theme ID from args: " + args.getString("theme_id"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
            return;
        }
        
        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        // Преобразование ответа в правильную кодировку
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.d(TAG, "Raw response: " + decodedResponse);
                        
                        // Фильтруем новости по теме
                        JSONArray allNews = new JSONArray(decodedResponse);
                        JSONArray filteredNews = new JSONArray();
                        int themeId = Integer.parseInt(args.getString("theme_id"));
                        
                        Log.d(TAG, "Total news count: " + allNews.length());
                        Log.d(TAG, "Filtering for theme ID: " + themeId);
                        
                        for (int i = 0; i < allNews.length(); i++) {
                            JSONObject newsItem = allNews.getJSONObject(i);
                            int itemTopicId = newsItem.getInt("topic_id");
                            Log.d(TAG, "News item " + i + " topic_id: " + itemTopicId);
                            
                            if (itemTopicId == themeId) {
                                Log.d(TAG, "Adding news item " + i + " to filtered list");
                                filteredNews.put(newsItem);
                            }
                        }
                        
                        Log.d(TAG, "Filtered news count: " + filteredNews.length());
                        
                        if (filteredNews.length() == 0) {
                            Toast.makeText(getContext(), "Новости по выбранной теме не найдены", Toast.LENGTH_LONG).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                            return;
                        }
                        
                        // Обновляем список новостей
                        parseResponse(filteredNews.toString());
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing updated news response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error making update request: " + error.getMessage(), error);
                    String errorMessage;
                    if (error.networkResponse != null) {
                        if (error.networkResponse.statusCode == 404) {
                            errorMessage = "Новости за выбранный период не найдены";
                        } else {
                            errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                        }
                    } else {
                        errorMessage = "Ошибка сети. Проверьте подключение к интернету";
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
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
                3,
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
            openResultFragment(newsItem, newsItem.getSummarizedText());
        } else {
            // Для Екатеринбурга — суммаризация по ссылке
            Bundle args = getArguments();
            String regionCode = args != null ? args.getString("region_code", "ekb") : "ekb";
            if (regionCode.equals("ekb")) {
                showProgressDialog();
                String url = "http://192.168.0.103:8000/summarize/by-link-ekb";
                org.json.JSONObject body = new org.json.JSONObject();
                try {
                    body.put("link", newsItem.getLink());
                } catch (org.json.JSONException e) {
                    hideProgressDialog();
                    Toast.makeText(getContext(), "Ошибка формирования запроса", Toast.LENGTH_SHORT).show();
                    return;
                }
                com.android.volley.toolbox.JsonObjectRequest request = new com.android.volley.toolbox.JsonObjectRequest(
                    com.android.volley.Request.Method.POST, url, body,
                    response -> {
                        hideProgressDialog();
                        String summarizedText = response.optString("summarized_text", "");
                        if (!summarizedText.isEmpty()) {
                            openResultFragment(newsItem, summarizedText);
                        } else {
                            Toast.makeText(getContext(), "Не удалось получить суммаризацию", Toast.LENGTH_SHORT).show();
                        }
                    },
                    error -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "Ошибка при суммаризации", Toast.LENGTH_SHORT).show();
                    }
                );
                // Устанавливаем увеличенный таймаут (5 минут)
                request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    300000, // 5 минут
                    3,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));
                requestQueue.add(request);
            } else {
                // Логика для других регионов (оставить как есть или реализовать аналогично)
                Toast.makeText(getContext(), "Суммаризация для других регионов пока не реализована", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openResultFragment(NewsItem newsItem, String summarizedText) {
        ResultFragment resultFragment = new ResultFragment();
        Bundle resultArgs = new Bundle();
        resultArgs.putString("post_theme", newsItem.getTitle());
        resultArgs.putString("publication_date", newsItem.getPublicationDate());
        resultArgs.putString("source", newsItem.getSource());
        resultArgs.putString("link", newsItem.getLink());
        resultArgs.putString("summarized_text", summarizedText);
        // Добавляем параметры генерации из аргументов
        Bundle args = getArguments();
        if (args != null) {
            resultArgs.putString("tone", args.getString("tone"));
            resultArgs.putString("length", args.getString("length"));
            resultArgs.putString("details", args.getString("details"));
            resultArgs.putStringArray("social_networks", args.getStringArray("social_networks"));
        }
        resultFragment.setArguments(resultArgs);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, resultFragment)
                .addToBackStack("ResultFragment")
                .commit();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

}
