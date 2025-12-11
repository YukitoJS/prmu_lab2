package com.example.prmu_lab2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.prmu_lab2.network.SupabaseConfig;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonRegister;
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Проверка, на активную сессию
        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);
        if (accessToken != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> signIn());
        buttonRegister.setOnClickListener(v -> signUp());
    }

    private void signUp() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        networkExecutor.execute(() -> {
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNUP_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Настройка соединения
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Создание JSON тела запроса
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("email", email);
                jsonBody.put("password", password);

                // Отправка данных
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(jsonBody.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                // Получение ответа
                int responseCode = connection.getResponseCode();
                String response = readResponse(connection, responseCode);

                mainHandler.post(() -> {
                    if (responseCode == 200) {
                        Toast.makeText(LoginActivity.this,
                                "Регистрация успешна! Теперь войдите",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Ошибка регистрации: " + responseCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });

                connection.disconnect();

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(LoginActivity.this,
                                "Ошибка: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void signIn() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        networkExecutor.execute(() -> {
            try {
                URL url = new URL(SupabaseConfig.AUTH_SIGNIN_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Настройка соединения
                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Создание JSON тела запроса
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("email", email);
                jsonBody.put("password", password);

                // Отправка данных
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(jsonBody.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                // Получение ответа
                int responseCode = connection.getResponseCode();
                String response = readResponse(connection, responseCode);

                mainHandler.post(() -> {
                    if (responseCode == 200) {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String accessToken = jsonResponse.getString("access_token");
                            String userId = jsonResponse.getJSONObject("user").getString("id");

                            // Сохранение данных сессии
                            SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
                            prefs.edit()
                                    .putString("access_token", accessToken)
                                    .putString("user_id", userId)
                                    .apply();

                            // Переход на главный экран
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();

                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this,
                                    "Ошибка парсинга ответа",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Ошибка входа: " + responseCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });

                connection.disconnect();

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(LoginActivity.this,
                                "Ошибка: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private String readResponse(HttpURLConnection connection, int responseCode) {
        try {
            if (responseCode >= 200 && responseCode < 300) {
                java.io.InputStream inputStream = connection.getInputStream();
                java.util.Scanner scanner = new java.util.Scanner(inputStream).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            } else {
                java.io.InputStream errorStream = connection.getErrorStream();
                java.util.Scanner scanner = new java.util.Scanner(errorStream).useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (Exception e) {
            return "Ошибка чтения ответа: " + e.getMessage();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        networkExecutor.shutdown();
    }
}