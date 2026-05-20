package server;

import model.User;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Map<String, User> sessions = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static String createSession(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user);
        return token;
    }

    public static User getUser(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public static void removeSession(String token) {
        sessions.remove(token);
    }
}
