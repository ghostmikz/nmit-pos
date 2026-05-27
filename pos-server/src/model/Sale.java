package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class Sale implements Serializable {
    private static final long serialVersionUID = 2L;
    private int id;
    private String receiptNumber;
    private int userId;
    private int paymentMethodId;
    private BigDecimal subtotal;
    private BigDecimal total;
    private String notes;
    private boolean isRefunded;
    private String createdAt;
    private String cashierName;
    private String paymentMethod;
    private List<SaleItem> items;
    private List<Map<String, Object>> payments;

    public Sale() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getReceiptNumber()                        { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber)      { this.receiptNumber = receiptNumber; }

    public int getUserId()                      { return userId; }
    public void setUserId(int userId)           { this.userId = userId; }

    public int getPaymentMethodId()                         { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId)     { this.paymentMethodId = paymentMethodId; }

    public BigDecimal getSubtotal()                 { return subtotal; }
    public void setSubtotal(BigDecimal subtotal)    { this.subtotal = subtotal; }

    public BigDecimal getTotal()                { return total; }
    public void setTotal(BigDecimal total)      { this.total = total; }

    public String getNotes()                { return notes; }
    public void setNotes(String notes)      { this.notes = notes; }

    public boolean isRefunded()                 { return isRefunded; }
    public void setRefunded(boolean refunded)   { isRefunded = refunded; }

    public String getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(String createdAt)       { this.createdAt = createdAt; }

    public String getCashierName()                       { return cashierName; }
    public void setCashierName(String cashierName)       { this.cashierName = cashierName; }

    public String getPaymentMethod()                     { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod)   { this.paymentMethod = paymentMethod; }

    public List<SaleItem> getItems()                { return items; }
    public void setItems(List<SaleItem> items)      { this.items = items; }

    public List<Map<String, Object>> getPayments()              { return payments; }
    public void setPayments(List<Map<String, Object>> payments) { this.payments = payments; }
}
