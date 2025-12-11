package com.example.prmu_lab2.network;

public class SupabaseConfig {
    // URL проекта Supabase
    public static final String SUPABASE_URL = "https://ugfdlwmjrtkufrzrlnus.supabase.co";
    // Публичный anon‑ключ проекта
    public static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVnZmRsd21qcnRrdWZyenJsbnVzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU0Njg4MzUsImV4cCI6MjA4MTA0NDgzNX0.KAkgnwwXy-De0atQprjb1NTFPNu7goSMW8Pei8wDhlA";
    // Эндпоинты аутентификации
    public static final String AUTH_SIGNUP_URL =
            SUPABASE_URL + "/auth/v1/signup";
    public static final String AUTH_SIGNIN_URL =
            SUPABASE_URL + "/auth/v1/token?grant_type=password";
    // Эндпоинт таблицы по варианту
    public static final String TABLE_URL =
            SUPABASE_URL + "/rest/v1/contacts";

    // Заголовки для запросов
    public static final String HEADER_API_KEY = "apikey";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";

}
