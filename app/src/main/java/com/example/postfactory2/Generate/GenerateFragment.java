package com.example.postfactory2.Generate;

import android.os.Bundle;
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

import com.example.postfactory2.R;

import java.util.ArrayList;
import java.util.List;

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
        spinnerTheme = view.findViewById(R.id.spinnerTheme);
        spinnerTone = view.findViewById(R.id.spinnerTone);
        etDetails = view.findViewById(R.id.etDetails);
        rgPostLength = view.findViewById(R.id.rgPostLength);
        rvSocialNetworks = view.findViewById(R.id.rvSocialNetworks);
        btnGenerate = view.findViewById(R.id.btnGenerate);

        // Заглушка: заполнение Spinner
        setupSpinners();

        // Настройка RecyclerView для выбора соцсетей
        setupRecyclerView();

        // Обработка кнопки "Сгенерировать"
        btnGenerate.setOnClickListener(v -> onGenerateClicked());

        return view;
    }

    private void setupSpinners() {
        // Пример данных для Spinner
        List<String> themes = new ArrayList<>();
        themes.add("Маркетинг");
        themes.add("Технологии");
        themes.add("Путешествия");
        themes.add("Еда");
        themes.add("Мода");

        List<String> tones = new ArrayList<>();
        tones.add("Дружеский");
        tones.add("Официальный");

        // Адаптер для Spinner темы
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, themes);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(themeAdapter);

        // Адаптер для Spinner тональности
        ArrayAdapter<String> toneAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, tones);
        toneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTone.setAdapter(toneAdapter);
    }

    private void setupRecyclerView() {
        rvSocialNetworks.setLayoutManager(new LinearLayoutManager(getContext()));

        List<String> socialNetworks = new ArrayList<>();
        socialNetworks.add("Instagram");
        socialNetworks.add("Facebook");
        socialNetworks.add("VK");
        socialNetworks.add("Twitter");

        SocialNetworkAdapter adapter = new SocialNetworkAdapter(socialNetworks);
        rvSocialNetworks.setAdapter(adapter);
    }


    private void onGenerateClicked() {
        try {
            String theme = spinnerTheme.getSelectedItem() != null ? spinnerTheme.getSelectedItem().toString() : "Не выбрано";
            String tone = spinnerTone.getSelectedItem() != null ? spinnerTone.getSelectedItem().toString() : "Не выбрано";
            String details = etDetails.getText().toString();

            int selectedLengthId = rgPostLength.getCheckedRadioButtonId();
            String postLength;
            if (selectedLengthId != -1) {
                RadioButton selectedLength = rgPostLength.findViewById(selectedLengthId);
                postLength = selectedLength.getText().toString();
            } else {
                postLength = "Не выбрано";
            }

            List<String> selectedNetworks = ((SocialNetworkAdapter) rvSocialNetworks.getAdapter()).getSelectedNetworks();

            Toast.makeText(getContext(), "Генерация поста:\n" +
                            "Тема: " + theme + "\n" +
                            "Тональность: " + tone + "\n" +
                            "Длина: " + postLength + "\n" +
                            "Детали: " + details + "\n" +
                            "Социальные сети: " + selectedNetworks,
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



}
