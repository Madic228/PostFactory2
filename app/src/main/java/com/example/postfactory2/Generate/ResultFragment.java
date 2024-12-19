package com.example.postfactory2.Generate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.postfactory2.R;

public class ResultFragment extends Fragment {

    private TextView tvGeneratedPost;
    private Button btnPublish, btnRegenerate, btnCopy, btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result, container, false);

        tvGeneratedPost = view.findViewById(R.id.tvGeneratedPost);
        btnPublish = view.findViewById(R.id.btnPublish);
        btnRegenerate = view.findViewById(R.id.btnRegenerate);
        btnCopy = view.findViewById(R.id.btnCopy);
        btnBack = view.findViewById(R.id.btnBack);



        // Получение текста из аргументов
        if (getArguments() != null) {
            String generatedPost = getArguments().getString("generated_post", "Текст не получен");
            tvGeneratedPost.setText(generatedPost);
        }

        // Обработка кнопок
        btnPublish.setOnClickListener(v -> Toast.makeText(getContext(), "Публикация поста!", Toast.LENGTH_SHORT).show());
        btnRegenerate.setOnClickListener(v -> Toast.makeText(getContext(), "Перегенерация поста!", Toast.LENGTH_SHORT).show());
        btnCopy.setOnClickListener(v -> copyToClipboard(tvGeneratedPost.getText().toString()));
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Generated Post", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(getContext(), "Текст скопирован в буфер обмена", Toast.LENGTH_SHORT).show();
    }
}
