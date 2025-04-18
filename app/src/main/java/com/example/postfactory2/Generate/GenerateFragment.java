package com.example.postfactory2.Generate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.DefaultRetryPolicy;

import java.util.HashMap;
import java.util.Map;

public class GenerateFragment extends Fragment {

    private Spinner spinnerTheme, spinnerTone;
    private EditText etNewsCount, etDetails;
    private Button btnGenerate;
    private RecyclerView rvSocialNetworks;

    private static final String TAG = "GenerateFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        // Инициализация элементов
        spinnerTheme = view.findViewById(R.id.spinerTheme);
        etNewsCount = view.findViewById(R.id.etNewsCount);
        etDetails = view.findViewById(R.id.etDetails);
        btnGenerate = view.findViewById(R.id.btnGenerate);
        spinnerTone = view.findViewById(R.id.spinnerTone);
        rvSocialNetworks = view.findViewById(R.id.rvSocialNetworks);

        // Настройка элементов
        setupSpinners();
        setupRecyclerView();

        // Обработка нажатия кнопки "Сгенерировать"
        btnGenerate.setOnClickListener(v -> onGenerateClicked());

        return view;
    }

    private void setupSpinners() {
        List<String> themes = new ArrayList<>();
        themes.add("Новости рынка недвижимости");
        themes.add("Изменения в законодательстве");
        themes.add("Финансы");
        themes.add("Строительные проекты и застройщики");

        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, themes);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);

        List<String> tones = new ArrayList<>();
        tones.add("Информативный");
        tones.add("Продающий");
        tones.add("Экспертный");
        tones.add("Дружелюбный");

        ArrayAdapter<String> toneAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, tones);
        toneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTone.setAdapter(toneAdapter);
    }

    private void setupRecyclerView() {
        rvSocialNetworks.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> socialNetworks = new ArrayList<>();
        socialNetworks.add("VK");
        socialNetworks.add("Telegram");

        SocialNetworkAdapter adapter = new SocialNetworkAdapter(socialNetworks);
        rvSocialNetworks.setAdapter(adapter);
    }
    private void onGenerateClicked() {
        try {
            // Проверка темы
            String theme = spinnerTheme.getSelectedItem() != null ? spinnerTheme.getSelectedItem().toString() : "";
            Log.d(TAG, "Selected theme: " + theme);

            int themeId;
            switch (theme) {
                case "Новости рынка недвижимости":
                    themeId = 1;
                    break;
                case "Изменения в законодательстве":
                    themeId = 2;
                    break;
                case "Финансы":
                    themeId = 3;
                    break;
                case "Строительные проекты и застройщики":
                    themeId = 4;
                    break;
                default:
                    Log.e(TAG, "Unsupported theme selected.");
                    Toast.makeText(getContext(), "Данная тема пока не поддерживается.", Toast.LENGTH_LONG).show();
                    return;
            }

            // Проверка количества постов
            String newsCountStr = etNewsCount.getText().toString();
            Log.d(TAG, "News count input: " + newsCountStr);

            int newsCount;
            if (newsCountStr.isEmpty() || (newsCount = Integer.parseInt(newsCountStr)) <= 0) {
                Log.e(TAG, "Invalid news count: " + newsCountStr);
                Toast.makeText(getContext(), "Введите корректное количество новостей.", Toast.LENGTH_LONG).show();
                return;
            }

            // Отправка POST-запроса
            sendPostRequest(themeId, newsCount);

        } catch (Exception e) {
            Log.e(TAG, "Error in onGenerateClicked: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendPostRequest(int themeId, int newsCount) {
        String url = "http://2.59.40.125:8000/api/fill_news/news/";
        Log.i(TAG, "Request URL: " + url);

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("topic_id", themeId);
            requestBody.put("limit", newsCount);
            Log.i(TAG, "Request body: " + requestBody.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating request body: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка подготовки запроса", Toast.LENGTH_SHORT).show();
            return;
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        // Преобразование ответа в правильную кодировку
                        String decodedResponse = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                        Log.i(TAG, "Decoded response: " + decodedResponse);

                        // Получаем выбранные параметры
                        String tone = spinnerTone.getSelectedItem().toString();
                        String length = getSelectedLength();
                        String details = etDetails.getText().toString();
                        String[] socialNetworks = getSelectedSocialNetworks();

                        // Передача данных в NewsListFragment
                        Bundle args = new Bundle();
                        args.putString("response", decodedResponse);
                        args.putString("theme_id", String.valueOf(themeId));
                        args.putString("news_count", String.valueOf(newsCount));
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
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing response: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error during request: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Status code: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Response body: " + responseBody);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error response: " + e.getMessage());
                        }
                    }
                    Toast.makeText(getContext(), "Ошибка сервера. Попробуйте позже.", Toast.LENGTH_LONG).show();
                }) {
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

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        // Увеличиваем время ожидания и количество попыток
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                30000, // Время ожидания (30 секунд)
                3,     // Количество попыток
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        // Добавляем запрос в очередь
        requestQueue.add(stringRequest);
        Log.i(TAG, "Request added to queue.");
    }

    private String getSelectedLength() {
        RadioGroup rgPostLength = requireView().findViewById(R.id.rgPostLength);
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
        RecyclerView rvSocialNetworks = requireView().findViewById(R.id.rvSocialNetworks);
        SocialNetworkAdapter adapter = (SocialNetworkAdapter) rvSocialNetworks.getAdapter();
        return adapter.getSelectedNetworks();
    }
}
