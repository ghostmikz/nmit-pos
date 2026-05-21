package model;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private String status;
    private String message;
    private Object data;

    public Response() {}

    public static Response ok(Object data) {
        Response r = new Response();
        r.status = "OK";
        r.data = data;
        return r;
    }

    public static Response error(String message) {
        Response r = new Response();
        r.status = "ERROR";
        r.message = message;
        return r;
    }

    public String getStatus()               { return status; }
    public void setStatus(String status)    { this.status = status; }

    public String getMessage()              { return message; }
    public void setMessage(String message)  { this.message = message; }

    public Object getData()             { return data; }
    public void setData(Object data)    { this.data = data; }
}
