package com.example.postfactory2.Generate;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import com.android.volley.DefaultRetryPolicy;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.VolleyError;

import java.text.ParseException;

public class GenerateFragment extends Fragment {

    private static final String REGIONS_BASE_URL = "http://192.168.0.103:8000";
    private static final String API_BASE_URL = "http://2.59.40.125:8000";

    private static final String REGIONS_API_URL = REGIONS_BASE_URL + "/api/v1";
    private static final String API_V1_URL = API_BASE_URL + "/api/v1";
    private static final String CLASSIFY_URL = REGIONS_BASE_URL + "/classify";
    private static final String SUMMARIZE_URL = REGIONS_BASE_URL + "/summarize";
    private static final String EKB_PARSE_URL = API_BASE_URL + "/api/parse/parse_once/";

    private AutoCompleteTextView spinnerTheme, spinnerTone, regionSpinner;
    private EditText etDetails, startDateEditText, endDateEditText;
    private RadioGroup periodRadioGroup, rgPostLength;
    private LinearLayout customDateLayout;
    private RecyclerView rvSocialNetworks;
    private RequestQueue requestQueue;
    private ProgressDialog progressDialog;
    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormatter;
    private String selectedRegionCode = "ekb"; // По умолчанию Екатеринбург

    private static final String TAG = "GenerateFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        // Инициализация элементов
        spinnerTheme = view.findViewById(R.id.spinerTheme);
        spinnerTone = view.findViewById(R.id.spinnerTone);
        regionSpinner = view.findViewById(R.id.regionSpinner);
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
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        // Инициализация форматтера даты
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();

        // Настройка элементов
        setupSpinners();
        setupRegionSpinner();
        setupRecyclerView();
        setupDatePicker();
        setupPeriodRadioGroup();

        // Настройка кнопок
        view.findViewById(R.id.btnGenerate).setOnClickListener(v -> onGenerateClicked());
        view.findViewById(R.id.checkStatisticsButton).setOnClickListener(v -> checkStatistics());
        view.findViewById(R.id.btnAddRegion).setOnClickListener(v -> showAddRegionDialog());

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

    private void setupRegionSpinner() {
        String url = REGIONS_API_URL + "/regions/";
        Log.d(TAG, "Loading regions list - URL: " + url);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<String> regions = new ArrayList<>();
                        Map<String, String> regionCodeMap = new HashMap<>();
                        Map<String, String> regionIdMap = new HashMap<>();

                        Log.d(TAG, "Received " + response.length() + " regions");
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject region = response.getJSONObject(i);
                            String name = region.getString("name");
                            String code = region.getString("code");
                            String id = String.valueOf(region.getInt("id"));
                            regions.add(name);
                            regionCodeMap.put(name, code);
                            regionIdMap.put(code, id);
                            Log.d(TAG, "Region " + (i + 1) + ": " + name + " (code: " + code + ", id: " + id + ")");
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            regions
                        );
                        regionSpinner.setAdapter(adapter);
                        regionSpinner.setThreshold(1);
                        regionSpinner.setOnClickListener(v -> regionSpinner.showDropDown());

                        // Устанавливаем обработчик выбора региона
                        regionSpinner.setOnItemClickListener((parent, view, position, id) -> {
                            String selectedRegion = regions.get(position);
                            selectedRegionCode = regionCodeMap.get(selectedRegion);
                            String regionId = regionIdMap.get(selectedRegionCode);
                            Log.d(TAG, "Selected region: " + selectedRegion +
                                    " (code: " + selectedRegionCode +
                                    ", id: " + regionId + ")");
                        });

