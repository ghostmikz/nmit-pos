package handler;

import dao.SaleDAO;
import model.Request;
import model.Response;
import model.Sale;
import model.SaleItem;
import model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class SaleHandler {

    private static final SaleDAO DAO = new SaleDAO();

    @SuppressWarnings("unchecked")
    public static Response create(Request req, User user) {
        try {
            Map<String, Object> d = (Map<String, Object>) req.getData();
            Sale sale = new Sale();
            sale.setUserId(user.getId());
            sale.setSubtotal((BigDecimal) d.get("subtotal"));
            sale.setTotal((BigDecimal) d.get("total"));
            sale.setItems((List<SaleItem>) d.get("items"));
            sale.setPayments((List<Map<String, Object>>) d.get("payments"));

            if (sale.getPayments() == null || sale.getPayments().isEmpty())
                return Response.error("No payment methods provided");

            String receipt = "RCP-" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS"));
            sale.setReceiptNumber(receipt);

            int saleId = DAO.createSale(sale);
            return Response.ok(Map.of("saleId", saleId, "receiptNumber", receipt));
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static Response processRefund(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Access denied");
        try {
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int saleId        = ((Number) data.get("saleId")).intValue();
            String reason     = (String) data.get("reason");
            BigDecimal amount = new BigDecimal(data.get("refundAmount").toString());
            DAO.processRefund(saleId, user.getId(), reason, amount);
            return Response.ok("Refund successful");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
