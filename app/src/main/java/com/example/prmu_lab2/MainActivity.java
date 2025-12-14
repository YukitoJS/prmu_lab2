package com.example.prmu_lab2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.prmu_lab2.adapters.ContactAdapter;
import com.example.prmu_lab2.models.Contact;
import com.example.prmu_lab2.network.SupabaseConfig;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
        implements ContactAdapter.OnItemClickListener,
        ContactAdapter.OnDeleteClickListener {

    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String accessToken;
    private String userId;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textViewEmpty;
    private ContactAdapter adapter;
    private FloatingActionButton fabAdd;



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
        setupClickListeners();

        loadContacts();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        textViewEmpty = findViewById(R.id.textViewEmpty);
        fabAdd = findViewById(R.id.fabAdd);
    }

    private void setupRecyclerView() {
        adapter = new ContactAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> showAddDialog());
    }

    // === МЕТОД ДЛЯ ДОБАВЛЕНИЯ КОНТАКТА ===
    private void showAddDialog() {
        // Создаем поля ввода
        final EditText etContactName = new EditText(this);
        etContactName.setHint("Имя контакта");

        final EditText etImportance = new EditText(this);
        etImportance.setHint("Важность контакта (1-10)");
        etImportance.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        final EditText etDate = new EditText(this);
        etDate.setHint("Дата последнего контакта (ГГГГ-ММ-ДД)");
        etDate.setText("2024-12-01");  // Пример для удобства

        // Создаем контейнер для полей
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        container.addView(etContactName);
        container.addView(etImportance);
        container.addView(etDate);

        // Создаем диалог
        new AlertDialog.Builder(this)
                .setTitle("Добавить контакт в контакты")
                .setView(container)
                .setPositiveButton("Добавить", (dialog, which) -> {
                    String itemName = etContactName.getText().toString().trim();
                    String importanceStr = etImportance.getText().toString().trim();
                    String date = etDate.getText().toString().trim();

                    if (itemName.isEmpty() || importanceStr.isEmpty() || date.isEmpty()) {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int importance = Integer.parseInt(importanceStr);
                        addContactItem(itemName, importance, date);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Введите корректную важность", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void addContactItem(String contactName, int importance, String date) {
        showLoading(true);

        networkExecutor.execute(() -> {
            try {
                URL url = new URL(SupabaseConfig.TABLE_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("apikey", SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("contact_name", contactName);
                jsonBody.put("importance_contact", importance);
                jsonBody.put("last_contact_date", date);
                jsonBody.put("user_id", userId);

                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(jsonBody.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();

                mainHandler.post(() -> {
                    showLoading(false);

                    if (responseCode == 201) {
                        // Просто перезагружаем список вместо парсинга ответа
                        loadContacts();
                        Toast.makeText(this, "Контакт добавлен", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });

                connection.disconnect();

            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // === МЕТОД ДЛЯ РЕДАКТИРОВАНИЯ КОНТАКТА ===
    @Override
    public void onItemClick(Contact item) {
        showEditDialog(item);
    }

    private void showEditDialog(Contact item) {
        // Создаем поля ввода с предзаполненными значениями
        final EditText etItemName = new EditText(this);
        etItemName.setHint("Название контакта");
        etItemName.setText(item.getContactName());

        final EditText etImportance = new EditText(this);
        etImportance.setHint("Важность контакнта (1-10)");
        etImportance.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etImportance.setText(String.valueOf(item.getImportanceContact()));

        final EditText etDate = new EditText(this);
        etDate.setHint("Дата последнего контакта (ГГГГ-ММ-ДД)");
        etDate.setText(item.getLastContactDate());

        // Создаем контейнер для полей
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        container.addView(etItemName);
        container.addView(etImportance);
        container.addView(etDate);

        // Создаем диалог
        new AlertDialog.Builder(this)
                .setTitle("Редактировать предмет")
                .setView(container)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String itemName = etItemName.getText().toString().trim();
                    String importanceStr = etImportance.getText().toString().trim();
                    String date = etDate.getText().toString().trim();

                    if (itemName.isEmpty() || importanceStr.isEmpty() || date.isEmpty()) {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int cost = Integer.parseInt(importanceStr);
                        updateContactItem(item.getId(), itemName, cost, date);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Введите корректную стоимость", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void updateContactItem(String itemId, String itemName, int importance, String date) {
        showLoading(true);

        networkExecutor.execute(() -> {
            try {
                URL url = new URL(SupabaseConfig.TABLE_URL + "?id=eq." + itemId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Используем PATCH для частичного обновления
                connection.setRequestMethod("PATCH");
                connection.setRequestProperty(SupabaseConfig.HEADER_API_KEY, SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken);
                connection.setRequestProperty(SupabaseConfig.HEADER_CONTENT_TYPE, SupabaseConfig.CONTENT_TYPE_JSON);
                connection.setDoOutput(true);

                // Создаем JSON только с измененными полями
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("contact_name", itemName);
                jsonBody.put("importance_contact", importance);
                jsonBody.put("last_contact_date", date);

                // Отправляем данные
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(jsonBody.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                int responseCode = connection.getResponseCode();

                mainHandler.post(() -> {
                    showLoading(false);

                    if (responseCode == 204) {  // 204 No Content
                        // Обновляем элемент в адаптере
                        Contact updatedItem = new Contact();
                        updatedItem.setId(itemId);
                        updatedItem.setUserId(userId);
                        updatedItem.setContactName(itemName);
                        updatedItem.setImportanceContact(importance);
                        updatedItem.setLastContactDate(date);

                        adapter.updateItem(updatedItem);

                        Toast.makeText(this, "Контакт обновлен", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка обновления: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });

                connection.disconnect();

            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Сетевая ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // === МЕТОД ДЛЯ УДАЛЕНИЯ КОНТАКТА ===
    @Override
    public void onDeleteClick(String itemId, String itemName) {
        showDeleteConfirmationDialog(itemId, itemName);
    }

    private void showDeleteConfirmationDialog(String itemId, String itemName) {
        new AlertDialog.Builder(this)
                .setTitle("Удалить контакт?")
                .setMessage("Вы уверены, что хотите удалить \"" + itemName + "\"?")
                .setPositiveButton("Да", (dialog, which) -> deleteInventoryItem(itemId))
                .setNegativeButton("Нет", null)
                .show();
    }

    private void deleteInventoryItem(String itemId) {
        showLoading(true);

        networkExecutor.execute(() -> {
            try {
                URL url = new URL(SupabaseConfig.TABLE_URL + "?id=eq." + itemId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("DELETE");
                connection.setRequestProperty(SupabaseConfig.HEADER_API_KEY, SupabaseConfig.SUPABASE_ANON_KEY);
                connection.setRequestProperty(SupabaseConfig.HEADER_AUTHORIZATION, "Bearer " + accessToken);

                int responseCode = connection.getResponseCode();

                mainHandler.post(() -> {
                    showLoading(false);

                    if (responseCode == 204) {  // 204 No Content
                        // Удаляем элемент из адаптера
                        adapter.removeItem(itemId);

                        if (adapter.getItemCount() == 0) {
                            showEmptyState(true);
                        }

                        Toast.makeText(this, "Контакт удален", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка удаления: " + responseCode, Toast.LENGTH_SHORT).show();
                    }
                });

                connection.disconnect();

            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "Сетевая ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
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