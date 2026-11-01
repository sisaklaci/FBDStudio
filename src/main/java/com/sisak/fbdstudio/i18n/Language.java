package com.sisak.fbdstudio.i18n;

import java.util.Locale;

/** The languages FBD Studio ships UI translations for. Native names are shown untranslated in the picker. */
public enum Language {
    HU("hu", "Magyar"),
    EN("en", "English"),
    DE("de", "Deutsch"),
    PL("pl", "Polski"),
    ES("es", "Español"),
    FR("fr", "Français"),
    ZH("zh", "中文"),
    JA("ja", "日本語");

    private final String code;
    private final String nativeName;

    Language(String code, String nativeName) {
        this.code = code;
        this.nativeName = nativeName;
    }

    public String code() {
        return code;
    }

    public String nativeName() {
        return nativeName;
    }

    public Locale locale() {
        return Locale.of(code);
    }

    public static Language fromCode(String code) {
        for (Language l : values()) {
            if (l.code.equalsIgnoreCase(code)) {
                return l;
            }
        }
        return EN;
    }

    public static Language detectSystemDefault() {
        String sysLang = Locale.getDefault().getLanguage();
        for (Language l : values()) {
            if (l.code.equalsIgnoreCase(sysLang)) {
                return l;
            }
        }
        return EN;
    }
}
