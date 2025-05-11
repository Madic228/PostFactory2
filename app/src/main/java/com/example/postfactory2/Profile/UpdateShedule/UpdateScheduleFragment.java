package com.example.postfactory2.Profile.UpdateShedule;

import android.app.DatePickerDialog;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdateScheduleFragment extends Fragment {

    private RequestQueue requestQueue;
    private EditText intervalEditText, articlesEditText, startDateEditText, endDateEditText;
    private ProgressDialog progressDialog;
    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormatter;

    private static final String TAG = "UpdateScheduleFragment"; // Добавляем тег для логов

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_update_schedule, container, false);

        // Инициализация ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Пожалуйста, подождите...");
        progressDialog.setCancelable(false);

        // Инициализация форматтера даты
        dateFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_MONTH, 7); // По умолчанию конечная дата через неделю

        // Инициализация полей ввода
        intervalEditText = rootView.findViewById(R.id.intervalEditText);
        articlesEditText = rootView.findViewById(R.id.articlesEditText);
        startDateEditText = rootView.findViewById(R.id.startDateEditText);
        endDateEditText = rootView.findViewById(R.id.endDateEditText);

        // Устанавливаем значения по умолчанию
        if (intervalEditText.getText().toString().isEmpty()) {
            intervalEditText.setText("6");  // 6 часов по умолчанию
        }
        if (articlesEditText.getText().toString().isEmpty()) {
            articlesEditText.setText("10");  // 10 постов по умолчанию
        }
        
        // Устанавливаем начальные даты
        startDateEditText.setText(dateFormatter.format(startDate.getTime()));
        endDateEditText.setText(dateFormatter.format(endDate.getTime()));

        // Настройка выбора дат
        setupDatePicker(startDateEditText, startDate);
        setupDatePicker(endDateEditText, endDate);

        // Инициализация RequestQueue
        requestQueue = Volley.newRequestQueue(getContext());

        // Настройка кнопок
        rootView.findViewById(R.id.updateButton).setOnClickListener(v -> updateSchedule());
        rootView.findViewById(R.id.startParsingButton).setOnClickListener(v -> startParsing());
        rootView.findViewById(R.id.deleteScheduleButton).setOnClickListener(v -> deleteSchedule());
        rootView.findViewById(R.id.startSummarizeButton).setOnClickListener(v -> startSummarization());

        return rootView;
    }

    private void setupDatePicker(EditText editText, Calendar calendar) {
        editText.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    editText.setText(dateFormatter.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    // Метод для обновления расписания
    private void updateSchedule() {
        if (!validateInputs()) return;

        String url = "http://2.59.40.125:8000/api/parse/update_schedule/";
        
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("interval_hours", Integer.parseInt(intervalEditText.getText().toString()));
            requestBody.put("start_date", startDateEditText.getText().toString());
            requestBody.put("end_date", endDateEditText.getText().toString());
            requestBody.put("max_articles", Integer.parseInt(articlesEditText.getText().toString()));

            JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                requestBody,
                response -> {
                    Log.d(TAG, "Response: " + response.toString());
                    Toast.makeText(getContext(), "Расписание обновлено!", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    String errorMessage = "Ошибка сети";
                    if (error.networkResponse != null) {
                        errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                    }
                    Log.e(TAG, "Error updating schedule: " + errorMessage, error);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            );

            requestQueue.add(jsonRequest);
        } catch (Exception e) {
            Log.e(TAG, "Exception during request preparation", e);
            Toast.makeText(getContext(), "Ошибка при отправке запроса", Toast.LENGTH_LONG).show();
        }
    }

    // Эндпоинт для запуска парсинга всех тем
    private void startParsing() {
        if (!validateInputs()) return;

        String url = "http://2.59.40.125:8000/api/parse/parse_period/";
        
        progressDialog.setMessage("Запуск парсинга... Это может занять несколько минут.");
        progressDialog.show();

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("interval_hours", Integer.parseInt(intervalEditText.getText().toString()));
            requestBody.put("start_date", startDateEditText.getText().toString());
            requestBody.put("end_date", endDateEditText.getText().toString());
            requestBody.put("max_articles", Integer.parseInt(articlesEditText.getText().toString()));

            JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    Log.d(TAG, "Response: " + response.toString());
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Парсинг успешно запущен!", Toast.LENGTH_LONG).show();
                },
                error -> {
                    progressDialog.dismiss();
                    String errorMessage = "Ошибка сети";
                    if (error.networkResponse != null) {
                        errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                    }
                    Log.e(TAG, "Error starting parsing: " + errorMessage, error);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
            );

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

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
            Request.Method.DELETE,
            url,
            null,
            response -> {
                Log.d(TAG, "Response: " + response.toString());
                Toast.makeText(getContext(), "Расписание удалено!", Toast.LENGTH_SHORT).show();
            },
            error -> {
                String errorMessage = "Ошибка сети";
                if (error.networkResponse != null) {
                    errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                }
                Log.e(TAG, "Error deleting schedule: " + errorMessage, error);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        );

        requestQueue.add(jsonRequest);
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

    private boolean validateInputs() {
        if (intervalEditText.getText().toString().isEmpty() ||
            articlesEditText.getText().toString().isEmpty() ||
            startDateEditText.getText().toString().isEmpty() ||
            endDateEditText.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Пожалуйста, заполните все поля.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int intervalHours = Integer.parseInt(intervalEditText.getText().toString());
        int maxArticles = Integer.parseInt(articlesEditText.getText().toString());

        if (intervalHours < 0 || maxArticles < 0) {
            Toast.makeText(getContext(), "Значения не могут быть меньше 0.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (startDate.after(endDate)) {
            Toast.makeText(getContext(), "Начальная дата не может быть позже конечной.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
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
