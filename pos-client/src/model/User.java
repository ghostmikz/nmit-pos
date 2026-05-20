package model;

public class User {
    private int id;
    private String username;
    private String fullName;
    private String role;
    private String token;
    private boolean isActive;

    public User() {}

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public String getUsername()                     { return username; }
    public void setUsername(String username)         { this.username = username; }

    public String getFullName()                 { return fullName; }
    public void setFullName(String fullName)    { this.fullName = fullName; }

    public String getRole()             { return role; }
    public void setRole(String role)    { this.role = role; }

    public String getToken()                { return token; }
    public void setToken(String token)      { this.token = token; }

    public boolean isActive()               { return isActive; }
    public void setActive(boolean active)   { isActive = active; }

    public boolean isAdmin()    { return "admin".equals(role); }
    public boolean isManager()  { return "manager".equals(role) || isAdmin(); }
    public boolean isCashier()  { return true; }
}
