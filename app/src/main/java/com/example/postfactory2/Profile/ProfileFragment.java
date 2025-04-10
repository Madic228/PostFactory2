package com.example.postfactory2.Profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.postfactory2.Auth.AuthActivity;
import com.example.postfactory2.Auth.LoginActivity;
import com.example.postfactory2.Profile.History.HistoryFragment;
import com.example.postfactory2.Profile.UpdateShedule.UpdateScheduleFragment;
import com.example.postfactory2.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import com.example.postfactory2.Auth.TokenManager;

public class ProfileFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profileImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Инициализация элементов
        profileImage = view.findViewById(R.id.profile_image);

        // Загрузка сохраненного изображения
        loadProfileImage();

        // Обработчики кнопок
        view.findViewById(R.id.change_photo_text).setOnClickListener(v -> openImagePicker());
        view.findViewById(R.id.history_button).setOnClickListener(v -> openHistory());
        view.findViewById(R.id.update_button).setOnClickListener(v -> openUpdate());
        view.findViewById(R.id.about_button).setOnClickListener(v -> openAbout());
        
        // Добавляем кнопку выхода из аккаунта
        Button logoutButton = view.findViewById(R.id.logout_button);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> logout());
        }

        return view;
    }
    
    // Метод для выхода из аккаунта
    private void logout() {
        // Очищаем данные пользователя через TokenManager
        TokenManager tokenManager = TokenManager.getInstance(requireContext());
        tokenManager.logout();
        
        // Показываем сообщение
        Toast.makeText(requireContext(), "Выход выполнен", Toast.LENGTH_SHORT).show();
        
        // Переходим на экран авторизации
        Intent intent = new Intent(requireActivity(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // Открытие выбора изображения
    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    // Получение результата выбора изображения
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                // Получение изображения и обрезка до круга
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                Bitmap circularBitmap = getCircularBitmap(bitmap);
                profileImage.setImageBitmap(circularBitmap);

                // Сохранение изображения
                saveProfileImage(circularBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Метод для обрезки изображения до круга
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Обрезаем изображение в круг
        Rect srcRect = new Rect(0, 0, size, size);
        RectF destRect = new RectF(0, 0, size, size);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.WHITE);
        canvas.drawOval(destRect, paint);

        // Накладываем изображение в круг
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, srcRect, destRect, paint);

        return output;
    }

    // Сохранение изображения в локальное хранилище
    private void saveProfileImage(Bitmap bitmap) {
        try {
            File file = new File(requireContext().getFilesDir(), "profile_image.png");
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();

            SharedPreferences preferences = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
            preferences.edit().putString("image_path", file.getAbsolutePath()).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Загрузка сохраненного изображения
    private void loadProfileImage() {
        SharedPreferences preferences = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE);
        String imagePath = preferences.getString("image_path", null);

        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                profileImage.setImageBitmap(getCircularBitmap(bitmap));
            }
        }
    }

    // Обработчики других кнопок
    private void openHistory() {
        // Создаем экземпляр HistoryFragment
        HistoryFragment historyFragment = new HistoryFragment();

        // Выполняем замену фрагмента
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, historyFragment) // ID контейнера для фрагментов
                .addToBackStack(null) // Добавляем в стек для возврата назад
                .commit();
    }


    private void openUpdate() {
        // Создаем экземпляр UpdateScheduleFragment
        UpdateScheduleFragment updateScheduleFragment = new UpdateScheduleFragment();

        // Выполняем замену фрагмента
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, updateScheduleFragment) // ID контейнера для фрагментов
                .addToBackStack(null) // Добавляем в стек для возврата назад
                .commit();
    }



    private void openAbout() {
        Toast.makeText(getContext(), "О приложении", Toast.LENGTH_SHORT).show();
    }

    // После успешной генерации новости в ResultFragment
    private void saveGenerationToHistory(String title, String content) {
        // Получаем токен пользователя
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(
                LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        String token = sharedPreferences.getString(LoginActivity.KEY_TOKEN, "");
        
        // Если нет токена, выходим
        if (token.isEmpty()) {
            return;
        }
        
        try {
            RequestQueue queue = Volley.newRequestQueue(requireContext());
            String url = "http://2.59.40.125:8000/api/generations";
            
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("title", title);
            jsonBody.put("content", content);
            
            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        // Успешно сохранено
                    },
                    error -> {
                        // Ошибка сохранения
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            
            queue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
