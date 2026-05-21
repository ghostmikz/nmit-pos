package model;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // admin, manager, cashier
    private boolean isActive;
    private Integer createdBy;

    public User() {}

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public String getUsername()                     { return username; }
    public void setUsername(String username)         { this.username = username; }

    public String getPasswordHash()                     { return passwordHash; }
    public void setPasswordHash(String passwordHash)    { this.passwordHash = passwordHash; }

    public String getFullName()                 { return fullName; }
    public void setFullName(String fullName)    { this.fullName = fullName; }

    public String getRole()             { return role; }
    public void setRole(String role)    { this.role = role; }

    public boolean isActive()               { return isActive; }
    public void setActive(boolean active)   { isActive = active; }

    public Integer getCreatedBy()               { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}
