package com.example.postfactory2.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnvConfig {
    private static final String TAG = "EnvConfig";
    private static Map<String, String> envVariables = new HashMap<>();
    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (isInitialized) {
            return;
        }

        try {
            // Получаем путь к корневой директории проекта
            File projectRoot = new File(context.getExternalFilesDir(null).getParentFile().getParentFile().getParentFile().getParentFile().toURI());
            File envFile = new File(projectRoot, ".env");

            Log.d(TAG, "Trying to read .env file from: " + envFile.getAbsolutePath());

            if (!envFile.exists()) {
                Log.e(TAG, ".env file not found at: " + envFile.getAbsolutePath());
                throw new IOException(".env file not found");
            }

            BufferedReader reader = new BufferedReader(new FileReader(envFile));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        envVariables.put(parts[0].trim(), parts[1].trim());
                        Log.d(TAG, "Loaded env variable: " + parts[0].trim() + " = " + parts[1].trim());
                    }
                }
            }
            reader.close();
            isInitialized = true;
        } catch (IOException e) {
            Log.e(TAG, "Error reading .env file", e);
            // Устанавливаем значение по умолчанию
            envVariables.put("summarization_server_ip", "192.168.31.252");
            Log.d(TAG, "Using default IP: 192.168.31.252");
        }
    }

    public static String get(String key) {
        return envVariables.getOrDefault(key, "");
    }

    public static String get(String key, String defaultValue) {
        return envVariables.getOrDefault(key, defaultValue);
    }
}