                        // Добавляем обработчик длительного нажатия для редактирования
                        regionSpinner.setOnTouchListener((v, event) -> {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                v.postDelayed(() -> {
                                    if (v.isPressed()) {
                                        String selectedText = regionSpinner.getText().toString();
                                        if (!selectedText.isEmpty()) {
                                            String code = regionCodeMap.get(selectedText);
                                            String id = regionIdMap.get(code);
                                            if (code != null && id != null) {
                                                showEditRegionDialog(selectedText, code, Integer.parseInt(id));
                                            }
                                        }
                                    }
                                }, 500); // 500ms для определения длительного нажатия
                            }
                            return false;
                        });

                        // Устанавливаем Екатеринбург по умолчанию
                        for (int i = 0; i < regions.size(); i++) {
                            if (regionCodeMap.get(regions.get(i)).equals("ekb")) {
                                regionSpinner.setText(regions.get(i), false);
                                selectedRegionCode = "ekb";
                                Log.d(TAG, "Set default region: " + regions.get(i) +
                                        " (code: ekb, id: " + regionIdMap.get("ekb") + ")");
                                break;
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing regions: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка загрузки списка регионов", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading regions: " + error.getMessage(), error);
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Error response body: " + responseBody);
                            Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error response: " + e.getMessage(), e);
                        }
                    }
                    Toast.makeText(getContext(), "Ошибка загрузки списка регионов", Toast.LENGTH_SHORT).show();
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Sending regions list request...");
        requestQueue.add(request);
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

    private void checkStatistics() {
        if (selectedRegionCode == null) {
            showError("Выберите регион");
            return;
        }

        String startDate = getStartDate();
        String endDate = getEndDate();

        if (startDate == null || endDate == null) {
            showError("Выберите период");
            return;
        }

        Log.d(TAG, "Starting statistics check for region: " + selectedRegionCode);
        Log.d(TAG, "Selected dates - Start: " + startDate + ", End: " + endDate);

        if (selectedRegionCode.equals("ekb")) {
            // Для Екатеринбурга используем старый эндпоинт
            String url = API_BASE_URL + "/api/fill_news/news/statistics/";
            Log.d(TAG, "EKB region detected, using URL: " + url);
            progressDialog.setMessage("Проверка новостей...");
            progressDialog.show();

            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("start_date", startDate);
                requestBody.put("end_date", endDate);
                Log.d(TAG, "EKB Request body: " + requestBody.toString());

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        requestBody,
                        response -> {
                            try {
                                Log.i(TAG, "EKB Response received: " + response.toString());
                                int totalArticles = response.getInt("total_articles");
                                Log.d(TAG, "Total articles found: " + totalArticles);

                                if (totalArticles == 0) {
                                    Log.w(TAG, "No articles found for the selected period");
                                    showError("Нет новостей за выбранный период");
                                } else {
                                    Log.i(TAG, "Successfully found " + totalArticles + " articles");
                                    showStatisticsDialog(response);
                                }
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                progressDialog.dismiss();
                                Log.e(TAG, "Error processing EKB response: " + e.getMessage(), e);
                                Log.e(TAG, "Stack trace: " + Log.getStackTraceString(e));
                                Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        },
                        error -> {
                            progressDialog.dismiss();
                            Log.e(TAG, "EKB Request failed with error: " + error.getMessage());
                            if (error.networkResponse != null) {
                                try {
                                    String errorResponse = new String(error.networkResponse.data, "utf-8");
                                    Log.e(TAG, "EKB Error response body: " + errorResponse);
                                    Log.e(TAG, "EKB Error status code: " + error.networkResponse.statusCode);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error reading EKB error response: " + e.getMessage(), e);
                                }
                            }
                            handleError(error, "Error during EKB request");
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("accept", "application/json");
                        headers.put("Content-Type", "application/json");
                        Log.d(TAG, "EKB Request headers: " + headers);
                        return headers;
                    }
                };

                request.setRetryPolicy(new DefaultRetryPolicy(
                        30000,
                        3,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                Log.d(TAG, "Sending EKB statistics check request...");
                requestQueue.add(request);
            } catch (Exception e) {
                progressDialog.dismiss();
                Log.e(TAG, "Error creating EKB request: " + e.getMessage(), e);
                Log.e(TAG, "Stack trace: " + Log.getStackTraceString(e));
                Toast.makeText(getContext(), "Ошибка при создании запроса: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            // Для других регионов проверяем статистику
            getRegionId(selectedRegionCode, regionId -> {
                if (regionId == null) {
                    Log.e(TAG, "Failed to get region ID for code: " + selectedRegionCode);
                    Toast.makeText(getContext(), "Ошибка: регион не найден", Toast.LENGTH_LONG).show();
                    return;
                }

                String statsUrl = String.format(REGIONS_API_URL + "/regions/%s/news/statistics/?start_date=%s&end_date=%s",
                        regionId,
                        startDate,
                        endDate);
                Log.d(TAG, "Checking statistics - URL: " + statsUrl);

                progressDialog.setMessage("Проверка статистики...");
                progressDialog.show();

                JsonObjectRequest statsRequest = new JsonObjectRequest(
                        Request.Method.GET,
                        statsUrl,
                        null,
                        response -> {
                            try {
                                Log.d(TAG, "Statistics response received: " + response.toString());
                                int totalArticles = response.getInt("total_articles");
                                Log.d(TAG, "Total articles found: " + totalArticles);

                                if (totalArticles == 0) {
                                    Log.w(TAG, "No articles found in statistics");
                                    Toast.makeText(getContext(),
                                            "Нет новостей за выбранный период. Парсинг запущен автоматически через планировщик.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Log.i(TAG, "Found " + totalArticles + " articles in statistics");
                                    showStatisticsDialog(response);
                                }
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                progressDialog.dismiss();
                                Log.e(TAG, "Error parsing statistics response: " + e.getMessage(), e);
                                Toast.makeText(getContext(), "Ошибка при обработке статистики", Toast.LENGTH_LONG).show();
                            }
                        },
                        error -> {
                            progressDialog.dismiss();
                            if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                                Toast.makeText(getContext(),
                                        "Нет новостей за выбранный период. Парсинг запущен автоматически через планировщик.",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                handleError(error, "Error getting statistics");
                            }
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("accept", "application/json");
                        Log.d(TAG, "Statistics request headers: " + headers);
                        return headers;
                    }
                };

                statsRequest.setRetryPolicy(new DefaultRetryPolicy(
                        30000,
                        3,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                Log.d(TAG, "Sending statistics request...");
                requestQueue.add(statsRequest);
            });
        }
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
            message.append("Статистика по темам:\n");
            for (int i = 0; i < topics.length(); i++) {
                JSONObject topic = topics.getJSONObject(i);
                message.append(topic.getString("topic_name")).append(": ")
                        .append(topic.getInt("count")).append(" (")
                        .append(String.format("%.1f", topic.getDouble("percentage"))).append("%)\n");
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Статистика новостей")
                    .setMessage(message.toString())
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing statistics", e);
            Toast.makeText(getContext(), "Ошибка при отображении статистики", Toast.LENGTH_LONG).show();
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

        if (selectedRegionCode.equals("ekb")) {
            // Для Екатеринбурга используем эндпоинт парсинга
            String url = API_BASE_URL + "/api/v1/parse_once/";
            Log.d(TAG, "Request URL: " + url);

            progressDialog.setMessage("Подготовка данных...\nЭто может занять несколько минут");
            progressDialog.show();

            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("start_date", startDate);
                requestBody.put("end_date", endDate);
                Log.d(TAG, "Request body: " + requestBody.toString());

                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        requestBody,
                        response -> {
                            try {
                                Log.d(TAG, "Response received: " + response.toString());

                                if (response.has("articles_count")) {
                                    int articlesCount = response.getInt("articles_count");
                                    String responseMessage = response.getString("message");
                                    Log.i(TAG, "Articles found: " + articlesCount + ", Message: " + responseMessage);

                                    progressDialog.dismiss();
                                    if (articlesCount > 0) {
                                        Toast.makeText(getContext(), responseMessage, Toast.LENGTH_LONG).show();
                                        // Запускаем только классификацию для Екатеринбурга
                                            classifyArticles(() -> {
                                                // После классификации получаем статистику
                                                String statsUrl = API_BASE_URL + "/api/fill_news/news/statistics/";
                                                progressDialog.setMessage("Получение статистики...");
                                                progressDialog.show();

                                                try {
                                                    JSONObject statsRequestBody = new JSONObject();
                                                    statsRequestBody.put("start_date", startDate);
                                                    statsRequestBody.put("end_date", endDate);
                                                    Log.d(TAG, "Statistics request body: " + statsRequestBody.toString());

                                                    JsonObjectRequest statsRequest = new JsonObjectRequest(
                                                            Request.Method.POST,
                                                            statsUrl,
                                                            statsRequestBody,
                                                            statsResponse -> {
                                                                progressDialog.dismiss();
                                                                try {
                                                                    Log.d(TAG, "Statistics response received: " + statsResponse.toString());

                                                                    // Старый формат диалога для Екатеринбурга
                                                                    StringBuilder message = new StringBuilder();
                                                                    message.append("Статистика новостей:\n\n");

                                                                    // Общее количество статей
                                                                    int totalArticles = statsResponse.getInt("total_articles");
                                                                    message.append("Всего статей: ").append(totalArticles).append("\n\n");

                                                                    // Статистика по темам
                                                                    if (statsResponse.has("topics_statistics")) {
                                                                        JSONObject topicsStats = statsResponse.getJSONObject("topics_statistics");
                                                                        message.append("Статистика по темам:\n");
                                                                        for (int i = 1; i <= 6; i++) {
                                                                            String topicName = getTopicName(i);
                                                                            int count = topicsStats.optInt(String.valueOf(i), 0);
                                                                            message.append(topicName).append(": ").append(count).append("\n");
                                                                        }
                                                                        message.append("\n");
                                                                    }

                                                                    // Статистика по источникам
                                                                    if (statsResponse.has("sources_statistics")) {
                                                                        JSONObject sourcesStats = statsResponse.getJSONObject("sources_statistics");
                                                                        message.append("Статистика по источникам:\n");
                                                                        for (int i = 1; i <= 3; i++) {
                                                                            String sourceName = getSourceName(i);
                                                                            int count = sourcesStats.optInt(String.valueOf(i), 0);
                                                                            message.append(sourceName).append(": ").append(count).append("\n");
                                                                        }
                                                                    }

                                                                    // Показываем диалог со статистикой
                                                                    new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                                                                            .setTitle("Статистика новостей")
                                                                            .setMessage(message.toString())
                                                                            .setPositiveButton("OK", null)
                                                                            .show();

                                                                } catch (Exception e) {
                                                                    Log.e(TAG, "Error parsing statistics response: " + e.getMessage(), e);
                                                                    Toast.makeText(getContext(), "Ошибка при обработке статистики", Toast.LENGTH_LONG).show();
                                                                }
                                                            },
                                                            error -> {
                                                                progressDialog.dismiss();
                                                                handleError(error, "Error getting statistics");
                                                            }
                                                    ) {
                                                        @Override
                                                        public Map<String, String> getHeaders() {
                                                            Map<String, String> headers = new HashMap<>();
                                                            headers.put("accept", "application/json");
                                                            headers.put("Content-Type", "application/json");
                                                            Log.d(TAG, "Statistics request headers: " + headers);
                                                            return headers;
                                                        }
                                                    };

                                                    statsRequest.setRetryPolicy(new DefaultRetryPolicy(
                                                            30000,
                                                            3,
                                                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                                                    ));

                                                    Log.d(TAG, "Sending statistics request to URL: " + statsUrl);
                                                    requestQueue.add(statsRequest);
                                                } catch (Exception e) {
                                                    progressDialog.dismiss();
                                                    Log.e(TAG, "Exception during statistics request preparation", e);
                                                    Toast.makeText(getContext(), "Ошибка при отправке запроса статистики", Toast.LENGTH_LONG).show();
                                                }
                                        });
                                    }
                                } else {
                                    progressDialog.dismiss();
                                    Log.w(TAG, "Unexpected response format: " + response.toString());
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
                            if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                                // Если новости не найдены, запускаем парсинг
                                startEkbParsing(startDate, endDate, onSuccess);
                            } else {
                                handleError(error, "Error checking news availability");
                            }
                        }
                ) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/json");
                        return headers;
                    }
                };

                request.setRetryPolicy(new DefaultRetryPolicy(
                        300000, // 5 минут таймаут
                        3,      // 3 попытки
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                requestQueue.add(request);
            } catch (Exception e) {
                progressDialog.dismiss();
                Log.e(TAG, "Error creating request: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Ошибка при создании запроса", Toast.LENGTH_LONG).show();
            }
        } else {
            // Для других регионов используем новый эндпоинт
            getRegionId(selectedRegionCode, regionId -> {
                if (regionId == null) {
                    Log.e(TAG, "Failed to get region ID for code: " + selectedRegionCode);
                    Toast.makeText(getContext(), "Ошибка: регион не найден", Toast.LENGTH_LONG).show();
                    return;
                }

                String url = String.format(REGIONS_API_URL + "/regions/%s/news/by-date/", regionId);
                Log.d(TAG, "Checking region news availability - URL: " + url);

                try {
                    JSONObject requestBody = new JSONObject();
                    requestBody.put("start_date", startDate);
                    requestBody.put("end_date", endDate);
                    Log.d(TAG, "Region news availability request body: " + requestBody.toString());

                    JsonObjectRequest request = new JsonObjectRequest(
                            Request.Method.POST,
                            url,
                            requestBody,
                            response -> {
                                try {
                                    Log.d(TAG, "Region news availability response: " + response.toString());

                                    if (response.has("articles_count")) {
                                        int articlesCount = response.getInt("articles_count");
                                        String responseMessage = response.getString("message");
                                        Log.i(TAG, "Region articles found: " + articlesCount + ", Message: " + responseMessage);

                                                                progressDialog.dismiss();
                                        if (articlesCount > 0) {
                                            Toast.makeText(getContext(), responseMessage, Toast.LENGTH_LONG).show();
                                            // Запускаем только классификацию для региона
                                            classifyRegionArticles(regionId, onSuccess);
                                        }
                                    }
                                                } catch (Exception e) {
                                                    progressDialog.dismiss();
                                    Log.e(TAG, "Error parsing region news availability response: " + e.getMessage(), e);
                                    Toast.makeText(getContext(), "Ошибка при обработке ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            },
                            error -> {
                                progressDialog.dismiss();
                                if (error.networkResponse != null && error.networkResponse.statusCode == 404) {
                                            // Если новости не найдены, запускаем парсинг
                                            performRegionParse(regionId, startDate, endDate, onSuccess);
                                } else {
                                handleError(error, "Error checking region news availability");
                                }
                            }
                    ) {
                        @Override
                        public Map<String, String> getHeaders() {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/json");
                            return headers;
                        }
                    };

                    request.setRetryPolicy(new DefaultRetryPolicy(
                            300000, // 5 минут таймаут
                            3,      // 3 попытки
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                    ));

                    requestQueue.add(request);
                } catch (Exception e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error creating region news availability request: " + e.getMessage(), e);
                    Toast.makeText(getContext(), "Ошибка при создании запроса", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void startEkbSummarization(Runnable onSuccess) {
        Log.d(TAG, "Starting EKB summarization");
        String url = SUMMARIZE_URL + "/all";

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("mode", "full");
            Log.d(TAG, "Summarization request body: " + requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        try {
                            Log.d(TAG, "Summarization response: " + response.toString());
                            onSuccess.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in summarization response: " + e.getMessage(), e);
                            onSuccess.run(); // Продолжаем выполнение даже при ошибке суммаризации
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error during summarization: " + error.getMessage(), error);
                        onSuccess.run(); // Продолжаем выполнение даже при ошибке суммаризации
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    300000, // 5 минут таймаут
                    3,      // 3 попытки
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error creating summarization request: " + e.getMessage(), e);
            onSuccess.run(); // Продолжаем выполнение даже при ошибке суммаризации
        }
    }

    private void startRegionSummarization(String regionId, Runnable onSuccess) {
        Log.d(TAG, "Starting region summarization for region ID: " + regionId);
        String url = String.format(SUMMARIZE_URL + "/regions/%s", regionId);
        Log.d(TAG, "Region summarization URL: " + url);

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("mode", "full");
            Log.d(TAG, "Region summarization request body: " + requestBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    requestBody,
                    response -> {
                        try {
                            Log.d(TAG, "Region summarization response: " + response.toString());
                            String message = response.getString("message");
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            onSuccess.run();
                        } catch (Exception e) {
                            Log.e(TAG, "Error in region summarization response: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Ошибка при суммаризации: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            onSuccess.run(); // Продолжаем выполнение даже при ошибке суммаризации
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error during region summarization: " + error.getMessage(), error);
                        if (error.networkResponse != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                Log.e(TAG, "Error response body: " + responseBody);
                                Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);

                                if (error.networkResponse.statusCode == 400) {
                                    Toast.makeText(getContext(), "Неверный режим суммаризации", Toast.LENGTH_LONG).show();
                                } else if (error.networkResponse.statusCode == 500) {
                                    Toast.makeText(getContext(), "Ошибка при суммаризации статей", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getContext(), "Ошибка при суммаризации: " + error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error response: " + e.getMessage(), e);
                                Toast.makeText(getContext(), "Ошибка при суммаризации", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Ошибка сети при суммаризации", Toast.LENGTH_LONG).show();
                        }
                        onSuccess.run(); // Продолжаем выполнение даже при ошибке суммаризации
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    Log.d(TAG, "Region summarization request headers: " + headers);
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    300000, // 5 минут таймаут
                    3,      // 3 попытки
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            Log.d(TAG, "Sending region summarization request...");
            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error creating region summarization request: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка при создании запроса суммаризации", Toast.LENGTH_LONG).show();
            onSuccess.run(); // Продолжаем выполнение даже при ошибке суммаризации
        }
    }

    private void startEkbParsing(String startDate, String endDate, Runnable onSuccess) {
        String url = String.format(EKB_PARSE_URL + "?start_date=%s&end_date=%s",
                startDate, endDate);
        Log.d(TAG, "Starting EKB parsing - URL: " + url);

        progressDialog.setMessage("Запуск парсинга новостей...\nЭто может занять несколько минут");
        progressDialog.show();

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        // Декодируем ответ в UTF-8
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.d(TAG, "Parse response: " + decodedResponse);
                        JSONObject jsonResponse = new JSONObject(decodedResponse);

                        if (jsonResponse.has("articles_count")) {
                            int articlesCount = jsonResponse.getInt("articles_count");
                            String message = jsonResponse.getString("message");
                            Log.i(TAG, "Articles parsed: " + articlesCount + ", Message: " + message);

                            progressDialog.dismiss();
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            onSuccess.run();
                        } else {
                            progressDialog.dismiss();
                            Log.w(TAG, "Unexpected parse response format: " + decodedResponse);
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
                    handleError(error, "Error during EKB parsing");
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                300000, // 5 минут таймаут
                3,      // 3 попытки
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private String formatDateForParsing(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(date));
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage(), e);
            return date;
        }
    }

    private void performRegionParse(String regionId, String startDate, String endDate, Runnable onSuccess) {
        String formattedStartDate = formatDateForParsing(startDate);
        String formattedEndDate = formatDateForParsing(endDate);

        String url = String.format(REGIONS_API_URL + "/regions/%s/parse_period/?start_date=%s&end_date=%s",
                regionId, formattedStartDate, formattedEndDate);
        Log.d(TAG, "Starting region parse - URL: " + url);
        Log.d(TAG, "Formatted dates - Start: " + formattedStartDate + ", End: " + formattedEndDate);

        // Закрываем предыдущий диалог, если он открыт
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // Создаем новый диалог
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage("Выполняется парсинг новостей...\nЭто может занять несколько минут");
        progressDialog.show();

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.d(TAG, "Parse response: " + decodedResponse);
                        JSONObject jsonResponse = new JSONObject(decodedResponse);

                        if (jsonResponse.has("message")) {
                            String message = jsonResponse.getString("message");
                            int articlesCount = jsonResponse.optInt("articles_count", 0);
                            Log.i(TAG, "Parse response: " + message + ", Articles: " + articlesCount);

                            if (articlesCount > 0) {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                                onSuccess.run();
                            } else {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Toast.makeText(getContext(),
                                        "Новости за выбранный период не найдены",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Log.e(TAG, "Unexpected parse response format: " + decodedResponse);
                            Toast.makeText(getContext(),
                                    "Неожиданный формат ответа от сервера",
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка при обработке ответа", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Parse error response body: " + responseBody);
                            Log.e(TAG, "Parse error status code: " + error.networkResponse.statusCode);

                            if (error.networkResponse.statusCode == 404) {
                                Toast.makeText(getContext(),
                                        "Новости за выбранный период не найдены",
                                        Toast.LENGTH_LONG).show();
                            } else {
                                handleError(error, "Error during region parse");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error response: " + e.getMessage(), e);
                            handleError(error, "Error during region parse");
                        }
                    } else {
                        handleError(error, "Error during region parse");
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                Log.d(TAG, "Parse request headers: " + headers);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                300000, // 5 минут таймаут
                3,      // 3 попытки
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Sending parse request...");
        requestQueue.add(request);
    }

    private void classifyRegionArticles(String regionId, Runnable onSuccess) {
        String url = String.format(API_V1_URL + "/regions/%s/classify/", regionId);
        Log.d(TAG, "Starting region classification - URL: " + url);

        progressDialog.setMessage("Выполняется классификация новостей...");
        progressDialog.show();

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    try {
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.d(TAG, "Classification response: " + decodedResponse);

                        JSONObject jsonResponse = new JSONObject(decodedResponse);
                        String message = jsonResponse.getString("message");
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        onSuccess.run();
                    } catch (Exception e) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error parsing classification response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка при обработке ответа классификации", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    handleError(error, "Error during region classification");
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json; charset=utf-8");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                3,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void checkRegionNewsAvailability(String regionId, String startDate, String endDate, Runnable onSuccess) {
        // Преобразуем regionId в число для использования в URL и теле запроса
        int regionIdInt = Integer.parseInt(regionId);
        String url = REGIONS_API_URL + "/regions/" + regionIdInt + "/news/by-date/";
        Log.d(TAG, "=== Начало запроса проверки новостей региона ===");
        Log.d(TAG, "URL запроса: " + url);
        Log.d(TAG, "Входные параметры - regionId: " + regionIdInt + ", startDate: " + startDate + ", endDate: " + endDate);

        try {
            // Преобразуем даты из dd.MM.yyyy в yyyy-MM-dd
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            String formattedStartDate = outputFormat.format(inputFormat.parse(startDate));
            String formattedEndDate = outputFormat.format(inputFormat.parse(endDate));
            Log.d(TAG, "Даты после форматирования - Start: " + formattedStartDate + ", End: " + formattedEndDate);

            JSONObject requestBody = new JSONObject();
            requestBody.put("start_date", formattedStartDate);
            requestBody.put("end_date", formattedEndDate);
            requestBody.put("region_id", regionIdInt);
            Log.d(TAG, "Тело запроса: " + requestBody.toString());

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        try {
                            String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            Log.d(TAG, "Ответ сервера: " + decodedResponse);

                            // Обрабатываем ответ как JSONArray
                            JSONArray articles = new JSONArray(decodedResponse);
                            Log.d(TAG, "Количество найденных статей: " + articles.length());

                            if (articles.length() > 0) {
                                // Проверяем, есть ли статьи без классификации
                                boolean needsClassification = false;
                                for (int i = 0; i < articles.length(); i++) {
                                    JSONObject article = articles.getJSONObject(i);
                                    Log.d(TAG, "Проверка статьи " + (i + 1) + ": topic_id=" + article.getInt("topic_id"));
                                    
                                    // Безопасная проверка summarized_text
                                    boolean hasSummarizedText = !article.isNull("summarized_text") && 
                                                              !article.getString("summarized_text").isEmpty();
                                    
                                    if (article.getInt("topic_id") == 0 && !hasSummarizedText) {
                                        needsClassification = true;
                                        Log.d(TAG, "Найдена статья, требующая классификации");
                                        break;
                                    }
                                }

                                if (needsClassification) {
                                    Log.d(TAG, "Запуск классификации статей");
                                    classifyRegionArticles(regionId, onSuccess);
                                } else {
                                    Log.d(TAG, "Все статьи уже классифицированы, продолжаем генерацию");
                                    onSuccess.run();
                                }
                            } else {
                                Log.d(TAG, "Статьи не найдены");
                                Toast.makeText(getContext(), "Нет новостей за выбранный период", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка обработки ответа: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Ошибка запроса: " + error.getMessage(), error);
                        if (error.networkResponse != null) {
                            try {
                                String errorBody = new String(error.networkResponse.data, "utf-8");
                                Log.e(TAG, "Тело ошибки: " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Ошибка чтения тела ошибки", e);
                            }
                        }
                        Toast.makeText(getContext(), "Ошибка запроса: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
            ) {
                @Override
                public String getBodyContentType() {
                    String contentType = "application/json; charset=utf-8";
                    Log.d(TAG, "Content-Type запроса: " + contentType);
                    return contentType;
                }

                @Override
                public byte[] getBody() {
                    try {
                        String bodyString = requestBody.toString();
                        Log.d(TAG, "Тело запроса перед отправкой: " + bodyString);
                        byte[] body = bodyString.getBytes("utf-8");
                        Log.d(TAG, "Тело запроса в байтах: " + new String(body, "UTF-8"));
                        return body;
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка создания тела запроса: " + e.getMessage(), e);
                        return null;
                    }
                }

                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("accept", "application/json; charset=utf-8");
                    Log.d(TAG, "Заголовки запроса: " + headers);
                    return headers;
                }
            };

            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    3,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            Log.d(TAG, "Отправка запроса на проверку новостей региона...");
            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка создания запроса: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка создания запроса: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void handleError(VolleyError error, String errorPrefix) {
        String errorMessage;
        if (error.networkResponse != null) {
            try {
                String responseBody = new String(error.networkResponse.data, "utf-8");
                Log.e(TAG, "Error response body: " + responseBody);
                Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);

                if (error.networkResponse.statusCode == 404) {
                    errorMessage = "Новости за выбранный период не найдены";
                } else if (error.networkResponse.statusCode == 405) {
                    errorMessage = "Метод не поддерживается. Пожалуйста, обновите приложение";
                } else {
                    JSONObject errorJson = new JSONObject(responseBody);
                    if (errorJson.has("detail")) {
                        errorMessage = errorJson.getString("detail");
                    } else {
                        errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing error response: " + e.getMessage(), e);
                errorMessage = "Ошибка сервера: " + error.networkResponse.statusCode;
            }
        } else {
            Log.e(TAG, "Network error: " + error.getMessage(), error);
            errorMessage = "Ошибка сети. Проверьте подключение к интернету";
        }
        Log.e(TAG, errorPrefix + ": " + errorMessage, error);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    private void classifyArticles(Runnable onSuccess) {
        String url;
        if (selectedRegionCode.equals("ekb")) {
            url = CLASSIFY_URL + "/classify/all";
        } else {
            url = CLASSIFY_URL + "/classify/regions";
        }
        Log.d(TAG, "Classification URL: " + url);
        progressDialog.setMessage("Классификация новостей...");
        progressDialog.show();

        try {
            JSONObject requestBody = new JSONObject();
            if (!selectedRegionCode.equals("ekb")) {
                requestBody.put("limit", 50); // Максимальное количество статей для классификации
            }
            Log.d(TAG, "Classification request body: " + requestBody.toString());

            StringRequest request = new StringRequest(
                    Request.Method.POST,
                    url,
                    response -> {
                        try {
                            // Декодируем ответ в UTF-8
                            String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            Log.d(TAG, "Classification response: " + decodedResponse);
                            JSONObject jsonResponse = new JSONObject(decodedResponse);
                            String message = jsonResponse.getString("message");
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            progressDialog.dismiss();
                            onSuccess.run();
                        } catch (Exception e) {
                            progressDialog.dismiss();
                            Log.e(TAG, "Error parsing classification response: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Ошибка при обработке ответа классификации", Toast.LENGTH_LONG).show();
                        }
                    },
                    error -> {
                        progressDialog.dismiss();
                        handleError(error, "Error during classification");
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("accept", "application/json");
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
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error creating classification request: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка при создании запроса классификации", Toast.LENGTH_LONG).show();
        }
    }

    private void getRegionId(String regionCode, OnRegionIdReceivedListener listener) {
        String url = REGIONS_API_URL + "/regions/";
        Log.d(TAG, "Fetching regions list from: " + url);
        Log.d(TAG, "Sending regions list request...");

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray regions = new JSONArray(response);
                        Log.d(TAG, "Received " + regions.length() + " regions");

                        for (int i = 0; i < regions.length(); i++) {
                            JSONObject region = regions.getJSONObject(i);
                            String code = region.getString("code");
                            int id = region.getInt("id");
                            Log.d(TAG, "Checking region " + (i + 1) + ": code=" + code + ", id=" + id);

                            if (code.equals(regionCode)) {
                                Log.d(TAG, "Found matching region ID: " + id);
                                listener.onRegionIdReceived(String.valueOf(id));
                                return;
                            }
                        }
                        Log.e(TAG, "Region not found for code: " + regionCode);
                        Toast.makeText(getContext(), "Регион не найден", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing regions response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка получения списка регионов", Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching regions: " + error.getMessage());
                    Toast.makeText(getContext(), "Ошибка получения списка регионов", Toast.LENGTH_LONG).show();
                }
        );

        requestQueue.add(request);
    }

    private interface OnRegionIdReceivedListener {
        void onRegionIdReceived(String regionId);
    }

    private void onGenerateClicked() {
        if (!validateInputs()) {
            return;
        }

        progressDialog.setMessage("Проверка наличия новостей...");
        progressDialog.show();

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

        if (selectedRegionCode.equals("ekb")) {
            checkNewsAvailability(() -> {
                int themeId = getThemeId(spinnerTheme.getText().toString());
                proceedWithGeneration(themeId);
            });
        } else {
            Log.d(TAG, "Getting region ID for code: " + selectedRegionCode);
            getRegionId(selectedRegionCode, regionId -> {
                Log.d(TAG, "Got region ID: " + regionId + " for code: " + selectedRegionCode);
                checkRegionNewsAvailability(regionId, startDate, endDate, () -> {
                    int themeId = getThemeId(spinnerTheme.getText().toString());
                    proceedWithGeneration(themeId);
                });
            });
        }
    }

    private void proceedWithGeneration(int themeId) {
        try {
            // Отправляем запрос на генерацию
            String url;
            if (selectedRegionCode.equals("ekb")) {
                url = API_BASE_URL + "/api/fill_news/news/by-date/";
                Log.d(TAG, "Generate request - URL: " + url);
                progressDialog.setMessage("Получение новостей...");
                progressDialog.show();

                JSONObject requestBody = new JSONObject();
                requestBody.put("start_date", startDateEditText.getText().toString());
                requestBody.put("end_date", endDateEditText.getText().toString());
                Log.d(TAG, "Request body: " + requestBody.toString());

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

                                Log.d(TAG, "Generation parameters - Theme: " + themeId +
                                        ", Tone: " + tone +
                                        ", Length: " + length +
                                        ", Details: " + details +
                                        ", Networks: " + String.join(", ", socialNetworks));

                                // Передача данных в NewsListFragment
                                Bundle args = new Bundle();
                                args.putString("response", decodedResponse);
                                args.putString("theme_id", String.valueOf(themeId));
                                args.putString("tone", tone);
                                args.putString("length", length);
                                args.putString("details", details);
                                args.putStringArray("social_networks", socialNetworks);
                                args.putString("region_code", selectedRegionCode);

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
                            handleError(error, "Error during generation");
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
                        30000,
                        3,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                ));

                Log.d(TAG, "Sending generate request...");
                requestQueue.add(request);
            } else {
                getRegionId(selectedRegionCode, regionId -> {
                    if (regionId == null) {
                        Toast.makeText(getContext(), "Ошибка: регион не найден", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String regionUrl = String.format(REGIONS_API_URL + "/regions/%s/news/", regionId);

                    Log.d(TAG, "Generate request - URL: " + regionUrl);
                    progressDialog.setMessage("Получение новостей...");
                    progressDialog.show();

                    try {
                        JSONObject requestBody = new JSONObject();
                        requestBody.put("topic_id", themeId);
                        requestBody.put("limit", 50); // Получаем до 50 новостей
                        Log.d(TAG, "Request body: " + requestBody.toString());

                        JsonArrayRequest request = new JsonArrayRequest(
                                Request.Method.POST,
                                regionUrl,
                                null, // тело запроса будет установлено в getBody()
                                response -> {
                                    try {
                                        String decodedResponse = response.toString();
                                        Log.i(TAG, "Response: " + decodedResponse);

                                        // Получаем выбранные параметры
                                        String tone = spinnerTone.getText().toString();
                                        String length = getSelectedLength();
                                        String details = etDetails.getText().toString();
                                        String[] socialNetworks = getSelectedSocialNetworks();

                                        Log.d(TAG, "Generation parameters - Theme: " + themeId +
                                                ", Tone: " + tone +
                                                ", Length: " + length +
                                                ", Details: " + details +
                                                ", Networks: " + String.join(", ", socialNetworks));

                                        // Передача данных в NewsListFragment
                                        Bundle args = new Bundle();
                                        args.putString("response", decodedResponse);
                                        args.putString("theme_id", String.valueOf(themeId));
                                        args.putString("tone", tone);
                                        args.putString("length", length);
                                        args.putString("details", details);
                                        args.putStringArray("social_networks", socialNetworks);
                                        args.putString("region_code", selectedRegionCode);
                                        args.putString("region_id", regionId);

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
                                    handleError(error, "Error during generation");
                                }
                        ) {
                            @Override
                            public Map<String, String> getHeaders() {
                                Map<String, String> headers = new HashMap<>();
                                headers.put("accept", "application/json; charset=utf-8");
                                Log.d(TAG, "Request headers: " + headers);
                                return headers;
                            }

                            @Override
                            public byte[] getBody() {
                                try {
                                    return requestBody.toString().getBytes("utf-8");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error creating request body", e);
                                    return null;
                                }
                            }

                            @Override
                            public String getBodyContentType() {
                                return "application/json; charset=utf-8";
                            }
                        };

                        request.setRetryPolicy(new DefaultRetryPolicy(
                                30000,
                                3,
                                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                        ));

                        Log.d(TAG, "Sending generate request...");
                        requestQueue.add(request);
                    } catch (Exception e) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Error creating request: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка создания запроса", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error in proceedWithGeneration: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка при генерации: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

        if (regionSpinner.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), "Выберите регион", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showAddRegionDialog() {
        showRegionDialog("", "", -1);
    }

    private void showRegionDialog(String regionName, String regionCode, int regionId) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_region, null);
        
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogDescription = dialogView.findViewById(R.id.tvDialogDescription);
        EditText etRegionName = dialogView.findViewById(R.id.etRegionName);
        EditText etRegionCode = dialogView.findViewById(R.id.etRegionCode);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Настраиваем диалог в зависимости от режима (добавление/редактирование)
        boolean isEditMode = regionId != -1;
        tvDialogTitle.setText(isEditMode ? "Редактирование региона" : "Добавление региона");
        tvDialogDescription.setText(isEditMode ? 
            "Измените название региона и его код" : 
            "Введите название региона и его код (латинскими буквами). Код будет использоваться для парсинга новостей.");
        
        if (isEditMode) {
            etRegionName.setText(regionName);
            etRegionCode.setText(regionCode);
            btnDelete.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnSave.setOnClickListener(v -> {
            String name = etRegionName.getText().toString().trim();
            String code = etRegionCode.getText().toString().trim().toLowerCase();

            if (name.isEmpty() || code.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                updateRegion(regionId, name, code);
            } else {
                createNewRegion(name, code);
            }
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            showDeleteConfirmationDialog(regionId, dialog);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDeleteConfirmationDialog(int regionId, AlertDialog parentDialog) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.LightDialogTheme)
                .setTitle("Удаление региона")
                .setMessage("Вы уверены, что хотите удалить этот регион? Все связанные новости также будут удалены.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteRegion(regionId);
                    parentDialog.dismiss();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateRegion(int regionId, String name, String code) {
        String url = REGIONS_API_URL + "/regions/" + regionId;
        Log.d(TAG, "Updating region - ID: " + regionId + ", Name: " + name + ", Code: " + code);
        progressDialog.setMessage("Обновление региона...");
        progressDialog.show();

        StringRequest request = new StringRequest(
                Request.Method.PUT,
                url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.i(TAG, "Region update response: " + decodedResponse);
                        setupRegionSpinner();
                        Toast.makeText(getContext(), "Регион успешно обновлен", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка при обработке ответа", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    handleError(error, "Ошибка при обновлении региона");
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("name", name);
                    jsonBody.put("code", code);
                    return jsonBody.toString().getBytes("utf-8");
                } catch (Exception e) {
                    Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
                    return null;
                }
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void deleteRegion(int regionId) {
        String url = REGIONS_API_URL + "/regions/" + regionId;
        Log.d(TAG, "Deleting region - ID: " + regionId);
        progressDialog.setMessage("Удаление региона...");
        progressDialog.show();

        StringRequest request = new StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    progressDialog.dismiss();
                    setupRegionSpinner();
                    Toast.makeText(getContext(), "Регион успешно удален", Toast.LENGTH_SHORT).show();
                },
                error -> {
                    progressDialog.dismiss();
                    handleError(error, "Ошибка при удалении региона");
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    private void createNewRegion(String name, String code) {
        String baseUrl = REGIONS_API_URL + "/regions/";
        String url = String.format("%s?name=%s&code=%s", baseUrl, name, code);
        Log.d(TAG, "Creating new region - Name: " + name + ", Code: " + code);
        Log.d(TAG, "Request URL: " + url);
        progressDialog.setMessage("Создание нового региона...");
        progressDialog.show();

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        // Декодируем ответ в UTF-8
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.i(TAG, "Region creation response: " + decodedResponse);

                        JSONObject jsonResponse = new JSONObject(decodedResponse);
                        // Получаем данные нового региона из ответа
                        int regionId = jsonResponse.getInt("id");
                        String newRegionName = jsonResponse.getString("name");
                        String newRegionCode = jsonResponse.getString("code");

                        Log.i(TAG, "Region created successfully - ID: " + regionId +
                                ", Name: " + newRegionName + ", Code: " + newRegionCode);

                        // Обновляем список регионов
                        setupRegionSpinner();

                        // Выбираем новый регион
                        regionSpinner.setText(newRegionName, false);
                        selectedRegionCode = newRegionCode;

                        // Запускаем парсинг для нового региона
                        startRegionParsing(regionId);

                        Toast.makeText(getContext(), "Регион успешно добавлен", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка при обработке ответа", Toast.LENGTH_SHORT).show();
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
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Sending create region request...");
        requestQueue.add(request);
    }

    private void startRegionParsing(int regionId) {
        // Получаем текущую дату и дату неделю назад
        Calendar endDate = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, -7);

        String formattedStartDate = formatDateForParsing(dateFormatter.format(startDate.getTime()));
        String formattedEndDate = formatDateForParsing(dateFormatter.format(endDate.getTime()));

        String url = String.format(REGIONS_API_URL + "/regions/%d/parse_period/?start_date=%s&end_date=%s",
                regionId, formattedStartDate, formattedEndDate);
        Log.d(TAG, "Starting region parsing - Region ID: " + regionId);
        Log.d(TAG, "Request URL: " + url);
        Log.d(TAG, "Formatted dates - Start: " + formattedStartDate + ", End: " + formattedEndDate);

        progressDialog.setMessage("Запуск парсинга для нового региона...");
        progressDialog.show();

        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                response -> {
                    progressDialog.dismiss();
                    try {
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.i(TAG, "Region parsing started successfully - Response: " + decodedResponse);
                        JSONObject jsonResponse = new JSONObject(decodedResponse);
                        String message = jsonResponse.getString("message");
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка при обработке ответа", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressDialog.dismiss();
                    String errorMessage;
                    if (error.networkResponse != null) {
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Parse error response body: " + responseBody);
                            Log.e(TAG, "Parse error status code: " + error.networkResponse.statusCode);
                            JSONObject errorJson = new JSONObject(responseBody);
                            if (errorJson.has("detail")) {
                                errorMessage = errorJson.getString("detail");
                                Log.e(TAG, "Parse error detail: " + errorMessage);
                            } else {
                                errorMessage = "Ошибка запуска парсинга: " + error.networkResponse.statusCode;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing error response: " + e.getMessage(), e);
                            errorMessage = "Ошибка запуска парсинга: " + error.networkResponse.statusCode;
                        }
                    } else {
                        Log.e(TAG, "Network error during parsing: " + error.getMessage(), error);
                        errorMessage = "Ошибка сети при запуске парсинга";
                    }
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                Log.d(TAG, "Parse request headers: " + headers);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                300000, // 5 минут таймаут
                3,      // 3 попытки
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "Sending start parsing request...");
        requestQueue.add(request);
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

    private String getTopicName(int topicId) {
        switch (topicId) {
            case 1: return "Изменения в законодательстве";
            case 2: return "Финансы";
            case 3: return "Строительные проекты и застройщики";
            case 4: return "ЖКХ";
            case 5: return "Ремонт";
            case 6: return "Дизайн";
            default: return "Неизвестная тема";
        }
    }

    private String getSourceName(int sourceId) {
        switch (sourceId) {
            case 1: return "Новостные сайты";
            case 2: return "Социальные сети";
            case 3: return "Официальные источники";
            default: return "Неизвестный источник";
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private String getStartDate() {
        if (periodRadioGroup.getCheckedRadioButtonId() == R.id.customRadio) {
            if (startDateEditText.getText().toString().isEmpty()) {
                return null;
            }
            return startDateEditText.getText().toString();
        }
        return startDateEditText.getText().toString();
    }

    private String getEndDate() {
        if (periodRadioGroup.getCheckedRadioButtonId() == R.id.customRadio) {
            if (endDateEditText.getText().toString().isEmpty()) {
                return null;
            }
            return endDateEditText.getText().toString();
        }
        return endDateEditText.getText().toString();
    }

    private void showEditRegionDialog(String regionName, String regionCode, int regionId) {
        showRegionDialog(regionName, regionCode, regionId);
    }

    private String formatDateForRegionRequest(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(date));
        } catch (ParseException e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage(), e);
            return date;
        }
    }
}