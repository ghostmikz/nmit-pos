package model;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private String status;
    private String message;
    private Object data;

    public Response() {}

    public String getStatus()            { return status; }
    public String getMessage()           { return message; }
    public Object getData()              { return data; }
}
