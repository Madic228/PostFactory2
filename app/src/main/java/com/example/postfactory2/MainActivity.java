package com.example.postfactory2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.postfactory2.Generate.GenerateFragment;
import com.example.postfactory2.Home.HomeFragment;
import com.example.postfactory2.Profile.ProfileFragment;
import com.example.postfactory2.utils.EnvConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}

