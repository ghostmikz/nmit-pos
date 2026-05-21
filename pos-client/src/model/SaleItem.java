package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class SaleItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private int productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public SaleItem() {}

    public SaleItem(int productId, String productName, int quantity, BigDecimal unitPrice) {
        this.productId   = productId;
        this.productName = productName;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.subtotal    = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public int getProductId()                   { return productId; }
    public void setProductId(int productId)     { this.productId = productId; }

    public String getProductName()                  { return productName; }
    public void setProductName(String productName)  { this.productName = productName; }

    public int getQuantity()                { return quantity; }
    public void setQuantity(int quantity)   { this.quantity = quantity; this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    public BigDecimal getUnitPrice()                { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice)  { this.unitPrice = unitPrice; }

    public BigDecimal getSubtotal()                 { return subtotal; }
    public void setSubtotal(BigDecimal subtotal)    { this.subtotal = subtotal; }
}
