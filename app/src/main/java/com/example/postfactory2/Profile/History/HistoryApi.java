package com.example.postfactory2.Profile.History;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.Auth.LoginActivity;
import com.example.postfactory2.Auth.TokenManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryApi {
    private static final String TAG = "HistoryApi";
    private static final String BASE_URL = "http://2.59.40.125:8000/api";
    
    public static void getUserGenerations(Context context, final HistoryCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = BASE_URL + "/generations/history";
        
        // Получаем токен через TokenManager
        TokenManager tokenManager = TokenManager.getInstance(context);
        
        // Проверяем, вошел ли пользователь и действителен ли токен
        if (!tokenManager.isLoggedIn()) {
            Log.e(TAG, "Пользователь не авторизован");
            callback.onError("Пользователь не авторизован. Выполните вход.");
            return;
        }
        
        // Проверяем, не истек ли токен
        if (tokenManager.isTokenExpired()) {
            Log.e(TAG, "Токен истек, пытаемся обновить");
            // Пытаемся обновить токен
            tokenManager.refreshToken(new TokenManager.TokenRefreshCallback() {
                @Override
                public void onTokenRefreshed() {
                    Log.d(TAG, "Токен успешно обновлен, повторяем запрос");
                    // Повторный запрос после обновления токена
                    getUserGenerations(context, callback);
                }

                @Override
                public void onTokenRefreshFailed(String errorMessage) {
                    Log.e(TAG, "Не удалось обновить токен: " + errorMessage);
                    callback.onError("Срок действия сессии истек. Выполните повторный вход.");
                }
            });
            return;
        }
        
        final String token = tokenManager.getToken();
        Log.d(TAG, "Запрос истории генераций, токен: " + (token.isEmpty() ? "пустой" : "имеется"));
        
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.d(TAG, "Получен ответ от сервера: " + response.toString());
                    List<Post> posts = new ArrayList<>();
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject postJson = response.getJSONObject(i);
                            
                            String title = postJson.getString("title");
                            String content = postJson.getString("content");
                            
                            // Форматируем дату
                            String dateStr = postJson.getString("generation_date");
                            SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));
                            Date date = serverFormat.parse(dateStr);
                            String formattedDate = displayFormat.format(date);
                            
                            // Определяем статус публикации
                            boolean published = postJson.getBoolean("published");
                            String status = published ? "Опубликовано" : "Не опубликовано";
                            
                            // Создаем объект поста и добавляем в список
                            Post post = new Post(title, content, formattedDate, status);
                            post.setId(postJson.getInt("id"));
                            if (published && !postJson.isNull("social_network_url")) {
                                post.setSocialNetworkUrl(postJson.getString("social_network_url"));
                            }
                            
                            posts.add(post);
                        }
                        callback.onSuccess(posts);
                    } catch (JSONException | ParseException e) {
                        Log.e(TAG, "Ошибка при обработке данных: " + e.getMessage());
                        callback.onError("Ошибка при обработке данных: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Ошибка сети: " + error.toString());
                    
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        Log.e(TAG, "Код ответа: " + statusCode);
                        Log.e(TAG, "Данные ответа: " + new String(error.networkResponse.data));
                        
                        // Проверяем, не истек ли токен (401 Unauthorized)
                        if (statusCode == 401) {
                            Log.e(TAG, "Токен недействителен (401), пытаемся обновить");
                            tokenManager.refreshToken(new TokenManager.TokenRefreshCallback() {
                                @Override
                                public void onTokenRefreshed() {
                                    Log.d(TAG, "Токен успешно обновлен, повторяем запрос");
                                    // Повторный запрос после обновления токена
                                    getUserGenerations(context, callback);
                                }

                                @Override
                                public void onTokenRefreshFailed(String errorMessage) {
                                    Log.e(TAG, "Не удалось обновить токен: " + errorMessage);
                                    callback.onError("Срок действия сессии истек. Выполните повторный вход.");
                                }
                            });
                            return;
                        }
                    }
                    
                    callback.onError("Ошибка сети: " + error.getMessage());
                }) {
            
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
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
    }
    
    public interface HistoryCallback {
        void onSuccess(List<Post> posts);
        void onError(String errorMessage);
    }

    public static void deleteGeneration(Context context, int generationId, final DeleteCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = BASE_URL + "/generations/" + generationId;
        
        // Получаем токен через TokenManager
        TokenManager tokenManager = TokenManager.getInstance(context);
        
        // Проверяем, вошел ли пользователь и действителен ли токен
        if (!tokenManager.isLoggedIn()) {
            Log.e(TAG, "Пользователь не авторизован");
            callback.onError("Пользователь не авторизован. Выполните вход.");
            return;
        }
        
        final String token = tokenManager.getToken();
        Log.d(TAG, "Запрос на удаление генерации ID: " + generationId);
        
        com.android.volley.toolbox.StringRequest request = new com.android.volley.toolbox.StringRequest(
                Request.Method.DELETE,
                url,
                response -> {
                    Log.d(TAG, "Генерация успешно удалена");
                    callback.onSuccess();
                },
                error -> {
                    Log.e(TAG, "Ошибка при удалении: " + error.toString());
                    
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        Log.e(TAG, "Код ответа: " + statusCode);
                        
                        if (statusCode == 401) {
                            // Токен истек, пробуем обновить
                            tokenManager.refreshToken(new TokenManager.TokenRefreshCallback() {
                                @Override
                                public void onTokenRefreshed() {
                                    deleteGeneration(context, generationId, callback);
                                }

                                @Override
                                public void onTokenRefreshFailed(String errorMessage) {
                                    callback.onError("Срок действия сессии истек. Выполните повторный вход.");
                                }
                            });
                            return;
                        }
                    }
                    
                    callback.onError("Ошибка при удалении: " + error.getMessage());
                }) {
            
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        
        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        
        queue.add(request);
    }
    
    public interface DeleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }
} 