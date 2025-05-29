package com.example.postfactory2.Auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.postfactory2.MainActivity;
import com.example.postfactory2.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final String LOGIN_URL = "http://2.59.40.125:8000/api/auth/login";
    private static final int TIMEOUT_MS = 10000; // 10 секунд таймаут
    private static final int MAX_RETRIES = 2; // Максимум 2 попытки

    public static final String PREF_NAME = "UserSessionPref";
    public static final String KEY_TOKEN = "token";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    
    private TokenManager tokenManager;
    private boolean isTokenRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Инициализация менеджера токенов
        tokenManager = TokenManager.getInstance(this);
        
        // Проверка, авторизован ли пользователь, но только если не обновляем токен
        if (tokenManager.isLoggedIn() && !isTokenRefreshing) {
            // Проверяем, не истек ли токен
            if (tokenManager.isTokenExpired()) {
                Log.d(TAG, "Токен истек, пытаемся обновить");
                // Пытаемся обновить токен
                isTokenRefreshing = true;
                tokenManager.refreshToken(new TokenManager.TokenRefreshCallback() {
                    @Override
                    public void onTokenRefreshed() {
                        Log.d(TAG, "Токен успешно обновлен, переходим на главный экран");
                        isTokenRefreshing = false;
                        startMainActivity();
                    }

                    @Override
                    public void onTokenRefreshFailed(String errorMessage) {
                        Log.e(TAG, "Не удалось обновить токен: " + errorMessage);
                        // Показываем экран входа, так как токен не обновился
                        isTokenRefreshing = false;
                        showLoginScreen();
                    }
                });
                return;
            } else {
                // Если пользователь уже авторизован и токен действителен,
                // перейти на главный экран
                Log.d(TAG, "Пользователь уже авторизован, переходим на главный экран");
                startMainActivity();
                return;
            }
        } else {
            // Показываем экран входа
            showLoginScreen();
        }
    }
    
    private void showLoginScreen() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        setContentView(R.layout.activity_login);

        EditText etUsername = findViewById(R.id.et_email);
        EditText etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            } else {
                login(username, password);
            }
        });

        tvForgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });
    }

    private void showForgotPasswordDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_forgot_password);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Настройка ширины диалога
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        dialog.getWindow().setLayout(width, android.view.WindowManager.LayoutParams.WRAP_CONTENT);

        // Обработка нажатия на email
        TextView tvItEmail = dialog.findViewById(R.id.tv_it_email);
        tvItEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:it@postfactory.ru"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Восстановление пароля в POST.factory");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        // Обработка нажатия на кнопку закрытия
        Button btnClose = dialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void login(String username, String password) {
        Log.d(TAG, "Попытка входа пользователя: " + username);
        
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(
                Request.Method.POST,
                LOGIN_URL,
                response -> {
                    try {
                        Log.d(TAG, "Получен ответ от сервера: " + response);
                        
                        // Получаем токен из ответа
                        JSONObject jsonResponse = new JSONObject(response);
                        String token = jsonResponse.getString("access_token");
                        
                        // Сохраняем данные с помощью TokenManager
                        tokenManager.saveToken(token, username);
                        tokenManager.savePassword(password);
                        
                        Log.d(TAG, "Данные успешно сохранены");
                        
                        // Сообщение об успешном входе
                        Toast.makeText(LoginActivity.this, "Успешный вход", Toast.LENGTH_SHORT).show();
                        
                        // Переход на главный экран
                        startMainActivity();
                    } catch (JSONException e) {
                        Log.e(TAG, "Ошибка при обработке ответа: " + e.getMessage());
                        Toast.makeText(LoginActivity.this, "Ошибка при обработке ответа", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    // Обработка ошибки
                    Log.e(TAG, "Ошибка при входе: " + error.toString());
                    
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Код ответа: " + error.networkResponse.statusCode);
                        if (error.networkResponse.statusCode == 401) {
                            Toast.makeText(LoginActivity.this, "Неверный логин или пароль", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Ошибка сервера: " + error.networkResponse.statusCode, Toast.LENGTH_SHORT).show();
                        }
                    } else if (error instanceof com.android.volley.TimeoutError) {
                        Toast.makeText(LoginActivity.this, "Сервер не отвечает. Проверьте подключение к интернету", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Ошибка сети: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("grant_type", "password");
                params.put("username", username);
                params.put("password", password);
                params.put("scope", "");
                params.put("client_id", "string");
                params.put("client_secret", "string");
                return params;
            }

            @Override
            public String getBodyContentType() {
                return "application/x-www-form-urlencoded; charset=UTF-8";
            }
        };

        // Устанавливаем таймаут и количество попыток
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }
    
    private void startMainActivity() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
