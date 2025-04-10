package com.example.postfactory2.Generate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import androidx.fragment.app.FragmentManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.Auth.LoginActivity;
import com.example.postfactory2.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.DefaultRetryPolicy;
import com.example.postfactory2.Auth.TokenManager;

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
            
            // Сохраняем генерацию в историю
            saveGenerationToHistory(postTheme, summarizedText);
        } else {
            tvPostTheme.setText("Тема не указана");
            tvPublicationDate.setText("Дата не указана");
            etGeneratedPost.setText("Нет данных для отображения.");
            tvSourceLink.setText("");
        }


        // Назад
        btnBackArrow.setOnClickListener(v -> {
            // Просто возвращаемся назад
            requireActivity().getSupportFragmentManager().popBackStack();
        });

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
            
            // Сохраняем изменения в историю
            String editedText = etGeneratedPost.getText().toString();
            String postTheme = tvPostTheme.getText().toString();
            saveGenerationToHistory(postTheme, editedText);
        }
    }
    
    // Метод для сохранения генерации в историю
    private void saveGenerationToHistory(String title, String content) {
        // Получаем токен через TokenManager
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        
        // Проверяем, вошел ли пользователь
        if (!tokenManager.isLoggedIn()) {
            Log.e("ResultFragment", "Пользователь не авторизован, нельзя сохранить в историю");
            return;
        }
        
        // Проверяем, не истек ли токен
        if (tokenManager.isTokenExpired()) {
            Log.e("ResultFragment", "Токен истек, пытаемся обновить");
            
            // Пытаемся обновить токен
            tokenManager.refreshToken(new TokenManager.TokenRefreshCallback() {
                @Override
                public void onTokenRefreshed() {
                    Log.d("ResultFragment", "Токен успешно обновлен, повторяем сохранение");
                    // Повторное сохранение после обновления токена
                    saveGenerationToHistoryWithToken(title, content, tokenManager.getToken());
                }

                @Override
                public void onTokenRefreshFailed(String errorMessage) {
                    Log.e("ResultFragment", "Не удалось обновить токен: " + errorMessage);
                    Toast.makeText(requireContext(), "Не удалось сохранить в историю: срок сессии истек", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        
        // Токен действителен, выполняем сохранение
        saveGenerationToHistoryWithToken(title, content, tokenManager.getToken());
    }
    
    // Вспомогательный метод для сохранения генерации с токеном
    private void saveGenerationToHistoryWithToken(String title, String content, String token) {
        // Проверка данных перед отправкой
        if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
            Log.e("ResultFragment", "Заголовок или содержание пусты, нельзя сохранить в историю");
            return;
        }
        
        // Проверка токена
        if (token == null || token.isEmpty()) {
            Log.e("ResultFragment", "Токен пустой, нельзя сохранить в историю");
            return;
        }
        
        Log.d("ResultFragment", "Пытаемся сохранить генерацию в историю: " + title);
        
        try {
            RequestQueue queue = Volley.newRequestQueue(requireContext());
            String url = "http://2.59.40.125:8000/api/generations";
            
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("title", title);
            jsonBody.put("content", content);
            
            Log.d("ResultFragment", "Отправляем данные на сервер: " + jsonBody.toString());
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        // Успешно сохранено
                        Log.d("ResultFragment", "Успешно сохранено в историю: " + response.toString());
                        Toast.makeText(requireContext(), "Генерация сохранена в историю", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        // Ошибка сохранения
                        Log.e("ResultFragment", "Ошибка при сохранении в историю: " + error.toString());
                        
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            Log.e("ResultFragment", "Код ответа: " + statusCode);
                            Log.e("ResultFragment", "Данные ответа: " + new String(error.networkResponse.data));
                            
                            // Проверяем, не истек ли токен (401 Unauthorized)
                            if (statusCode == 401) {
                                // Обновление токена и повторная попытка уже обрабатываются в основном методе
                                Toast.makeText(requireContext(), "Не удалось сохранить в историю: авторизация устарела", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        
                        Toast.makeText(requireContext(), "Не удалось сохранить в историю", Toast.LENGTH_SHORT).show();
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            
            // Устанавливаем политику повторов для запроса
            request.setRetryPolicy(new DefaultRetryPolicy(
                    30000, // 30 секунд тайм-аут
                    1, // Максимальное количество повторных попыток
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            
            queue.add(request);
        } catch (JSONException e) {
            Log.e("ResultFragment", "Ошибка JSON при сохранении в историю: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("ResultFragment", "Непредвиденная ошибка при сохранении в историю: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
