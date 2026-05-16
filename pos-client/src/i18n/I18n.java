package i18n;

import util.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18n {
    private static ResourceBundle bundle;
    private static Locale currentLocale;
    private static final List<LanguageListener> listeners = new ArrayList<>();

    static {
        currentLocale = new Locale(AppSettings.getLanguage());
        load();
    }

    private static void load() {
        bundle = ResourceBundle.getBundle("i18n.messages", currentLocale);
    }

    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    public static void setLocale(String lang) {
        currentLocale = new Locale(lang);
        AppSettings.setLanguage(lang);
        AppSettings.save();
        load();
        for (LanguageListener l : new ArrayList<>(listeners)) l.onLanguageChanged();
    }

    public static void addListener(LanguageListener l)    { listeners.add(l); }
    public static void removeListener(LanguageListener l) { listeners.remove(l); }

    public static Locale  getLocale()     { return currentLocale; }
    public static boolean isMongolian()   { return "mn".equals(currentLocale.getLanguage()); }
}
