package com.example.postfactory2.Generate;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.android.volley.DefaultRetryPolicy;

import java.util.HashMap;
import java.util.Map;

public class GenerateFragment extends Fragment {

    private AutoCompleteTextView spinnerTheme, spinnerTone;
    private EditText etDetails, startDateEditText, endDateEditText;
    private RadioGroup periodRadioGroup, rgPostLength;
    private LinearLayout customDateLayout;
    private RecyclerView rvSocialNetworks;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormatter;

    private static final String TAG = "GenerateFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        // Инициализация элементов
        spinnerTheme = view.findViewById(R.id.spinerTheme);
        spinnerTone = view.findViewById(R.id.spinnerTone);
        etDetails = view.findViewById(R.id.etDetails);
        startDateEditText = view.findViewById(R.id.startDateEditText);
        endDateEditText = view.findViewById(R.id.endDateEditText);
        periodRadioGroup = view.findViewById(R.id.periodRadioGroup);
        rgPostLength = view.findViewById(R.id.rgPostLength);
        customDateLayout = view.findViewById(R.id.customDateLayout);
        rvSocialNetworks = view.findViewById(R.id.rvSocialNetworks);

        // Инициализация RequestQueue
        requestQueue = Volley.newRequestQueue(requireContext());

        // Инициализация ProgressDialog
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Пожалуйста, подождите...");
        progressDialog.setCancelable(false);

        // Инициализация форматтера даты
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();

        // Настройка элементов
        setupSpinners();
        setupRecyclerView();
        setupDatePicker();
        setupPeriodRadioGroup();

        // Настройка кнопок
        view.findViewById(R.id.btnGenerate).setOnClickListener(v -> onGenerateClicked());
        view.findViewById(R.id.checkStatisticsButton).setOnClickListener(v -> checkStatistics());

