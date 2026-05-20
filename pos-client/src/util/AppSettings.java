package util;

import java.io.*;
import java.util.Properties;

public class AppSettings {
    private static final String FILE = "settings.properties";
    private static final Properties props = new Properties();

    static { load(); }

    private AppSettings() {}

    private static void load() {
        File f = new File(FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
            } catch (IOException ignored) {}
        }
    }

    public static void save() {
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            props.store(out, "NMIT-POS Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String get(String key, String def) { return props.getProperty(key, def); }
    public static void   set(String key, String val)  { props.setProperty(key, val); }

    public static String getLanguage()              { return get("language",    "mn"); }
    public static void   setLanguage(String lang)   { set("language", lang); }

    public static String getServerHost()            { return get("server.host", "localhost"); }
    public static void   setServerHost(String host) { set("server.host", host); }

    public static int    getServerPort()            { try { return Integer.parseInt(get("server.port", "9090")); } catch (NumberFormatException e) { return 9090; } }
    public static void   setServerPort(int port)    { set("server.port", String.valueOf(port)); }
}
