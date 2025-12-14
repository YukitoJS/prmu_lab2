package com.example.prmu_lab2.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Contact {
    private String id;
    private String userId;
    private String contactName;
    private int importanceContact;
    private String lastContactDate;

    // Конструкторы
    public Contact() {}

    public Contact(String contactName, int importanceContact, String lastContactDate) {
        this.contactName = contactName;
        this.importanceContact = importanceContact;
        this.lastContactDate = lastContactDate;
    }

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public int getImportanceContact() { return importanceContact; }
    public void setImportanceContact(int importanceContact) { this.importanceContact = importanceContact; }

    public String getLastContactDate() { return lastContactDate; }
    public void setLastContactDate(String lastContactDate) { this.lastContactDate = lastContactDate; }

    // Форматированная дата для отображения
    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date date = inputFormat.parse(lastContactDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return lastContactDate;
        }
    }
}
