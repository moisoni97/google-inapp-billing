package games.moisoni.google_inapp_billing;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public final class SharedPrefsHelper {

    private static final String DEFAULT_SUFFIX = "_preferences";
    private static SharedPreferences mPrefs;

    private static void initPrefs(@NonNull Context context, String prefsName, int mode) {
        mPrefs = context.getSharedPreferences(prefsName, mode);
    }

    public static SharedPreferences getPreferences() {
        if (mPrefs != null) {
            return mPrefs;
        }

        throw new RuntimeException("SharedPrefsHelper class is not correctly instantiated");
    }

    public static int getInt(final String key, final int defValue) {
        return getPreferences().getInt(key, defValue);
    }

    public static boolean getBoolean(final String key, final boolean defValue) {
        return getPreferences().getBoolean(key, defValue);
    }

    public static String getString(final String key, final String defValue) {
        return getPreferences().getString(key, defValue);
    }

    public static void putInt(final String key, final int value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static void putBoolean(final String key, final boolean value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void putString(final String key, final String value) {
        final SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    public final static class Builder {

        private String mKey;
        private Context mContext;

        private int mMode = -1;
        private boolean mUseDefault = false;

        public Builder setPrefsName(final String prefsName) {
            mKey = prefsName;
            return this;
        }

        public Builder setContext(final Context context) {
            mContext = context;
            return this;
        }

        public Builder setMode(final int mode) {
            if (mode == ContextWrapper.MODE_PRIVATE) {
                mMode = mode;
            } else {
                throw new RuntimeException("Mode can only be set to ContextWrapper.MODE_PRIVATE");
            }

            return this;
        }

        public Builder setUseDefaultSharedPreference(boolean defaultSharedPreference) {
            mUseDefault = defaultSharedPreference;
            return this;
        }

        public void build() {
            if (mContext == null) {
                throw new RuntimeException("Please set the context before building SharedPrefsHelper instance");
            }

            if (TextUtils.isEmpty(mKey)) {
                mKey = mContext.getPackageName();
            }

            if (mUseDefault) {
                mKey += DEFAULT_SUFFIX;
            }

            if (mMode == -1) {
                mMode = ContextWrapper.MODE_PRIVATE;
            }

            SharedPrefsHelper.initPrefs(mContext, mKey, mMode);
        }
    }
}