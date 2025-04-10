package com.example.postfactory2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.postfactory2.Auth.AuthActivity;
import com.example.postfactory2.Auth.TokenManager;
import com.example.postfactory2.Generate.GenerateFragment;
import com.example.postfactory2.Home.HomeFragment;
import com.example.postfactory2.Profile.ProfileFragment;
import com.example.postfactory2.utils.EnvConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // Увеличим интервал проверки токена до 30 минут
    private static final long TOKEN_CHECK_INTERVAL = 30 * 60 * 1000;
    
    // Флаг, указывающий, что происходит обновление токена
    private boolean isTokenRefreshing = false;
    
    private TokenManager tokenManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tokenCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Инициализация TokenManager
        tokenManager = TokenManager.getInstance(this);

        // Инициализируем конфигурацию
        EnvConfig.init(getApplicationContext());

        // Загрузить первый фрагмент
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Настройка BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_generate) {
                fragment = new GenerateFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
            }
            return true;
        });
        
        // Инициализируем периодическую проверку токена только один раз при запуске
        if (!isTokenRefreshing) {
            initTokenChecker();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // НЕ запускаем отдельную проверку при каждом onResume, 
        // так как это может вызывать мерцание экрана
        // Периодическая проверка из initTokenChecker справится с обновлением
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Не останавливаем проверку, чтобы избежать лишних перезапусков
    }
    
    /**
     * Инициализирует периодическую проверку и обновление токена
     */
    private void initTokenChecker() {
        tokenCheckRunnable = () -> {
            Log.d(TAG, "Выполняется плановая проверка токена");
            
            // Проверяем, что процесс обновления токена не идет
            if (!isTokenRefreshing) {
                // Проверяем нужно ли обновить токен
                if (tokenManager.shouldRefreshToken()) {
                    refreshTokenSafely();
                } else {
                    Log.d(TAG, "Токен действителен");
                    // Планируем следующую проверку
                    scheduleNextCheck();
                }
            } else {
                // Токен уже обновляется, планируем следующую проверку
                scheduleNextCheck();
            }
        };
        
        // Запускаем первую проверку через полный интервал вместо /6,
        // чтобы избежать частых проверок при запуске
        handler.postDelayed(tokenCheckRunnable, TOKEN_CHECK_INTERVAL);
    }
    
    /**
     * Планирует следующую проверку токена
     */
    private void scheduleNextCheck() {
        handler.postDelayed(tokenCheckRunnable, TOKEN_CHECK_INTERVAL);
    }
    
    /**
     * Безопасно обновляет токен, предотвращая циклические обновления
     */
    private void refreshTokenSafely() {
        // Устанавливаем флаг, что токен обновляется
        isTokenRefreshing = true;
        Log.d(TAG, "Начинаем обновление токена");
        
        tokenManager.refreshToken(new TokenManager.TokenRefreshCallback() {
            @Override
            public void onTokenRefreshed() {
                Log.d(TAG, "Токен успешно обновлен");
                // Сбрасываем флаг
                isTokenRefreshing = false;
                // Планируем следующую проверку
                scheduleNextCheck();
            }

            @Override
            public void onTokenRefreshFailed(String errorMessage) {
                Log.e(TAG, "Не удалось обновить токен: " + errorMessage);
                // Сбрасываем флаг
                isTokenRefreshing = false;
                
                // Только в случае серьезной ошибки направляем на экран авторизации
                // и только если токен полностью истек, а не "скоро истечет"
                if (tokenManager.isTokenExpired()) {
                    redirectToAuth();
                } else {
                    // Если токен еще действителен, просто планируем проверку позже
                    scheduleNextCheck();
                }
            }
        });
    }
    
    /**
     * Перенаправляет пользователя на экран авторизации
     */
    private void redirectToAuth() {
        if (!isFinishing() && !isDestroyed()) {
            Log.d(TAG, "Перенаправление на экран авторизации");
            Intent intent = new Intent(this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

