package model;

import java.math.BigDecimal;

public class Product {
    private int id;
    private String barcode;
    private String name;
    private int categoryId;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal costPrice;
    private int stockQuantity;
    private String unit;
    private String expiryDate;
    private boolean isActive;
    private boolean hasImage;
    private int lowStockAlert;

    public Product() {}

    public int getId()                      { return id; }
    public void setId(int id)               { this.id = id; }

    public String getBarcode()              { return barcode; }
    public void setBarcode(String barcode)  { this.barcode = barcode; }

    public String getName()                 { return name; }
    public void setName(String name)        { this.name = name; }

    public int getCategoryId()                  { return categoryId; }
    public void setCategoryId(int categoryId)   { this.categoryId = categoryId; }

    public String getCategoryName()                     { return categoryName; }
    public void setCategoryName(String categoryName)    { this.categoryName = categoryName; }

    public BigDecimal getPrice()                { return price; }
    public void setPrice(BigDecimal price)      { this.price = price; }

    public BigDecimal getCostPrice()                { return costPrice; }
    public void setCostPrice(BigDecimal costPrice)  { this.costPrice = costPrice; }

    public int getStockQuantity()                       { return stockQuantity; }
    public void setStockQuantity(int stockQuantity)     { this.stockQuantity = stockQuantity; }

    public String getUnit()             { return unit; }
    public void setUnit(String unit)    { this.unit = unit; }

    public String getExpiryDate()                       { return expiryDate; }
    public void setExpiryDate(String expiryDate)        { this.expiryDate = expiryDate; }

    public boolean isActive()               { return isActive; }
    public void setActive(boolean active)   { isActive = active; }

    public boolean isHasImage()                { return hasImage; }
    public void setHasImage(boolean hasImage)  { this.hasImage = hasImage; }

    public int getLowStockAlert()                      { return lowStockAlert; }
    public void setLowStockAlert(int lowStockAlert)    { this.lowStockAlert = lowStockAlert; }
}
