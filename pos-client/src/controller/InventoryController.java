package controller;

import client.SocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import model.Category;
import model.Product;
import model.User;
import view.panels.InventoryPanel;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InventoryController {

    private static final Gson GSON = new Gson();

    private final InventoryPanel view;
    private final User           user;

    public InventoryController(InventoryPanel view, User user) {
        this.view = view;
        this.user = user;
        view.setProductSaver(this::saveProduct);
        view.setProductDeleter(this::deleteProduct);
        view.setCategorySaver(this::saveCategory);
        view.setCategoryDeleter(this::deleteCategory);
        view.setImageLoader(this::loadImage);
        loadData();
        // Reload whenever the panel is navigated to so stock is always fresh
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && view.isShowing()) {
                loadData();
            }
        });
    }

    // ── Initial load ──────────────────────────────────────────────────────────

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
                    error = "Серверт холбогдож чадсангүй: " + e.getMessage();
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

    // ── Product save (add or update) ──────────────────────────────────────────

    private void saveProduct(Product p, byte[] imageBytes, boolean imageChanged, Runnable onSuccess) {
        boolean isNew = p.getId() == 0;
        new SwingWorker<JsonObject, Void>() {
            int newId = -1;

            @Override protected JsonObject doInBackground() throws Exception {
                JsonObject res = SocketClient.getInstance()
                    .send(isNew ? "ADD_PRODUCT" : "UPDATE_PRODUCT", user.getToken(), p);
                if ("OK".equals(res.get("status").getAsString()) && isNew) {
                    Object data = res.get("data");
                    if (data instanceof com.google.gson.JsonPrimitive jp) newId = jp.getAsInt();
                }
                return res;
            }

            @Override protected void done() {
                try {
                    JsonObject res = get();
                    if (!"OK".equals(res.get("status").getAsString())) {
                        view.showError(res.get("message").getAsString());
                        return;
                    }
                    int targetId = isNew ? newId : p.getId();
                    if (imageChanged && imageBytes != null && targetId > 0) {
                        uploadImage(targetId, imageBytes, onSuccess);
                    } else {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }

    // ── Product delete ────────────────────────────────────────────────────────

    private void deleteProduct(int productId, Runnable onSuccess) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception {
                return SocketClient.getInstance().send("DELETE_PRODUCT", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    JsonObject res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
                    } else {
                        SwingUtilities.invokeLater(() -> view.showError(res.get("message").getAsString()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }

    // ── Image load ────────────────────────────────────────────────────────────

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

    // ── Image upload ──────────────────────────────────────────────────────────

    private void uploadImage(int productId, byte[] bytes, Runnable onSuccess) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("image", Base64.getEncoder().encodeToString(bytes));
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                SocketClient.getInstance().send("UPDATE_PRODUCT_IMAGE", user.getToken(), data);
                return null;
            }
            @Override protected void done() {
                SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
            }
        }.execute();
    }

    // ── Category save ─────────────────────────────────────────────────────────

    private void saveCategory(Category c, boolean isNew, Runnable onSuccess) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", c.getName());
        if (!isNew) data.put("id", c.getId());

        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception {
                return SocketClient.getInstance()
                    .send(isNew ? "ADD_CATEGORY" : "UPDATE_CATEGORY", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    JsonObject res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
                    } else {
                        SwingUtilities.invokeLater(() -> view.showError(res.get("message").getAsString()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }

    // ── Category delete ───────────────────────────────────────────────────────

    private void deleteCategory(int catId, Runnable onSuccess, Consumer<String> onError) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", catId);
        new SwingWorker<JsonObject, Void>() {
            @Override protected JsonObject doInBackground() throws Exception {
                return SocketClient.getInstance().send("DELETE_CATEGORY", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    JsonObject res = get();
                    if ("OK".equals(res.get("status").getAsString())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
                    } else {
                        SwingUtilities.invokeLater(() -> onError.accept(res.get("message").getAsString()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> onError.accept(e.getMessage()));
                }
            }
        }.execute();
    }
}
