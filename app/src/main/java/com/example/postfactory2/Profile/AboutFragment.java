package com.example.postfactory2.Profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.postfactory2.R;

public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView descriptionText = view.findViewById(R.id.description_text);
        Button emailButton = view.findViewById(R.id.email_button);
        Button telegramButton = view.findViewById(R.id.telegram_button);

        descriptionText.setText("PostFactory - это инновационное мобильное приложение, разработанное для автоматизации создания контента в сфере недвижимости. " +
                "Приложение использует технологии искусственного интеллекта для генерации информационных постов по следующим темам:\n\n" +
                "• Изменения в законодательстве\n" +
                "• Финансы\n" +
                "• Строительные проекты и застройщики\n" +
                "• ЖКХ\n" +
                "• Ремонт\n" +
                "• Дизайн\n\n" +
                "Основные преимущества:\n" +
                "• Автоматизированный сбор и анализ данных\n" +
                "• Генерация качественного контента\n" +
                "• Удобная публикация в социальных сетях\n" +
                "• Экономия времени на создание постов\n" +
                "• Повышение эффективности маркетинговой деятельности");

        emailButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:your.email@example.com"));
            startActivity(intent);
        });

        telegramButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://t.me/your_telegram"));
            startActivity(intent);
        });

        return view;
    }
} 