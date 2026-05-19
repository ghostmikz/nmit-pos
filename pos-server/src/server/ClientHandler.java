package server;

import com.google.gson.Gson;
import handler.LoginHandler;
import handler.ProductHandler;
import handler.ReportHandler;
import handler.SaleHandler;
import model.Request;
import model.Response;
import model.User;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream(),  "UTF-8"));
             PrintWriter   out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                Response response = handle(line);
                out.println(gson.toJson(response));
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private Response handle(String json) {
        try {
            Request req = gson.fromJson(json, Request.class);
            String action = req.getAction();

            // LOGIN does not require a token
            if ("LOGIN".equals(action)) return LoginHandler.handle(req);

            // All other actions require a valid session
            User user = SessionManager.getUser(req.getToken());
            if (user == null) return Response.error("Unauthorized — please log in");

            return switch (action) {
                case "LOGOUT"         -> LoginHandler.logout(req);
                case "GET_PRODUCTS"   -> ProductHandler.getAll(req, user);
                case "GET_CATEGORIES" -> ProductHandler.getCategories(req, user);
                case "ADD_PRODUCT"    -> ProductHandler.add(req, user);
                case "UPDATE_PRODUCT" -> ProductHandler.update(req, user);
                case "DELETE_PRODUCT" -> ProductHandler.delete(req, user);
                case "UPDATE_STOCK"        -> ProductHandler.updateStock(req, user);
                case "GET_PRODUCT_IMAGE"   -> ProductHandler.getImage(req, user);
                case "UPDATE_PRODUCT_IMAGE"-> ProductHandler.updateImage(req, user);
                case "CREATE_SALE"    -> SaleHandler.create(req, user);
                case "PROCESS_REFUND" -> SaleHandler.processRefund(req, user);
                case "GET_REPORT"     -> ReportHandler.getSalesReport(req, user);
                case "GET_DASHBOARD"  -> ReportHandler.getDashboard(req, user);
                default               -> Response.error("Unknown action: " + action);
            };
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Server error: " + e.getMessage());
        }
    }
}
