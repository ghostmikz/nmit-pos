package model;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private String action;
    private String token;
    private Object data;

    public Request() {}

    public String getAction() { return action; }
    public String getToken()  { return token; }
    public Object getData()   { return data; }
}
