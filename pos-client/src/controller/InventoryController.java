package controller;

import client.SocketClient;
import model.Category;
import model.Product;
import model.Response;
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
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && view.isShowing()) {
                loadData();
            }
        });
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

                    Response r1 = SocketClient.getInstance().send("GET_CATEGORIES", user.getToken());
                    if ("OK".equals(r1.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<Category> c = (List<Category>) r1.getData();
                        categories = c;
                    }

                    Response r2 = SocketClient.getInstance().send("GET_PRODUCTS", user.getToken());
                    if ("OK".equals(r2.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<Product> p = (List<Product>) r2.getData();
                        products = p;
                    }
                } catch (Exception e) {
                    error = e.getMessage();
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

    private void saveProduct(Product p, byte[] imageBytes, boolean imageChanged, Runnable onSuccess) {
        boolean isNew = p.getId() == 0;
        new SwingWorker<Response, Void>() {
            int newId = -1;

            @Override protected Response doInBackground() throws Exception {
                Response res = SocketClient.getInstance()
                    .send(isNew ? "ADD_PRODUCT" : "UPDATE_PRODUCT", user.getToken(), p);
                if ("OK".equals(res.getStatus()) && isNew && res.getData() instanceof Integer id)
                    newId = id;
                return res;
            }

            @Override protected void done() {
                try {
                    Response res = get();
                    if (!"OK".equals(res.getStatus())) {
                        view.showError(res.getMessage());
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

    private void deleteProduct(int productId, Runnable onSuccess) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return SocketClient.getInstance().send("DELETE_PRODUCT", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
                    } else {
                        SwingUtilities.invokeLater(() -> view.showError(res.getMessage()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }

    private void loadImage(int productId, Consumer<String> callback) {
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() throws Exception {
                Map<String, Object> data = new HashMap<>();
                data.put("productId", productId);
                Response res = SocketClient.getInstance().send("GET_PRODUCT_IMAGE", user.getToken(), data);
                if ("OK".equals(res.getStatus()) && res.getData() != null)
                    return (String) res.getData();
                return null;
            }
            @Override protected void done() {
                try { callback.accept(get()); } catch (Exception e) { callback.accept(null); }
            }
        }.execute();
    }

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

    private void saveCategory(Category c, boolean isNew, Runnable onSuccess) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", c.getName());
        if (!isNew) data.put("id", c.getId());

        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return SocketClient.getInstance()
                    .send(isNew ? "ADD_CATEGORY" : "UPDATE_CATEGORY", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
                    } else {
                        SwingUtilities.invokeLater(() -> view.showError(res.getMessage()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }

    private void deleteCategory(int catId, Runnable onSuccess, Consumer<String> onError) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", catId);
        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return SocketClient.getInstance().send("DELETE_CATEGORY", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        SwingUtilities.invokeLater(() -> { onSuccess.run(); loadData(); });
                    } else {
                        SwingUtilities.invokeLater(() -> onError.accept(res.getMessage()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> onError.accept(e.getMessage()));
                }
            }
        }.execute();
    }
}
