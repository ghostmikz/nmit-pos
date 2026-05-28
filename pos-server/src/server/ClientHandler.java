package server;

import handler.CategoryHandler;
import handler.LoginHandler;
import handler.PaymentMethodHandler;
import handler.ProductHandler;
import handler.ReportHandler;
import handler.SaleHandler;
import handler.UserHandler;
import model.Request;
import model.Response;
import model.User;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                try {
                    Request req = (Request) in.readObject();
                    Response response = handle(req);
                    out.writeObject(response);
                    out.reset();
                    out.flush();
                } catch (EOFException | java.net.SocketException e) {
                    break;
                } catch (ClassNotFoundException e) {
                    // skip malformed request
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            dao.DatabaseConnection.close();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private Response handle(Request req) {
        try {
            String action = req.getAction();

            if ("LOGIN".equals(action)) return LoginHandler.handle(req);

            User user = SessionManager.getUser(req.getToken());
            if (user == null) return Response.error("Unauthorized — please log in");

            return switch (action) {
                case "LOGOUT"              -> LoginHandler.logout(req);
                case "GET_PRODUCTS"        -> ProductHandler.getAll(req, user);
                case "GET_CATEGORIES"      -> CategoryHandler.getAll(req, user);
                case "ADD_CATEGORY"        -> CategoryHandler.add(req, user);
                case "UPDATE_CATEGORY"     -> CategoryHandler.update(req, user);
                case "DELETE_CATEGORY"     -> CategoryHandler.delete(req, user);
                case "ADD_PRODUCT"         -> ProductHandler.add(req, user);
                case "UPDATE_PRODUCT"      -> ProductHandler.update(req, user);
                case "DELETE_PRODUCT"      -> ProductHandler.delete(req, user);
                case "UPDATE_STOCK"        -> ProductHandler.updateStock(req, user);
                case "GET_PRODUCT_IMAGE"   -> ProductHandler.getImage(req, user);
                case "UPDATE_PRODUCT_IMAGE"-> ProductHandler.updateImage(req, user);
                case "CREATE_SALE"         -> SaleHandler.create(req, user);
                case "PROCESS_REFUND"      -> SaleHandler.processRefund(req, user);
                case "GET_USERS"           -> UserHandler.getAll(req, user);
                case "ADD_USER"            -> UserHandler.add(req, user);
                case "UPDATE_USER"         -> UserHandler.update(req, user);
                case "SET_USER_ACTIVE"     -> UserHandler.setActive(req, user);
                case "DELETE_USER"         -> UserHandler.delete(req, user);
                case "GET_REPORT"           -> ReportHandler.getSalesReport(req, user);
                case "GET_PAYMENT_METHODS"    -> PaymentMethodHandler.getAll(req, user);
                case "ADD_PAYMENT_METHOD"     -> PaymentMethodHandler.add(req, user);
                case "DELETE_PAYMENT_METHOD"  -> PaymentMethodHandler.delete(req, user);
                case "GET_DASHBOARD"          -> ReportHandler.getDashboard(req, user);
                default                    -> Response.error("Unknown action: " + action);
            };
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Server error: " + e.getMessage());
        }
    }
}
