package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Sale implements Serializable {
    private static final long serialVersionUID = 2L;
    private int id;
    private String receiptNumber;
    private String cashierName;
    private String paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal total;
    private boolean isRefunded;
    private String createdAt;
    private String notes;

    public Sale() {}

    public int getId()                         { return id; }
    public void setId(int id)                  { this.id = id; }

    public String getReceiptNumber()                       { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber)     { this.receiptNumber = receiptNumber; }

    public String getCashierName()                     { return cashierName; }
    public void setCashierName(String cashierName)     { this.cashierName = cashierName; }

    public String getPaymentMethod()                   { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getSubtotal()                { return subtotal; }
    public void setSubtotal(BigDecimal subtotal)   { this.subtotal = subtotal; }

    public BigDecimal getTotal()               { return total; }
    public void setTotal(BigDecimal total)     { this.total = total; }

    public boolean isRefunded()                { return isRefunded; }
    public void setRefunded(boolean refunded)  { isRefunded = refunded; }

    public String getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(String createdAt)     { this.createdAt = createdAt; }

    public String getNotes()               { return notes; }
    public void setNotes(String notes)     { this.notes = notes; }
}
