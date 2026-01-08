package com.example.androidexample;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREFS = "theme_prefs";
    private static final String KEY_THEME = "theme";

    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    // Call before setContentView in each Activity
    public static void applyTheme(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, THEME_LIGHT);
        setThemeMode(theme);
    }

    public static void setTheme(Context context, String theme) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_THEME, theme).apply();
        setThemeMode(theme);
    }

    public static boolean isDark(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, THEME_LIGHT);
        return THEME_DARK.equals(theme);
    }

    private static void setThemeMode(String theme) {
        if (THEME_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
            );
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
            );
        }
    }
}
