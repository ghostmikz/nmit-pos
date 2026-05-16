package client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import util.AppSettings;

import java.io.*;
import java.net.Socket;

public class SocketClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter    out;
    private final Gson gson = new Gson();

    private static SocketClient instance;

    private SocketClient() {}

    public static SocketClient getInstance() {
        if (instance == null) instance = new SocketClient();
        return instance;
    }

    public void connect() throws IOException {
        String host = AppSettings.getServerHost();
        int    port = AppSettings.getServerPort();
        socket = new Socket(host, port);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public JsonObject send(String action, String token, Object data) throws IOException {
        JsonObject req = new JsonObject();
        req.addProperty("action", action);
        if (token != null) req.addProperty("token", token);
        req.add("data", gson.toJsonTree(data));
        out.println(req.toString());
        String response = in.readLine();
        return JsonParser.parseString(response).getAsJsonObject();
    }

    public JsonObject send(String action, String token) throws IOException {
        return send(action, token, null);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
