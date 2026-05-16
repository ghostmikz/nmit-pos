package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Sale {
    private int id;
    private String receiptNumber;
    private int userId;
    private int paymentMethodId;
    private Integer discountId;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String notes;
    private boolean isRefunded;
    private LocalDateTime createdAt;
    private List<SaleItem> items;

    public Sale() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }

    public String getReceiptNumber()                        { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber)      { this.receiptNumber = receiptNumber; }

    public int getUserId()                      { return userId; }
    public void setUserId(int userId)           { this.userId = userId; }

    public int getPaymentMethodId()                         { return paymentMethodId; }
    public void setPaymentMethodId(int paymentMethodId)     { this.paymentMethodId = paymentMethodId; }

    public Integer getDiscountId()              { return discountId; }
    public void setDiscountId(Integer discountId) { this.discountId = discountId; }

    public BigDecimal getSubtotal()                 { return subtotal; }
    public void setSubtotal(BigDecimal subtotal)    { this.subtotal = subtotal; }

    public BigDecimal getDiscountAmount()                       { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount)    { this.discountAmount = discountAmount; }

    public BigDecimal getTotal()                { return total; }
    public void setTotal(BigDecimal total)      { this.total = total; }

    public String getNotes()                { return notes; }
    public void setNotes(String notes)      { this.notes = notes; }

    public boolean isRefunded()                 { return isRefunded; }
    public void setRefunded(boolean refunded)   { isRefunded = refunded; }

    public LocalDateTime getCreatedAt()                     { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }

    public List<SaleItem> getItems()                { return items; }
    public void setItems(List<SaleItem> items)      { this.items = items; }
}