        // Устанавливаем начальный период (сегодня)
        updateDatesByPeriod(R.id.todayRadio);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем списки при возвращении на экран
        setupSpinners();
    }

    private void setupSpinners() {
        List<String> themes = new ArrayList<>();
        themes.add("Изменения в законодательстве");
        themes.add("Финансы");
        themes.add("Строительные проекты и застройщики");
        themes.add("ЖКХ");
        themes.add("Ремонт");
        themes.add("Дизайн");

        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            themes
        );
        spinnerTheme.setAdapter(themeAdapter);
        spinnerTheme.setThreshold(1); // Показывать список после ввода 1 символа
        spinnerTheme.setOnClickListener(v -> spinnerTheme.showDropDown());

        List<String> tones = new ArrayList<>();
        tones.add("Информативный");
        tones.add("Продающий");
        tones.add("Экспертный");
        tones.add("Дружелюбный");

        ArrayAdapter<String> toneAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            tones
        );
        spinnerTone.setAdapter(toneAdapter);
        spinnerTone.setThreshold(1); // Показывать список после ввода 1 символа
        spinnerTone.setOnClickListener(v -> spinnerTone.showDropDown());
    }

    private void setupRecyclerView() {
        rvSocialNetworks.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> socialNetworks = new ArrayList<>();
        socialNetworks.add("VK");
        socialNetworks.add("Telegram");

        SocialNetworkAdapter adapter = new SocialNetworkAdapter(socialNetworks);
        rvSocialNetworks.setAdapter(adapter);
    }

    private void setupDatePicker() {
        startDateEditText.setOnClickListener(v -> showDatePicker(startDate, startDateEditText));
        endDateEditText.setOnClickListener(v -> showDatePicker(endDate, endDateEditText));
    }

    private void showDatePicker(Calendar calendar, EditText editText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            android.R.style.Theme_Material_Light_Dialog,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                editText.setText(dateFormatter.format(calendar.getTime()));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void setupPeriodRadioGroup() {
        periodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.customRadio) {
                customDateLayout.setVisibility(View.VISIBLE);
            } else {
                customDateLayout.setVisibility(View.GONE);
                updateDatesByPeriod(checkedId);
            }
        });
    }

    private void updateDatesByPeriod(int periodId) {
        Calendar now = Calendar.getInstance();
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();

        if (periodId == R.id.todayRadio) {
            // Сегодня
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);
            endDate.set(Calendar.HOUR_OF_DAY, 23);
            endDate.set(Calendar.MINUTE, 59);
            endDate.set(Calendar.SECOND, 59);
        } else if (periodId == R.id.weekRadio) {
            // За неделю
            startDate.add(Calendar.DAY_OF_MONTH, -7);
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);
        } else if (periodId == R.id.monthRadio) {
            // За месяц
            startDate.add(Calendar.MONTH, -1);
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);
        }

        if (periodId != R.id.customRadio) {
            startDateEditText.setText(dateFormatter.format(startDate.getTime()));
            endDateEditText.setText(dateFormatter.format(endDate.getTime()));
        }
    }

    private void checkNewsAvailability(Runnable onSuccess) {
        // Форматируем даты в нужный формат (ДД.ММ.ГГГГ)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        
        String startDate;
        String endDate;
        try {
            startDate = outputFormat.format(inputFormat.parse(startDateEditText.getText().toString()));
            endDate = outputFormat.format(inputFormat.parse(endDateEditText.getText().toString()));
            Log.d(TAG, "Formatted dates - Start: " + startDate + ", End: " + endDate);
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error formatting dates: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка форматирования дат", Toast.LENGTH_LONG).show();
            return;
        }

        String url = String.format("http://2.59.40.125:8000/api/parse/parse_once/?start_date=%s&end_date=%s",
                startDate, endDate);
        Log.d(TAG, "Request URL: " + url);
        
        progressDialog.setMessage("Проверка наличия новостей...\nЭто может занять несколько минут");
        progressDialog.show();

        StringRequest request = new StringRequest(
            Request.Method.POST,
            url,
            response -> {
                try {
                    // Декодируем ответ в UTF-8
                    String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                    Log.d(TAG, "Response received: " + decodedResponse);
                    
                    JSONObject jsonResponse = new JSONObject(decodedResponse);
                    if (jsonResponse.has("articles_count")) {
                        int articlesCount = jsonResponse.getInt("articles_count");
                        String message = jsonResponse.getString("message");
                        Log.i(TAG, "Articles found: " + articlesCount + ", Message: " + message);
                        
                        // Показываем сообщение только если найдены новости
                        progressDialog.dismiss();
                        if (articlesCount > 0) {
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                        onSuccess.run();
                    } else {
                        progressDialog.dismiss();
                        Log.w(TAG, "Unexpected response format: " + decodedResponse);
                        Toast.makeText(getContext(), 
                            "Неожиданный формат ответа от сервера", 
                            Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Ошибка при обработке ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            },
            error -> {
                progressDialog.dismiss();
                String errorMessage;
                if (error.networkResponse != null) {
                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        Log.e(TAG, "Error response body: " + responseBody);
                        Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);
                        JSONObject errorJson = new JSONObject(responseBody);
                        if (errorJson.has("detail")) {
                            errorMessage = errorJson.getString("detail");
                            Log.e(TAG, "Error detail: " + errorMessage);
                        } else {
                            errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error response: " + e.getMessage(), e);
                        errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                    }
                } else {
                    Log.e(TAG, "Network error: " + error.getMessage(), error);
                    errorMessage = "Ошибка сети. Проверьте подключение к интернету";
                }
                Log.e(TAG, "Error checking news availability: " + errorMessage, error);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json; charset=utf-8");
                Log.d(TAG, "Request headers: " + headers);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
            300000, // 5 минут таймаут
            3,      // 3 попытки
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Sending request...");
        requestQueue.add(request);
    }

    private void classifyArticles(Runnable onSuccess) {
        String url = "http://192.168.0.103:8000/classify/classify/all";
        Log.d(TAG, "Classification URL: " + url);
        progressDialog.setMessage("Классификация новостей...");
        progressDialog.show();

        StringRequest request = new StringRequest(
            Request.Method.POST,
            url,
            response -> {
                try {
                    // Декодируем ответ в UTF-8
                    String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                    Log.d(TAG, "Classification response: " + decodedResponse);
                    progressDialog.dismiss();
                    onSuccess.run();
                } catch (Exception e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error decoding classification response: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Ошибка при обработке ответа классификации", Toast.LENGTH_LONG).show();
                }
            },
            error -> {
                progressDialog.dismiss();
                String errorMessage;
                if (error.networkResponse != null) {
                    try {
                        String responseBody = new String(error.networkResponse.data, "utf-8");
                        Log.e(TAG, "Classification error response: " + responseBody);
                        Log.e(TAG, "Classification error URL: " + url);
                        Log.e(TAG, "Classification error status code: " + error.networkResponse.statusCode);
                        JSONObject errorJson = new JSONObject(responseBody);
                        if (errorJson.has("detail")) {
                            errorMessage = errorJson.getString("detail");
                            Log.e(TAG, "Classification error detail: " + errorMessage);
                        } else {
                            errorMessage = "Ошибка классификации: " + error.networkResponse.statusCode;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing classification error response: " + e.getMessage(), e);
                        errorMessage = "Ошибка классификации: " + error.networkResponse.statusCode;
                    }
                } else {
                    Log.e(TAG, "Network error during classification: " + error.getMessage(), error);
                    errorMessage = "Ошибка сети при классификации";
                }
                Log.e(TAG, "Classification error: " + errorMessage, error);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json; charset=utf-8");
                Log.d(TAG, "Classification request headers: " + headers);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
            30000,
            3,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Sending classification request...");
        requestQueue.add(request);
    }

    private void checkStatistics() {
        // Проверяем только даты
        if (periodRadioGroup.getCheckedRadioButtonId() == R.id.customRadio) {
            if (startDateEditText.getText().toString().isEmpty() ||
                endDateEditText.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Выберите даты", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startDate.after(endDate)) {
                Toast.makeText(getContext(), "Начальная дата не может быть позже конечной", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Сначала проверяем наличие новостей через разовый парсинг
        checkNewsAvailability(() -> {
            // После успешного парсинга выполняем классификацию
            classifyArticles(() -> {
                // После классификации получаем статистику
                String url = "http://2.59.40.125:8000/api/fill_news/news/statistics/";
                progressDialog.setMessage("Получение статистики...");
                progressDialog.show();

                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("start_date", startDateEditText.getText().toString());
                    requestBody.put("end_date", endDateEditText.getText().toString());

                    JsonObjectRequest jsonRequest = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        requestBody,
                        response -> {
                            progressDialog.dismiss();
                            try {
                                int totalArticles = response.getInt("total_articles");
                                if (totalArticles == 0) {
                                    Toast.makeText(getContext(), 
                                        "Новости за выбранный период не найдены. Попробуйте выбрать другой период.", 
                                        Toast.LENGTH_LONG).show();
                                } else {
                                    showStatisticsDialog(response);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing statistics: " + e.getMessage(), e);
                                Toast.makeText(getContext(), "Ошибка при обработке статистики", Toast.LENGTH_LONG).show();
                            }
                        },
                        error -> {
                            progressDialog.dismiss();
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
                            Log.e(TAG, "Error getting statistics: " + errorMessage, error);
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                        }
                    );

                    jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                        30000,
                        3,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    ));

                    requestQueue.add(jsonRequest);
                } catch (Exception e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Exception during request preparation", e);
                    Toast.makeText(getContext(), "Ошибка при отправке запроса", Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showStatisticsDialog(JSONObject statistics) {
        try {
            StringBuilder message = new StringBuilder();
            JSONObject period = statistics.getJSONObject("period");
            message.append("Статистика за период:\n");
            message.append(period.getString("start_date")).append(" - ")
                   .append(period.getString("end_date")).append("\n\n");
            
            message.append("Всего статей: ").append(statistics.getInt("total_articles")).append("\n\n");
            
            JSONArray topics = statistics.getJSONArray("topics");
            for (int i = 0; i < topics.length(); i++) {
                JSONObject topic = topics.getJSONObject(i);
                message.append(topic.getString("topic_name")).append(": ")
                       .append(topic.getInt("count")).append(" (")
                       .append(String.format("%.1f", topic.getDouble("percentage"))).append("%)\n");
            }

            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Статистика по темам")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing statistics", e);
            Toast.makeText(getContext(), "Ошибка при отображении статистики", Toast.LENGTH_LONG).show();
        }
    }

    private void onGenerateClicked() {
        if (!validateInputs()) return;

        try {
            // Получаем выбранную тему
            String theme = spinnerTheme.getText().toString();
            int themeId = getThemeId(theme);
            if (themeId == -1) {
                Toast.makeText(getContext(), "Выберите тему", Toast.LENGTH_SHORT).show();
                return;
            }

            // Отправляем запрос на получение новостей
            String url = "http://2.59.40.125:8000/api/fill_news/news/by-date/";
            progressDialog.setMessage("Получение новостей...");
            progressDialog.show();

            JSONObject requestBody = new JSONObject();
            requestBody.put("start_date", startDateEditText.getText().toString());
            requestBody.put("end_date", endDateEditText.getText().toString());

            StringRequest request = new StringRequest(Request.Method.POST, url,
                    response -> {
                        try {
                            String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            Log.i(TAG, "Response: " + decodedResponse);

                            // Получаем выбранные параметры
                            String tone = spinnerTone.getText().toString();
                            String length = getSelectedLength();
                            String details = etDetails.getText().toString();
                            String[] socialNetworks = getSelectedSocialNetworks();

                            // Передача данных в NewsListFragment
                            Bundle args = new Bundle();
                            args.putString("response", decodedResponse);
                            args.putString("theme_id", String.valueOf(themeId));
                            args.putString("tone", tone);
                            args.putString("length", length);
                            args.putString("details", details);
                            args.putStringArray("social_networks", socialNetworks);

                            NewsListFragment newsListFragment = new NewsListFragment();
                            newsListFragment.setArguments(args);

                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, newsListFragment)
                                    .addToBackStack(null)
                                    .commit();

                            progressDialog.dismiss();
                        } catch (Exception e) {
                            progressDialog.dismiss();
                            Log.e(TAG, "Error processing response: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        String errorMessage;
                        if (error.networkResponse != null) {
                            if (error.networkResponse.statusCode == 404) {
                                errorMessage = "Новости за выбранный период не найдены";
                            } else {
                                errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                            }
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                Log.e(TAG, "Response body: " + responseBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error response: " + e.getMessage());
                            }
                        } else {
                            errorMessage = "Ошибка сети. Проверьте подключение к интернету";
                        }
                        Log.e(TAG, "Error during request: " + error.toString());
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

            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    3,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error in onGenerateClicked: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private int getThemeId(String theme) {
        switch (theme) {
            case "Изменения в законодательстве":
                return 1;
            case "Финансы":
                return 2;
            case "Строительные проекты и застройщики":
                return 3;
            case "ЖКХ":
                return 4;
            case "Ремонт":
                return 5;
            case "Дизайн":
                return 6;
            default:
                return -1;
        }
    }

    private String getSelectedLength() {
        int selectedId = rgPostLength.getCheckedRadioButtonId();
        
        if (selectedId == R.id.rbShort) {
            return "Короткий";
        } else if (selectedId == R.id.rbMedium) {
            return "Средний";
        } else if (selectedId == R.id.rbLong) {
            return "Длинный";
        }
        return "Средний"; // По умолчанию
    }

    private String[] getSelectedSocialNetworks() {
        SocialNetworkAdapter adapter = (SocialNetworkAdapter) rvSocialNetworks.getAdapter();
        return adapter.getSelectedNetworks();
    }

    private boolean validateInputs() {
        if (spinnerTheme.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Выберите тему", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerTone.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Выберите тон", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (periodRadioGroup.getCheckedRadioButtonId() == R.id.customRadio) {
            if (startDateEditText.getText().toString().isEmpty() ||
                endDateEditText.getText().toString().isEmpty()) {
                Toast.makeText(getContext(), "Выберите даты", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (startDate.after(endDate)) {
                Toast.makeText(getContext(), "Начальная дата не может быть позже конечной", Toast.LENGTH_SHORT).show();
                return false;
            }
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
