package com.example.postfactory2.Generate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.example.postfactory2.R;

import java.util.ArrayList;
import java.util.List;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;

public class GenerateFragment extends Fragment {

    private Spinner spinnerTheme, spinnerTone;
    private EditText etDetails;
    private RadioGroup rgPostLength;
    private RecyclerView rvSocialNetworks;
    private Button btnGenerate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_generate, container, false);

        // Инициализация элементов
        spinnerTheme = view.findViewById(R.id.spinerTheme);
        spinnerTone = view.findViewById(R.id.spinnerTone);
        etDetails = view.findViewById(R.id.etDetails);
        rgPostLength = view.findViewById(R.id.rgPostLength);
        rvSocialNetworks = view.findViewById(R.id.rvSocialNetworks);
        btnGenerate = view.findViewById(R.id.btnGenerate);

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
        themes.add("Общие тенденции рынка");
        themes.add("Ремонт и DIY");
        themes.add("Дизайн");

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



    private static final String TAG = "GenerateFragment";

    private void onGenerateClicked() {
        try {
            String theme = spinnerTheme.getSelectedItem() != null ? spinnerTheme.getSelectedItem().toString() : "Не выбрано";
            String tone = spinnerTone.getSelectedItem() != null ? spinnerTone.getSelectedItem().toString() : "Не выбрано";
            String details = etDetails.getText().toString();

            Log.d(TAG, "Selected theme: " + theme);
            Log.d(TAG, "Selected tone: " + tone);
            Log.d(TAG, "Details: " + details);

            int selectedLengthId = rgPostLength.getCheckedRadioButtonId();
            String postLength;
            if (selectedLengthId != -1) {
                RadioButton selectedLength = rgPostLength.findViewById(selectedLengthId);
                postLength = selectedLength.getText().toString();
            } else {
                postLength = "Не выбрано";
            }

            Log.d(TAG, "Post length: " + postLength);

            List<String> selectedNetworks = ((SocialNetworkAdapter) rvSocialNetworks.getAdapter()).getSelectedNetworks();
            Log.d(TAG, "Selected networks: " + selectedNetworks);

            JSONObject requestBody = new JSONObject();
            requestBody.put("theme", theme);
            requestBody.put("tone", tone);
            requestBody.put("details", details);
            requestBody.put("length", postLength);
            requestBody.put("social_networks", new JSONArray(selectedNetworks));

            Log.i(TAG, "Request body: " + requestBody.toString());

            // Исправленный URL
            String url = "http://192.168.151.69 :8000/api/generate/"; // Замените x.x на ваш IP
            Log.i(TAG, "Request URL: " + url);

            RequestQueue requestQueue = Volley.newRequestQueue(requireContext());

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                    response -> {
                        Log.i(TAG, "Server response: " + response);

                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String summarizedText = jsonResponse.getString("summarized_text");
                            String generatedPost = jsonResponse.getString("post_text");
                            JSONObject userData = jsonResponse.getJSONObject("user_data");

                            // Объединяем все данные для отображения
                            StringBuilder combinedText = new StringBuilder();
                            combinedText.append("Тема: ").append(userData.getString("theme")).append("\n");
                            combinedText.append("Тон: ").append(userData.getString("tone")).append("\n");
                            combinedText.append("Детали: ").append(userData.getString("details")).append("\n");
                            combinedText.append("Длина поста: ").append(userData.getString("length")).append("\n");
                            combinedText.append("Социальные сети: ").append(userData.getJSONArray("social_networks").toString()).append("\n\n");
                            combinedText.append("Суммаризированный текст:\n").append(summarizedText).append("\n\n");
                            combinedText.append("Сгенерированный текст:\n").append(generatedPost);

                            Log.i(TAG, "Combined text for display: " + combinedText.toString());

                            ResultFragment resultFragment = new ResultFragment();
                            Bundle args = new Bundle();
                            args.putString("post_theme", userData.getString("theme"));  // Тема
                            args.putString("summarized_text", summarizedText);         // Суммаризированный текст
                            args.putString("generated_post", combinedText.toString()); // Весь текст
                            resultFragment.setArguments(args);

                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, resultFragment)
                                    .addToBackStack(null)
                                    .commit();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing response: " + e.getMessage(), e);
                            Toast.makeText(getContext(), "Ошибка обработки ответа: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    },
                    error -> {
                        Log.e(TAG, "Error during request: " + error.getMessage(), error);
                        Toast.makeText(getContext(), "Ошибка запроса: " + error.getMessage(), Toast.LENGTH_LONG).show();
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

            // Добавляем политику повторных попыток с увеличенным временем ожидания
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    180000, // Время ожидания (180 секунд)
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(stringRequest);
            Log.i(TAG, "Request added to queue");
        } catch (Exception e) {
            Log.e(TAG, "Error in onGenerateClicked: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }




    // Добавить после onGenerateClicked()
    private void navigateToResultFragment() {
        ResultFragment resultFragment = new ResultFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, resultFragment)
                .addToBackStack(null)
                .commit();
    }

}
