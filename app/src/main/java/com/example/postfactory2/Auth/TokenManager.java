package com.example.postfactory2.Auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер для управления JWT токенами.
 * Отвечает за хранение, проверку срока действия и обновление токена.
 */
public class TokenManager {
    private static final String TAG = "TokenManager";
    
    // Константы для SharedPreferences
    private static final String PREF_NAME = LoginActivity.PREF_NAME;
    private static final String KEY_TOKEN = LoginActivity.KEY_TOKEN;
    private static final String KEY_USERNAME = LoginActivity.KEY_USERNAME;
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_PASSWORD = "user_password";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry";
    private static final String KEY_IS_LOGGED_IN = LoginActivity.KEY_IS_LOGGED_IN;
    private static final String KEY_LAST_REFRESH_TIME = "last_refresh_time";
    
    // URL для обновления токена
    private static final String LOGIN_URL = "http://2.59.40.125:8000/api/auth/login";
    
    // Допустимый запас времени перед истечением срока (2 часа в миллисекундах)
    // Увеличим порог до 2 часов, чтобы уменьшить количество обновлений
    private static final long TOKEN_REFRESH_THRESHOLD = 2 * 60 * 60 * 1000;
    
    // Минимальный интервал между запросами на обновление (15 минут)
    private static final long MIN_REFRESH_INTERVAL = 15 * 60 * 1000;
    
    private final Context context;
    private final SharedPreferences preferences;
    
    // Singleton инстанс
    private static TokenManager instance;
    
    // Флаг, указывающий, что происходит обновление токена
    private boolean isRefreshing = false;
    
    /**
     * Получить инстанс TokenManager
     */
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Конструктор
     */
    private TokenManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Сохранить токен в SharedPreferences
     */
    public void saveToken(String token, String username) {
        Log.d(TAG, "Сохранение токена для пользователя: " + username);
        
        // Извлечь срок действия токена
        long expiryTime = extractExpiryTimeFromToken(token);
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_USERNAME, username);
        editor.putLong(KEY_TOKEN_EXPIRY, expiryTime);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        
        // Сохраняем время обновления токена
        editor.putLong(KEY_LAST_REFRESH_TIME, System.currentTimeMillis());
        
