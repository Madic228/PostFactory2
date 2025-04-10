package com.example.postfactory2.Auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.postfactory2.MainActivity;
import com.example.postfactory2.R;

public class AuthActivity extends AppCompatActivity {
    private static final String TAG = "AuthActivity";
    private TokenManager tokenManager;
    private boolean isTokenRefreshing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Инициализация TokenManager
        tokenManager = TokenManager.getInstance(this);
        
        // Проверяем, авторизован ли пользователь, только если не обновляем токен
        if (tokenManager.isLoggedIn() && !isTokenRefreshing) {
            // Проверяем, не истек ли токен
            if (tokenManager.isTokenExpired()) {
                Log.d(TAG, "Токен истек, пытаемся обновить");
                
                // Пытаемся обновить токен, предотвращая циклы
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
                        // Продолжаем показывать экран авторизации
                        isTokenRefreshing = false;
                        showAuthScreen();
                    }
                });
                return;
            } else {
                // Если пользователь авторизован и токен действителен, переходим на главный экран
                Log.d(TAG, "Пользователь авторизован, переходим на главный экран");
                startMainActivity();
                return;
            }
        }
        
        // Если пользователь не авторизован, показываем экран авторизации
        showAuthScreen();
    }
    
    private void showAuthScreen() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        setContentView(R.layout.auth_screen);

        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);

        // Переход на экран авторизации
        loginButton.setOnClickListener(v -> {
            Intent intent = new Intent(AuthActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Переход на экран регистрации
        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(AuthActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
    
    private void startMainActivity() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
