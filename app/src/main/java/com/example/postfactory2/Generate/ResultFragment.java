package com.example.postfactory2.Generate;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.postfactory2.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ResultFragment extends Fragment {

    private TextView tvPostTheme;
    private EditText etGeneratedPost;
    private ImageButton btnBackArrow, btnShare, btnCopy, btnRegenerate;
    private FloatingActionButton btnEdit;
    private boolean isEditable = false;
    private TextView tvPublicationDate, tvSourceLink;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        // Инициализация элементов
        tvPostTheme = view.findViewById(R.id.tvPostTheme);
        etGeneratedPost = view.findViewById(R.id.etGeneratedPost);
        btnBackArrow = view.findViewById(R.id.btnBackArrow);
        btnShare = view.findViewById(R.id.btnShare);
        btnCopy = view.findViewById(R.id.btnCopy);
        btnRegenerate = view.findViewById(R.id.btnRegenerate);
        btnEdit = view.findViewById(R.id.btnEdit);
        tvPublicationDate = view.findViewById(R.id.tvPublicationDate);
        tvSourceLink = view.findViewById(R.id.tvSourceLink);


        // Получение данных из аргументов
        if (getArguments() != null) {
            String postTheme = getArguments().getString("post_theme", "Тема не указана");
            String publicationDate = getArguments().getString("publication_date", "Дата не указана");
            String source = getArguments().getString("source", "Источник не указан");
            String summarizedText = getArguments().getString("summarized_text", "Текст не получен");
            String link = getArguments().getString("link", "");

            // Устанавливаем данные
            tvPostTheme.setText(postTheme);
            tvPublicationDate.setText("Дата публикации: " + publicationDate);
            etGeneratedPost.setText(summarizedText);
            tvSourceLink.setText(link); // Ссылка добавляется в TextView
        } else {
            tvPostTheme.setText("Тема не указана");
            tvPublicationDate.setText("Дата не указана");
            etGeneratedPost.setText("Нет данных для отображения.");
            tvSourceLink.setText("");
        }


        // Назад
        btnBackArrow.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Поделиться
        btnShare.setOnClickListener(v -> {
            String shareText = etGeneratedPost.getText().toString();

            // Create an intent with the action set to share
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            // Set the text type for the intent
            shareIntent.setType("text/plain");

            // Add the text to be shared as an extra
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

            // Create a chooser intent to let the user choose the app to share with
            Intent chooserIntent = Intent.createChooser(shareIntent, "Share Post");

            // Start the chooser activity
            requireActivity().startActivity(chooserIntent);
        });
        // Скопировать
        btnCopy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Generated Post", etGeneratedPost.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Текст скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
        });

        // Перегенерация
        btnRegenerate.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Перегенерация поста", Toast.LENGTH_SHORT).show();

            // Замена текущего фрагмента на фрагмент генерации
            GenerateFragment generateFragment = new GenerateFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, generateFragment) // Указываем контейнер для фрагментов
                    .addToBackStack(null) // Добавляем в стек, если нужно
                    .commit();
        });

        // Редактирование текста
        btnEdit.setOnClickListener(v -> toggleEditMode());

        return view;
    }


    private void toggleEditMode() {
        isEditable = !isEditable;
        etGeneratedPost.setFocusable(isEditable);
        etGeneratedPost.setFocusableInTouchMode(isEditable);
        etGeneratedPost.setCursorVisible(isEditable);

        if (isEditable) {
            btnEdit.setImageResource(R.drawable.ic_save); // Заменить значок на "Сохранить"
            Toast.makeText(getContext(), "Режим редактирования включен", Toast.LENGTH_SHORT).show();
        } else {
            btnEdit.setImageResource(R.drawable.ic_edit); // Заменить значок на "Редактировать"
            Toast.makeText(getContext(), "Изменения сохранены", Toast.LENGTH_SHORT).show();
        }
    }
}
