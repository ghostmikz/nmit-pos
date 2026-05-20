package server;

import dao.DatabaseConnection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class POSServer {
    private static final int PORT = 9090;

    public static void main(String[] args) {
        System.out.println("NMIT-POS Server starting on port " + PORT + "...");

        // Test DB connection on startup
        try {
            DatabaseConnection.getInstance();
            System.out.println("Database connected successfully.");
        } catch (Exception e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            System.exit(1);
        }

        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server ready. Waiting for clients...");
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("Client connected: " + client.getInetAddress());
                pool.execute(new ClientHandler(client));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            DatabaseConnection.close();
        }
    }
}
