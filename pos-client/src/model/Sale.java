package model;

import java.math.BigDecimal;

public class Sale {
    private int id;
    private String receiptNumber;
    private String cashierName;
    private String paymentMethod;
    private String discountName;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private boolean isRefunded;
    private String createdAt;

    public Sale() {}

    public int getId()                         { return id; }
    public void setId(int id)                  { this.id = id; }

    public String getReceiptNumber()                       { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber)     { this.receiptNumber = receiptNumber; }

    public String getCashierName()                     { return cashierName; }
    public void setCashierName(String cashierName)     { this.cashierName = cashierName; }

    public String getPaymentMethod()                   { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDiscountName()                    { return discountName; }
    public void setDiscountName(String discountName)   { this.discountName = discountName; }

    public BigDecimal getSubtotal()                { return subtotal; }
    public void setSubtotal(BigDecimal subtotal)   { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount()                      { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount)   { this.discountAmount = discountAmount; }

    public BigDecimal getTotal()               { return total; }
    public void setTotal(BigDecimal total)     { this.total = total; }

    public boolean isRefunded()                { return isRefunded; }
    public void setRefunded(boolean refunded)  { isRefunded = refunded; }

    public String getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(String createdAt)     { this.createdAt = createdAt; }
}
