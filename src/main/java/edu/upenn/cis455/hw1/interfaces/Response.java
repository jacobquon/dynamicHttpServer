package edu.upenn.cis455.hw1.interfaces;

import java.io.UnsupportedEncodingException;

public abstract class Response {
    protected int statusCode = 200;
    protected byte[] body;
    protected String contentType = null;//"text/plain";
    
    public int status() {
        return statusCode;
    }
    
    public void status(int statusCode) {
        this.statusCode = statusCode;
    }
    
    public String body() {
        try {
            return body == null ? "" : new String(body, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }
    
    public byte[] bodyRaw() {
        return body;
    }
    
    public void bodyRaw(byte[] b) {
        body = b;
    }
    
    public void body(String body) {
        this.body = body == null ? null : body.getBytes();
    }
    
    public String type() {
        return contentType;
    }
    
    public void type(String contentType) {
        this.contentType = contentType;
    }
    
    public abstract String getHeaders();

    /**
     * Add a header key/value
     */
    public abstract void header(String header, String value);
    
    /**
     * Trigger an HTTP redirect to a new location
     */
    public abstract void redirect(String location);
    
    /**
     * Trigger a redirect with a specific HTTP 3xx status code
     */
    public abstract void redirect(String location, int httpStatusCode);
    
    public abstract void cookie(String name, String value);
    
    public abstract void cookie(String name, String value, int maxAge);

    public abstract void cookie(String path, String name, String value);
    
    public abstract void cookie(String path, String name, String value, int maxAge);

    public abstract void removeCookie(String name);
    
    public abstract void removeCookie(String path, String name);
}
