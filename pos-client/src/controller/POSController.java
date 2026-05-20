package controller;

import client.SocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import model.*;
import view.panels.POSPanel;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

public class POSController {

    private static final Gson GSON = new Gson();

    private final POSPanel view;
    private final User     user;

    public POSController(POSPanel view, User user) {
        this.view = view;
        this.user = user;
        view.setPaymentHandler(this::pay);
        view.setImageLoader(this::loadImage);
        loadData();
    }

    private void loadData() {
        new SwingWorker<Void, Void>() {
            List<Product>  products   = new ArrayList<>();
            List<Category> categories = new ArrayList<>();
            String error;

            @Override protected Void doInBackground() {
                try {
                    if (!SocketClient.getInstance().isConnected())
                        SocketClient.getInstance().connect();

                    JsonObject r1 = SocketClient.getInstance().send("GET_CATEGORIES", user.getToken());
                    if ("OK".equals(r1.get("status").getAsString()))
                        categories = GSON.fromJson(r1.get("data"), new TypeToken<List<Category>>(){}.getType());

                    JsonObject r2 = SocketClient.getInstance().send("GET_PRODUCTS", user.getToken());
                    if ("OK".equals(r2.get("status").getAsString()))
                        products = GSON.fromJson(r2.get("data"), new TypeToken<List<Product>>(){}.getType());

                } catch (Exception e) {
                    error = "Серверт холбогдож чадсангүй";
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) { view.showError(error); return; }
                view.setCategories(categories);
                view.setProducts(products);
            }
        }.execute();
    }

    private void loadImage(int productId, Consumer<String> callback) {
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", productId);
                JsonObject res = SocketClient.getInstance().send("GET_PRODUCT_IMAGE", user.getToken(), data);
                if ("OK".equals(res.get("status").getAsString()) && !res.get("data").isJsonNull())
                    return res.get("data").getAsString();
                return null;
            }
            @Override protected void done() {
                try { callback.accept(get()); } catch (Exception e) { callback.accept(null); }
            }
        }.execute();
    }

    private void pay(List<CartItem> items, int paymentMethodId, BigDecimal total, Runnable onSuccess) {
        List<SaleItem> saleItems = new ArrayList<>();
        for (CartItem ci : items) saleItems.add(ci.toSaleItem());

        Map<String, Object> data = new HashMap<>();
        data.put("paymentMethodId", paymentMethodId);
        data.put("subtotal",        total);
        data.put("discountId",      null);
        data.put("discountAmount",  BigDecimal.ZERO);
        data.put("total",           total);
        data.put("notes",           null);
        data.put("items",           saleItems);

        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception {
                return SocketClient.getInstance().send("CREATE_SALE", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    JsonObject res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        String receipt = res.getAsJsonObject("data").get("receiptNumber").getAsString();
                        SwingUtilities.invokeLater(() -> {
                            onSuccess.run();
                            loadData(); // refresh stock counts in POS product grid
                            JOptionPane.showMessageDialog(view,
                                    "Баримт: " + receipt, "Төлбөр амжилттай",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                    } else {
                        String msg = res.get("message").getAsString();
                        SwingUtilities.invokeLater(() -> view.showError(msg));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError("Алдаа: " + e.getMessage()));
                }
            }
        }.execute();
    }
}
