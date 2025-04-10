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
import android.app.ProgressDialog;

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
import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

import com.android.volley.DefaultRetryPolicy;
import com.example.postfactory2.Auth.TokenManager;

public class ResultFragment extends Fragment {
    private static final String TAG = "ResultFragment";
    private static final String TEXT_PROCESSOR_URL = "http://192.168.31.252:8000/text/process";
    private ProgressDialog progressDialog;

    private TextView tvPostTheme;
    private EditText etGeneratedPost;
    private ImageButton btnBackArrow, btnShare, btnCopy, btnRegenerate;
    private FloatingActionButton btnEdit;
    private boolean isEditable = false;
    private TextView tvPublicationDate, tvSourceLink;
    private String originalSummarizedText;

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
            String tone = getArguments().getString("tone", "Информативный");
            String length = getArguments().getString("length", "Средний");
            String details = getArguments().getString("details", "");
            String[] socialNetworks = getArguments().getStringArray("social_networks");

            // Сохраняем оригинальный текст
            originalSummarizedText = summarizedText;

            // Устанавливаем данные
            tvPostTheme.setText(postTheme);
            tvPublicationDate.setText("Дата публикации: " + publicationDate);
            tvSourceLink.setText(link);

            // Пытаемся обработать текст через API
            processText(summarizedText, tone, length, details, socialNetworks);
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

    private void processText(String text, String tone, String length, String details, String[] socialNetworks) {
        // Показываем прогресс-бар
        showProgressDialog();
        
        try {
            // Создаем JSON объект для запроса
            JSONObject requestBody = new JSONObject();
            requestBody.put("text", text);
            requestBody.put("tone", tone);
            requestBody.put("length", length);
            requestBody.put("details", details);
            
            // Проверяем, что socialNetworks не null
            if (socialNetworks != null && socialNetworks.length > 0) {
                requestBody.put("social_networks", new JSONArray(socialNetworks));
            } else {
                requestBody.put("social_networks", new JSONArray());
            }

            // Создаем запрос
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                TEXT_PROCESSOR_URL,
                requestBody,
                response -> {
                    try {
                        hideProgressDialog();
                        // Проверяем статус ответа
                        if (response.getString("status").equals("success")) {
                            JSONObject data = response.getJSONObject("data");
                            // Получаем текст для первой соцсети
                            String processedText = data.getString(socialNetworks[0]);
                            etGeneratedPost.setText(processedText);
                            
                            // Сохраняем в историю
                            saveGenerationToHistory(tvPostTheme.getText().toString(), processedText);
                        } else {
                            // Если статус не success, используем оригинальный текст
                            etGeneratedPost.setText(originalSummarizedText);
                            saveGenerationToHistory(tvPostTheme.getText().toString(), originalSummarizedText);
                        }
                    } catch (JSONException e) {
                        hideProgressDialog();
                        Log.e(TAG, "Ошибка при обработке ответа: " + e.getMessage());
                        etGeneratedPost.setText(originalSummarizedText);
                        saveGenerationToHistory(tvPostTheme.getText().toString(), originalSummarizedText);
                    }
                },
                error -> {
                    hideProgressDialog();
                    Log.e(TAG, "Ошибка при отправке запроса: " + error.getMessage());
                    // При ошибке используем оригинальный текст
                    etGeneratedPost.setText(originalSummarizedText);
                    saveGenerationToHistory(tvPostTheme.getText().toString(), originalSummarizedText);
                }
            );

            // Увеличиваем таймаут и количество попыток
            request.setRetryPolicy(new DefaultRetryPolicy(
                60000, // 60 секунд таймаут
                3,     // 3 попытки
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            // Добавляем запрос в очередь
            RequestQueue queue = Volley.newRequestQueue(requireContext());
            queue.add(request);

        } catch (JSONException e) {
            hideProgressDialog();
            Log.e(TAG, "Ошибка при создании запроса: " + e.getMessage());
            etGeneratedPost.setText(originalSummarizedText);
            saveGenerationToHistory(tvPostTheme.getText().toString(), originalSummarizedText);
        }
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
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Fragment not attached to context");
            return;
        }

        TokenManager tokenManager = TokenManager.getInstance(getContext());
        String token = tokenManager.getToken();
        
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Token is null or empty");
            return;
        }

        saveGenerationToHistoryWithToken(title, content, token);
    }
    
    // Вспомогательный метод для сохранения генерации с токеном
    private void saveGenerationToHistoryWithToken(String title, String content, String token) {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Fragment not attached to context");
            return;
        }

        // Проверка данных перед отправкой
        if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
            Log.e(TAG, "Заголовок или содержание пусты, нельзя сохранить в историю");
            return;
        }
        
        // Проверка токена
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "Токен пустой, нельзя сохранить в историю");
            return;
        }
        
        Log.d(TAG, "Пытаемся сохранить генерацию в историю: " + title);
        
        try {
            RequestQueue queue = Volley.newRequestQueue(getContext());
            String url = "http://2.59.40.125:8000/api/generations";
            
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("title", title);
            jsonBody.put("content", content);
            
            Log.d(TAG, "Отправляем данные на сервер: " + jsonBody.toString());
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        // Успешно сохранено
                        Log.d(TAG, "Успешно сохранено в историю: " + response.toString());
                        Toast.makeText(requireContext(), "Генерация сохранена в историю", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        // Ошибка сохранения
                        Log.e(TAG, "Ошибка при сохранении в историю: " + error.toString());
                        
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            Log.e(TAG, "Код ответа: " + statusCode);
                            Log.e(TAG, "Данные ответа: " + new String(error.networkResponse.data));
                            
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
            Log.e(TAG, "Ошибка JSON при сохранении в историю: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Непредвиденная ошибка при сохранении в историю: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Генерация текста...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}
