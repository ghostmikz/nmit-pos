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
        view.setPaymentHandler(this::pay);
        view.setSplitPaymentHandler(this::splitPay);
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
                            loadData();
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

    private void splitPay(List<CartItem> items, int method1Id, BigDecimal amount1,
                          int method2Id, BigDecimal amount2, BigDecimal total, Runnable onSuccess) {
        List<SaleItem> saleItems = new ArrayList<>();
        for (CartItem ci : items) saleItems.add(ci.toSaleItem());

        String m1 = switch (method1Id) { case 2 -> "Card"; case 3 -> "QPay"; case 4 -> "MonPay"; default -> "Cash"; };
        String m2 = switch (method2Id) { case 2 -> "Card"; case 3 -> "QPay"; case 4 -> "MonPay"; default -> "Cash"; };
        String notes = "Split: " + m1 + " ₮" + amount1.toPlainString() + " + " + m2 + " ₮" + amount2.toPlainString();

        Map<String, Object> data = new HashMap<>();
        data.put("paymentMethodId", method1Id);
        data.put("subtotal",        total);
        data.put("discountId",      null);
        data.put("discountAmount",  BigDecimal.ZERO);
        data.put("total",           total);
        data.put("notes",           notes);
        data.put("items",           saleItems);

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
                            loadData();
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
