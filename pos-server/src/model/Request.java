package model;

// Client → Server JSON wrapper
// {"action":"LOGIN","token":null,"data":{...}}
public class Request {
    private String action;
    private String token;
    private Object data;

    public Request() {}

    public String getAction()               { return action; }
    public void setAction(String action)    { this.action = action; }

    public String getToken()                { return token; }
    public void setToken(String token)      { this.token = token; }

    public Object getData()             { return data; }
    public void setData(Object data)    { this.data = data; }
}
