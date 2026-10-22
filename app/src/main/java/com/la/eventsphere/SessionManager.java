
package com.la.eventsphere;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "EventSpherePrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_ACCOUNT_TYPE = "accountType";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveSession(String accountType) {
        preferences.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_ACCOUNT_TYPE, accountType)
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getAccountType() {
        return preferences.getString(KEY_ACCOUNT_TYPE, "");
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
