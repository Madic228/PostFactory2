package com.example.postfactory2.Profile.UpdateShedule;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.R;
import com.example.postfactory2.utils.EnvConfig;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class UpdateScheduleFragment extends Fragment {

    private RequestQueue requestQueue;
    private EditText intervalEditText, articlesEditText;
    private ProgressDialog progressDialog;

    private static final String TAG = "UpdateScheduleFragment"; // Добавляем тег для логов

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_update_schedule, container, false);

        // Инициализация ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Пожалуйста, подождите...");
        progressDialog.setCancelable(false);

        intervalEditText = rootView.findViewById(R.id.intervalEditText);
        articlesEditText = rootView.findViewById(R.id.articlesEditText);

        // Устанавливаем значения по умолчанию, если поля пустые
        if (intervalEditText.getText().toString().isEmpty()) {
            intervalEditText.setText("6");  // 6 часов по умолчанию
        }

        if (articlesEditText.getText().toString().isEmpty()) {
            articlesEditText.setText("10");  // 10 постов по умолчанию
        }

        // Инициализация RequestQueue
        requestQueue = Volley.newRequestQueue(getContext());

        rootView.findViewById(R.id.updateButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSchedule();
            }
        });

        rootView.findViewById(R.id.startParsingButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startParsing();
            }
        });

        rootView.findViewById(R.id.deleteScheduleButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteSchedule();
            }
        });

        rootView.findViewById(R.id.startSummarizeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSummarization();
            }
        });

        return rootView;
    }

    // Метод для обновления расписания
    private void updateSchedule() {
        String intervalText = intervalEditText.getText().toString();
        String articlesText = articlesEditText.getText().toString();

        // Проверка на пустые поля
        if (intervalText.isEmpty() || articlesText.isEmpty()) {
            Toast.makeText(getContext(), "Пожалуйста, заполните все поля.", Toast.LENGTH_SHORT).show();
            return;
        }

        int intervalHours = Integer.parseInt(intervalText);
        int maxArticles = Integer.parseInt(articlesText);

        // Проверка на отрицательные значения
        if (intervalHours < 0 || maxArticles < 0) {
            Toast.makeText(getContext(), "Значения не могут быть меньше 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = String.format("http://2.59.40.125:8000/api/parse/update_schedule/?interval_hours=%d&max_articles=%d",
                intervalHours, maxArticles);

        Log.d(TAG, "Update schedule: intervalHours = " + intervalHours + ", maxArticles = " + maxArticles);

        try {
            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.PUT, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "Response: " + response.toString());
                            Toast.makeText(getContext(), "Интервал обновлен!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "Ошибка сети";
                            if (error.networkResponse != null) {
                                errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                            }
                            Log.e(TAG, "Error updating schedule: " + errorMessage, error);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });

            requestQueue.add(jsonRequest);
        } catch (Exception e) {
            Log.e(TAG, "Exception during request preparation", e);
            Toast.makeText(getContext(), "Ошибка при отправке запроса", Toast.LENGTH_LONG).show();
        }
    }

    // Эндпоинт для запуска парсинга всех тем
    private void startParsing() {
        String intervalText = intervalEditText.getText().toString();
        String articlesText = articlesEditText.getText().toString();

        // Проверка на пустые поля
        if (intervalText.isEmpty() || articlesText.isEmpty()) {
            Toast.makeText(getContext(), "Пожалуйста, заполните все поля.", Toast.LENGTH_SHORT).show();
            return;
        }

        int intervalHours = Integer.parseInt(intervalText);
        int maxArticles = Integer.parseInt(articlesText);

        // Проверка на отрицательные значения
        if (intervalHours < 0 || maxArticles < 0) {
            Toast.makeText(getContext(), "Значения не могут быть меньше 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = String.format("http://2.59.40.125:8000/api/parse/parse_all/?interval_hours=%d&max_articles=%d",
                intervalHours, maxArticles);

        Log.d(TAG, "Start parsing: intervalHours = " + intervalHours + ", maxArticles = " + maxArticles);

        // Показываем диалог загрузки
        progressDialog.setMessage("Запуск парсинга... Это может занять несколько минут.");
        progressDialog.show();

        try {
            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "Response: " + response.toString());
                            progressDialog.dismiss();
                            Toast.makeText(getContext(), "Парсинг всех тем успешно запущен!", Toast.LENGTH_LONG).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressDialog.dismiss();
                            String errorMessage = "Ошибка сети";
                            if (error.networkResponse != null) {
                                errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                            }
                            Log.e(TAG, "Error starting parsing: " + errorMessage, error);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });

            // Увеличиваем таймаут до 5 минут
            jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                    300000, // 5 минут
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(jsonRequest);
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Exception during request preparation", e);
            Toast.makeText(getContext(), "Ошибка при отправке запроса", Toast.LENGTH_LONG).show();
        }
    }

    // Эндпоинт для удаления расписания
    private void deleteSchedule() {
        String url = "http://2.59.40.125:8000/api/parse/delete_schedule/";

        Log.d(TAG, "Delete schedule request");

        try {
            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.DELETE, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.d(TAG, "Response: " + response.toString());
                            Toast.makeText(getContext(), "Парсинг удален из расписания!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "Ошибка сети";
                            if (error.networkResponse != null) {
                                errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                            }
                            Log.e(TAG, "Error deleting schedule: " + errorMessage, error);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json; charset=utf-8");
                    return headers;
                }
            };

            requestQueue.add(jsonRequest);
        } catch (Exception e) {
            Log.e(TAG, "Exception during request preparation", e);
            Toast.makeText(getContext(), "Ошибка при отправке запроса", Toast.LENGTH_LONG).show();
        }
    }

    // Метод для запуска суммаризации всех новостей
    private void startSummarization() {
        String url = "http://192.168.0.103:8000/all?mode=full";
        Log.d(TAG, "Start summarization request to: " + url);

        // Показываем диалог загрузки
        progressDialog.setMessage("Запуск суммаризации... Это может занять продолжительное время.");
        progressDialog.show();

        try {
            StringRequest request = new StringRequest(Request.Method.POST, url,
                    response -> {
                        Log.d(TAG, "Response: " + response);
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Суммаризация успешно запущена!", Toast.LENGTH_LONG).show();
                    },
                    error -> {
                        progressDialog.dismiss();
                        String errorMessage = "Ошибка сети";
                        if (error.networkResponse != null) {
                            errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                Log.e(TAG, "Server error details: " + error.networkResponse.statusCode);
                                Log.e(TAG, "Server error data: " + responseBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error response: " + e.getMessage());
                            }
                        }
                        Log.e(TAG, "Error starting summarization: " + errorMessage, error);
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
            );

            // Увеличиваем таймаут до 10 минут из-за длительности процесса суммаризации
            request.setRetryPolicy(new DefaultRetryPolicy(
                    600000, // 10 минут
                    3,      // 3 попытки
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Exception during request preparation", e);
            Toast.makeText(getContext(), "Ошибка при отправке запроса", Toast.LENGTH_LONG).show();
        }
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
