package com.example.prmu_lab2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prmu_lab2.adapters.ContactAdapter;
import com.example.prmu_lab2.models.Contact;
import com.example.prmu_lab2.network.SupabaseConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String accessToken;
    private String userId;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private ContactAdapter adapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences prefs = getSharedPreferences("session", MODE_PRIVATE);
        accessToken = prefs.getString("access_token", null);

        if (accessToken == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return; }

        initViews();
        setupRecyclerView();

        loadContacts();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadContacts() {
        showLoading(true);

        networkExecutor.execute(() -> {
            try {
                // Создаем URL для получения данных
                URL url = new URL(SupabaseConfig.TABLE_URL + "?select=*");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Настраиваем соединение (GET запрос)
                connection.setRequestMethod("GET");
                connection.setRequestProperty(SupabaseConfig.HEADER_API_KEY, SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken);
                connection.setRequestProperty(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON);

                // Получаем ответ
                int responseCode = connection.getResponseCode();
                String response = readResponse(connection, responseCode);

                mainHandler.post(() -> {
                    showLoading(false);

                    if (responseCode == 200) {
                        List<Contact> items = parseContacts(response);
                        adapter.setContact(items);

                        if (items.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                        }

                        showToast("Загружено: " + items.size() + " предметов");

                    } else {
                        showToast("Ошибка загрузки: " + responseCode);
                        showEmptyState(true);
                    }
                });

                connection.disconnect();

            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    showToast("Сетевая ошибка: " + e.getMessage());
                    showEmptyState(true);
                });
            }
        });
    }

    private List<Contact> parseContacts(String jsonResponse) {
        List<Contact> items = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(jsonResponse);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                Contact item = new Contact();
                item.setId(jsonObject.optString("id"));
                item.setContactName(jsonObject.optString("contact_name", "Без названия"));
                item.setImportanceContact(jsonObject.optInt("importance_contact", 0));
                item.setLastContactDate(jsonObject.optString("last_contact_date", ""));

                items.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }

    private String readResponse(HttpURLConnection connection, int responseCode) {
        try {
            InputStream inputStream;
            if (responseCode >= 200 && responseCode < 300) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            return "Ошибка чтения: " + e.getMessage();
        }
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState(boolean isEmpty) {
        textViewEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkExecutor != null && !networkExecutor.isShutdown()) {
            networkExecutor.shutdown();
        }
    }
}