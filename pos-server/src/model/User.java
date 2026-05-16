package model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // admin, manager, cashier
    private boolean isActive;
    private Integer createdBy;

    public User() {}

    public User(int id, String username, String fullName, String role, boolean isActive) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.isActive = isActive;
    }

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
