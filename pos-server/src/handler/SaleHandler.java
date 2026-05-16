package handler;

import com.google.gson.Gson;
import dao.SaleDAO;
import model.Request;
import model.Response;
import model.Sale;
import model.User;

import java.math.BigDecimal;
import java.util.Map;

public class SaleHandler {

    private static final Gson gson = new Gson();

    public static Response create(Request req, User user) {
        try {
            Sale sale = gson.fromJson(gson.toJson(req.getData()), Sale.class);
            sale.setUserId(user.getId());

            // Generate receipt number: RCP-YYYYMMDD-HHMMSS
            String receipt = "RCP-" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            sale.setReceiptNumber(receipt);

            int saleId = new SaleDAO().createSale(sale);
            return Response.ok(Map.of("saleId", saleId, "receiptNumber", receipt));
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    public static Response processRefund(Request req, User user) {
        if (!user.getRole().equals("admin") && !user.getRole().equals("manager"))
            return Response.error("Зөвхөн менежер буцаалт хийх боломжтой");
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) req.getData();
            int saleId            = ((Double) data.get("saleId")).intValue();
            String reason         = (String) data.get("reason");
            BigDecimal amount     = new BigDecimal(data.get("refundAmount").toString());
            new SaleDAO().processRefund(saleId, user.getId(), reason, amount);
            return Response.ok("Буцаалт амжилттай");
        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }
}