        editor.apply();
    }
    
    /**
     * Сохранить зашифрованный пароль (для автообновления токена)
     */
    public void savePassword(String password) {
        if (password == null || password.isEmpty()) {
            return;
        }
        
        // Зашифруйте пароль или используйте EncryptedSharedPreferences для большей безопасности
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
    }
    
    /**
     * Получить текущий токен
     */
    public String getToken() {
        return preferences.getString(KEY_TOKEN, "");
    }
    
    /**
     * Получить имя пользователя
     */
    public String getUsername() {
        return preferences.getString(KEY_USERNAME, "");
    }
    
    /**
     * Получить email пользователя
     */
    public String getEmail() {
        return preferences.getString(KEY_EMAIL, "");
    }
    
    /**
     * Проверить, вошел ли пользователь в систему
     */
    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false) && !getToken().isEmpty();
    }
    
    /**
     * Выход из системы - очистка всех данных
     */
    public void logout() {
        Log.d(TAG, "Очистка данных авторизации");
        
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_TOKEN);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_TOKEN_EXPIRY);
        editor.remove(KEY_LAST_REFRESH_TIME);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }
    
    /**
     * Проверить, нужно ли обновить токен
     */
    public boolean shouldRefreshToken() {
        // Если сейчас происходит обновление, не запускаем еще одно
        if (isRefreshing) {
            return false;
        }
        
        // Если пользователь не авторизован, не обновляем токен
        if (!isLoggedIn()) {
            return false;
        }
        
        String token = getToken();
        if (token.isEmpty()) {
            return false;
        }
        
        // Проверяем, не было ли недавнего обновления
        long lastRefreshTime = preferences.getLong(KEY_LAST_REFRESH_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        // Если обновление было недавно, не запускаем новое
        if ((currentTime - lastRefreshTime) < MIN_REFRESH_INTERVAL) {
            Log.d(TAG, "Токен был недавно обновлен, пропускаем проверку");
            return false;
        }
        
        // Проверяем срок действия токена
        long expiryTime = preferences.getLong(KEY_TOKEN_EXPIRY, 0);
        
        // Если срок действия не извлечен или неверен,
        // попробуем получить его из токена
        if (expiryTime <= 0) {
            expiryTime = extractExpiryTimeFromToken(token);
            
            // Если срок по-прежнему неверен, обновляем токен
            if (expiryTime <= 0) {
                return true;
            }
            
            // Сохраняем корректный срок
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(KEY_TOKEN_EXPIRY, expiryTime);
            editor.apply();
        }
        
        // Если токен уже истек, его нужно обновить
        if (currentTime >= expiryTime) {
            return true;
        }
        
        // Проверяем, истекает ли токен в ближайшее время (2 часа)
        boolean shouldRefresh = (expiryTime - currentTime) < TOKEN_REFRESH_THRESHOLD;
        
        if (shouldRefresh) {
            Log.d(TAG, "Токен истекает в ближайшее время, требуется обновление");
        } else {
            Log.d(TAG, "Токен действителен");
        }
        
        return shouldRefresh;
    }
    
    /**
     * Проверить, истек ли срок действия токена
     */
    public boolean isTokenExpired() {
        if (!isLoggedIn()) {
            return true;
        }
        
        String token = getToken();
        if (token.isEmpty()) {
            return true;
        }
        
        // Проверяем срок действия токена
        long expiryTime = preferences.getLong(KEY_TOKEN_EXPIRY, 0);
        long currentTime = System.currentTimeMillis();
        
        // Если срок действия не извлечен или неверен,
        // попробуем получить его из токена
        if (expiryTime <= 0) {
            expiryTime = extractExpiryTimeFromToken(token);
            
            // Если срок по-прежнему неверен, считаем токен истекшим
            if (expiryTime <= 0) {
                return true;
            }
            
            // Сохраняем корректный срок
            SharedPreferences.Editor editor = preferences.edit();
            editor.putLong(KEY_TOKEN_EXPIRY, expiryTime);
            editor.apply();
        }
        
        return currentTime >= expiryTime;
    }
    
    /**
     * Извлечь срок действия из JWT токена
     */
    private long extractExpiryTimeFromToken(String token) {
        try {
            // Получить payload часть JWT (часть между первой и второй точкой)
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                Log.e(TAG, "Неверный формат токена");
                return 0;
            }
            
            String payload = parts[1];
            // Добавляем отсутствующее дополнение, если необходимо
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE);
            String decodedPayload = new String(decodedBytes, StandardCharsets.UTF_8);
            
            JSONObject payloadJson = new JSONObject(decodedPayload);
            
            // Извлечь поле "exp" - время истечения в Unix-формате (секунды)
            if (payloadJson.has("exp")) {
                long expSeconds = payloadJson.getLong("exp");
                return expSeconds * 1000; // Конвертировать в миллисекунды
            } else {
                Log.e(TAG, "В токене отсутствует поле 'exp'");
                return 0;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при извлечении срока токена: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Попытка обновить токен
     */
    public synchronized void refreshToken(final TokenRefreshCallback callback) {
        // Если уже идет процесс обновления, не запускаем его снова
        if (isRefreshing) {
            Log.d(TAG, "Обновление токена уже выполняется");
            return;
        }
        
        String username = getUsername();
        String password = preferences.getString(KEY_PASSWORD, "");
        
        if (username.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "Не удается обновить токен: отсутствуют учетные данные");
            if (callback != null) {
                callback.onTokenRefreshFailed("Отсутствуют учетные данные для обновления");
            }
            return;
        }
        
        // Проверяем время последнего обновления
        long lastRefreshTime = preferences.getLong(KEY_LAST_REFRESH_TIME, 0);
        long currentTime = System.currentTimeMillis();
        
        // Если обновление было недавно, не запускаем новое
        if ((currentTime - lastRefreshTime) < MIN_REFRESH_INTERVAL) {
            Log.d(TAG, "Токен был недавно обновлен, пропускаем обновление");
            
            // Проверяем, не истек ли токен
            if (!isTokenExpired()) {
                if (callback != null) {
                    callback.onTokenRefreshed();
                }
                return;
            }
        }
        
        Log.d(TAG, "Попытка обновления токена для пользователя: " + username);
        isRefreshing = true;
        
        // Создаем очередь запросов Volley
        RequestQueue queue = Volley.newRequestQueue(context);
        
        // Подготавливаем тело запроса
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "password");
        params.put("username", username);
        params.put("password", password);
        params.put("scope", "");
        params.put("client_id", "string");
        params.put("client_secret", "string");
        
        // Создаем форм-данные для запроса
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(param.getKey());
            postData.append('=');
            postData.append(param.getValue());
        }
        
        String url = LOGIN_URL;
        
        // Создаем запрос
        StringRequest request = new StringRequest(Request.Method.POST, url,
            response -> {
                try {
                    // Обрабатываем успешный ответ
                    Log.d(TAG, "Получен ответ от сервера при обновлении токена");
                    
                    JSONObject jsonResponse = new JSONObject(response);
                    String newToken = jsonResponse.getString("access_token");
                    saveToken(newToken, username);
                    
                    Log.d(TAG, "Токен успешно обновлен");
                    isRefreshing = false;
                    
                    if (callback != null) {
                        callback.onTokenRefreshed();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Ошибка при обработке ответа: " + e.getMessage());
                    isRefreshing = false;
                    if (callback != null) {
                        callback.onTokenRefreshFailed("Неверный формат ответа");
                    }
                }
            },
            error -> {
                Log.e(TAG, "Ошибка при обновлении токена: " + error.toString());
                isRefreshing = false;
                if (callback != null) {
                    callback.onTokenRefreshFailed("Ошибка сети или неверные учетные данные");
                }
            }) {
            
            @Override
            public byte[] getBody() {
                return postData.toString().getBytes();
            }
            
            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };
        
        // Задаем таймаут и пробы
        request.setShouldRetryServerErrors(true);
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                30000, // 30 секунд таймаут
                1,     // Максимум 1 повторная попытка
                1.0f   // Стандартный backoffMultiplier
        ));
        
        // Отправляем запрос
        queue.add(request);
    }
    
    /**
     * Интерфейс для обратного вызова при обновлении токена
     */
    public interface TokenRefreshCallback {
        void onTokenRefreshed();
        void onTokenRefreshFailed(String errorMessage);
    }
} 