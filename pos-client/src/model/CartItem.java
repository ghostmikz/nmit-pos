package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Product product;
    private int quantity;

    public CartItem(Product product) {
        this.product  = product;
        this.quantity = 1;
    }

    public Product getProduct()    { return product; }
    public int     getQuantity()   { return quantity; }

    public void increment() { quantity++; }
    public void decrement() { if (quantity > 1) quantity--; }

    public BigDecimal getSubtotal() {
        return product.getPrice().multiply(BigDecimal.valueOf(quantity));
    }

    public SaleItem toSaleItem() {
        return new SaleItem(product.getId(), product.getName(), quantity, product.getPrice());
    }
}
