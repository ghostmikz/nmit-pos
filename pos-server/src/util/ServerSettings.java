package util;

import java.io.*;
import java.util.Properties;

public class ServerSettings {
    private static final String FILE  = "server.properties";
    private static final Properties props = new Properties();

    static { load(); }

    private ServerSettings() {}

    private static void load() {
        File f = new File(FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                props.load(in);
                System.out.println("[Config] Loaded " + f.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[Config] Failed to read " + FILE + ": " + e.getMessage());
            }
        } else {
            System.out.println("[Config] " + FILE + " not found — using defaults");
        }
    }

    private static String get(String key, String def) { return props.getProperty(key, def); }

    public static int    getPort()       {
        try { return Integer.parseInt(get("server.port", "9090")); }
        catch (NumberFormatException e) { return 9090; }
    }

    public static String getDbUrl()      { return get("db.url",
        "jdbc:mysql://localhost:3306/pos_db?useSSL=false&serverTimezone=UTC" +
        "&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true"); }

    public static String getDbUser()     { return get("db.user",     "root"); }
    public static String getDbPassword() { return get("db.password", "");     }
}
