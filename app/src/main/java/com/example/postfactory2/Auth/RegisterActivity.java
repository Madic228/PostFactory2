package com.example.postfactory2.Auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.postfactory2.R;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TextView tvItEmail = findViewById(R.id.tv_it_email);
        Button btnBack = findViewById(R.id.btn_back);

        // Обработка нажатия на email
        tvItEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:it@postfactory.ru"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Заявка на регистрацию в POST.factory");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        // Обработка нажатия на кнопку "Вернуться"
        btnBack.setOnClickListener(v -> {
            finish();
        });
    }
}
