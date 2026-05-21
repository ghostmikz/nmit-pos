package client;

import model.Request;
import model.Response;
import util.AppSettings;

import java.io.*;
import java.net.Socket;

public class SocketClient {
    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

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
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in  = new ObjectInputStream(socket.getInputStream());
    }

    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public synchronized Response send(String action, String token, Object data) throws IOException {
        try {
            out.writeObject(new Request(action, token, data));
            out.reset();
            out.flush();
            return (Response) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Protocol error: " + e.getMessage(), e);
        }
    }

    public Response send(String action, String token) throws IOException {
        return send(action, token, null);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
