package com.example.prmu_lab2.models;

public class Contact {
    private int id;
    private String title;
    private double dateLastContact;
    private String thumbnail;
    private String description;

    public Contact() {
    }

    public double getDateLastContact() {
        return dateLastContact;
    }

    public void setDateLastContact(double dateLastContact) {
        this.dateLastContact = dateLastContact;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
