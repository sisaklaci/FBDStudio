package com.sisak.fbdstudio.i18n;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * Central UI string lookup, reloadable at runtime when the user switches language. Views register
 * a listener (fired synchronously on {@link #setLocale}) to re-apply text to already-built controls
 * instead of rebuilding the whole UI tree.
 */
public final class Messages {

    private static final Preferences PREFS = Preferences.userNodeForPackage(Messages.class);
    private static final String KEY_LANGUAGE = "language";
    private static final List<Runnable> listeners = new ArrayList<>();

    private static Locale locale = Locale.ENGLISH;
    private static ResourceBundle bundle = ResourceBundle.getBundle("i18n.messages", locale);

    private Messages() {
    }

    public static Language loadSavedLanguageOrDetect() {
        String saved = PREFS.get(KEY_LANGUAGE, null);
        Language lang = saved != null ? Language.fromCode(saved) : Language.detectSystemDefault();
        setLocale(lang.locale());
        return lang;
    }

    public static void saveLanguage(Language lang) {
        PREFS.put(KEY_LANGUAGE, lang.code());
    }

    public static void setLocale(Locale newLocale) {
        locale = newLocale;
        bundle = ResourceBundle.getBundle("i18n.messages", locale);
        Locale.setDefault(newLocale); // keeps JavaFX's own built-in dialog button text in sync
        for (Runnable r : new ArrayList<>(listeners)) {
            r.run();
        }
    }

    public static Locale getLocale() {
        return locale;
    }

    public static String get(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    public static String get(String key, Object... args) {
        return MessageFormat.format(get(key), args);
    }

    /** Registered once per long-lived view; called whenever the language changes. */
    public static void addListener(Runnable r) {
        listeners.add(r);
    }
}
