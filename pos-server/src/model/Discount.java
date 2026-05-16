package model;

import java.math.BigDecimal;

public class Discount {
    private int id;
    private String name;
    private String type; // percentage, fixed
    private BigDecimal value;
    private boolean isActive;

    public Discount() {}

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }

    public String getType()             { return type; }
    public void setType(String type)    { this.type = type; }

    public BigDecimal getValue()                { return value; }
    public void setValue(BigDecimal value)      { this.value = value; }

    public boolean isActive()               { return isActive; }
    public void setActive(boolean active)   { isActive = active; }
}
