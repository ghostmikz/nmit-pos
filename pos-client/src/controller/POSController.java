package controller;

import client.SocketClient;
import i18n.I18n;
import model.*;
import view.panels.POSPanel;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.*;

public class POSController {

    private final POSPanel view;
    private final User     user;

    public POSController(POSPanel view, User user) {
        this.view = view;
        this.user = user;
        view.setPaymentHandler(this::createSale);
        view.setImageLoader(this::loadImage);
        loadData();
        view.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0
                    && view.isShowing()) {
                loadData();
            }
        });
    }

    private boolean staticDataLoaded = false;

    private void loadData() { loadData(false); }

    private void loadData(boolean productsOnly) {
        new SwingWorker<Void, Void>() {
            List<Product>             products        = new ArrayList<>();
            List<Category>            categories      = new ArrayList<>();
            List<Map<String, Object>> paymentMethods  = new ArrayList<>();
            String error;

            @Override protected Void doInBackground() {
                try {
                    if (!SocketClient.getInstance().isConnected())
                        SocketClient.getInstance().connect();

                    if (!productsOnly) {
                        Response r1 = SocketClient.getInstance().send("GET_CATEGORIES", user.getToken());
                        if ("OK".equals(r1.getStatus())) {
                            @SuppressWarnings("unchecked")
                            List<Category> c = (List<Category>) r1.getData();
                            categories = c;
                        }
                    }

                    Response r2 = SocketClient.getInstance().send("GET_PRODUCTS", user.getToken());
                    if ("OK".equals(r2.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<Product> p = (List<Product>) r2.getData();
                        products = p;
                    }

                    if (!productsOnly) {
                        Response r3 = SocketClient.getInstance().send("GET_PAYMENT_METHODS", user.getToken());
                        if ("OK".equals(r3.getStatus()) && r3.getData() instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> pm = (List<Map<String, Object>>) r3.getData();
                            paymentMethods = pm;
                        }
                    }
                } catch (Exception e) {
                    error = e.getMessage();
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) { view.showError(error); return; }
                if (!productsOnly) {
                    view.setCategories(categories);
                    view.setPaymentMethods(paymentMethods);
                    staticDataLoaded = true;
                }
                view.setProducts(products);
            }
        }.execute();
    }

    private void loadImage(int productId, java.util.function.Consumer<String> callback) {
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

    private void createSale(List<CartItem> items, List<Map<String, Object>> payments,
                            BigDecimal total, Runnable onSuccess) {
        List<SaleItem> saleItems = new ArrayList<>();
        for (CartItem ci : items) saleItems.add(ci.toSaleItem());

        Map<String, Object> data = new HashMap<>();
        data.put("subtotal", total);
        data.put("total",    total);
        data.put("items",    saleItems);
        data.put("payments", payments);

        new SwingWorker<Response, Void>() {
            @Override protected Response doInBackground() throws Exception {
                return SocketClient.getInstance().send("CREATE_SALE", user.getToken(), data);
            }
            @Override protected void done() {
                try {
                    Response res = get();
                    if ("OK".equals(res.getStatus())) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> d = (Map<String, Object>) res.getData();
                        String receipt = (String) d.get("receiptNumber");
                        SwingUtilities.invokeLater(() -> {
                            onSuccess.run();
                            loadData(true);
                            JOptionPane.showMessageDialog(view,
                                    I18n.t("pos.receipt.prefix") + " " + receipt,
                                    I18n.t("pos.success"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> view.showError(res.getMessage()));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> view.showError(e.getMessage()));
                }
            }
        }.execute();
    }
}
