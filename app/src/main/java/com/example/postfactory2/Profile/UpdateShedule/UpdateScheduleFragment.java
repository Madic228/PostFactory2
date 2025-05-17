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
    private EditText intervalEditText, periodDaysEditText, checkPreviousDaysEditText, maxArticlesEditText;
    private ProgressDialog progressDialog;
    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormatter;

    private static final String TAG = "UpdateScheduleFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_update_schedule, container, false);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Пожалуйста, подождите...");
        progressDialog.setCancelable(false);

        dateFormatter = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        endDate.add(Calendar.DAY_OF_MONTH, 7);

        intervalEditText = rootView.findViewById(R.id.intervalEditText);
        periodDaysEditText = rootView.findViewById(R.id.periodDaysEditText);
        checkPreviousDaysEditText = rootView.findViewById(R.id.checkPreviousDaysEditText);
        maxArticlesEditText = rootView.findViewById(R.id.maxArticlesEditText);

        if (intervalEditText.getText().toString().isEmpty()) {
            intervalEditText.setText("6");
        }
        if (periodDaysEditText.getText().toString().isEmpty()) {
            periodDaysEditText.setText("7");
        }
        if (checkPreviousDaysEditText.getText().toString().isEmpty()) {
            checkPreviousDaysEditText.setText("2");
        }
        if (maxArticlesEditText.getText().toString().isEmpty()) {
            maxArticlesEditText.setText("10");
        }

        requestQueue = Volley.newRequestQueue(getContext());

        rootView.findViewById(R.id.updateButton).setOnClickListener(v -> updateSchedule());
        rootView.findViewById(R.id.startParsingButton).setOnClickListener(v -> startParsing());
        rootView.findViewById(R.id.deleteScheduleButton).setOnClickListener(v -> deleteSchedule());
        rootView.findViewById(R.id.parseOnceButton).setOnClickListener(v -> parseOnce());
        rootView.findViewById(R.id.startSummarizeButton).setOnClickListener(v -> startSummarization());

        return rootView;
    }

    private void updateSchedule() {
        if (!validateInputs()) return;

        String intervalHours = intervalEditText.getText().toString();
        String periodDays = periodDaysEditText.getText().toString();
        String checkPreviousDays = checkPreviousDaysEditText.getText().toString();

        String url = String.format("http://2.59.40.125:8000/api/parse/update_schedule/?interval_hours=%s&period_days=%s&check_previous_days=%s",
                intervalHours, periodDays, checkPreviousDays);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
            Request.Method.PUT,
            url,
            null,
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
    }

    private void startParsing() {
        if (!validateInputs()) return;

        String intervalHours = intervalEditText.getText().toString();
        String periodDays = periodDaysEditText.getText().toString();
        String checkPreviousDays = checkPreviousDaysEditText.getText().toString();

        String url = String.format("http://2.59.40.125:8000/api/parse/parse_period/?interval_hours=%s&period_days=%s&check_previous_days=%s",
                intervalHours, periodDays, checkPreviousDays);
        
        progressDialog.setMessage("Запуск парсинга... Это может занять несколько минут.");
        progressDialog.show();

        StringRequest request = new StringRequest(
            Request.Method.POST,
            url,
            response -> {
                Log.d(TAG, "Response: " + response);
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

        request.setRetryPolicy(new DefaultRetryPolicy(
            300000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

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

    private void parseOnce() {
        if (!validateInputs()) return;

        String startDateStr = dateFormatter.format(startDate.getTime());
        String endDateStr = dateFormatter.format(endDate.getTime());
        String maxArticles = maxArticlesEditText.getText().toString();

        String url = String.format("http://2.59.40.125:8000/api/parse/parse_once/?start_date=%s&end_date=%s&max_articles=%s",
                startDateStr, endDateStr, maxArticles);
        
        progressDialog.setMessage("Выполнение разового парсинга...");
        progressDialog.show();

        StringRequest request = new StringRequest(
            Request.Method.POST,
            url,
            response -> {
                Log.d(TAG, "Response: " + response);
                progressDialog.dismiss();
                Toast.makeText(getContext(), "Разовый парсинг выполнен!", Toast.LENGTH_LONG).show();
            },
            error -> {
                progressDialog.dismiss();
                String errorMessage = "Ошибка сети";
                if (error.networkResponse != null) {
                    errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                }
                Log.e(TAG, "Error during one-time parsing: " + errorMessage, error);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        );

        requestQueue.add(request);
    }

    private void startSummarization() {
        String url = "http://2.59.40.125:8000/api/summarize/all";
        Log.d(TAG, "Start summarization request to: " + url);

        progressDialog.setMessage("Запуск суммаризации... Это может занять продолжительное время.");
        progressDialog.show();

        StringRequest request = new StringRequest(
            Request.Method.POST,
            url,
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

        request.setRetryPolicy(new DefaultRetryPolicy(
            600000, // 10 минут
            3,      // 3 попытки
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private boolean validateInputs() {
        if (intervalEditText.getText().toString().isEmpty() ||
            periodDaysEditText.getText().toString().isEmpty() ||
            checkPreviousDaysEditText.getText().toString().isEmpty() ||
            maxArticlesEditText.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Пожалуйста, заполните все поля.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int intervalHours = Integer.parseInt(intervalEditText.getText().toString());
        int periodDays = Integer.parseInt(periodDaysEditText.getText().toString());
        int checkPreviousDays = Integer.parseInt(checkPreviousDaysEditText.getText().toString());
        int maxArticles = Integer.parseInt(maxArticlesEditText.getText().toString());

        if (intervalHours < 0 || periodDays < 0 || checkPreviousDays < 0 || maxArticles < 0) {
            Toast.makeText(getContext(), "Значения не могут быть меньше 0.", Toast.LENGTH_SHORT).show();
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
