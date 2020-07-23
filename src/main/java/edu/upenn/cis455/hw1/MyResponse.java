package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.Response;

public class MyResponse extends Response {
    
    private int contentLength;
    private String headers;
    private boolean redirected;
    
    public MyResponse() {
        this.contentType = "text/html";
        this.headers = "";
        this.redirected = false;
    }
    
    public String getHeaders() {
        return this.headers;
    }
    
    public String getStatusText() {
        if (this.statusCode == 200) {
            return " Ok";
        } else if (this.statusCode == 404) {
            return " File Not Found";
        } else if (this.statusCode == 501) {
            return " Not Implemented";
        } else if (this.statusCode == 100) {
            return " Continue";
        } else if (this.statusCode == 412) {
            return " Precondition Failed";
        } else if (this.statusCode == 304) {
            return " Not Modified";
        } else if (this.statusCode == 400) {
            return " Bad Request";
        } else if (this.statusCode == 300) {
            return " Multiple Choices";
        } else if (this.statusCode == 301) {
            return " Moved Permanently";
        } else if (this.statusCode == 302) {
            return " Found";
        } else if (this.statusCode == 303) {
            return " See Other";
        } else if (this.statusCode == 305) {
            return " Use Proxy";
        } else if (this.statusCode == 307) {
            return " Temporary Redirect";
        } else {
            return " ";
        }
    }

    public void header(String header, String value) {
        this.headers += header + ": " + value + "\r\n";
    }
    
    public int contentLength() {
        return this.contentLength;
    }
    
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
    
    public void redirect(String location) {
        this.redirected = true;
        this.statusCode = 302;
        this.body("<!DOCTYPE html><html><body><h1>302 Found</h1><p1>You have been redirected to " + location +"</p1></body></html>");
        this.contentType = "text/html";
        this.header("Location", location);
    }
    
    public void redirect(String location, int httpStatusCode) {
        this.redirected = true;
        this.statusCode = httpStatusCode;
        this.body("<!DOCTYPE html><html><body><h1>" + httpStatusCode + getStatusText() + "</h1><p1>You have been redirected to " + location +"</p1></body></html>");
        this.contentType = "text/html";
        this.header("Location", location);
    }
    
    public boolean getRedirected() {
        return this.redirected;
    }
    
    public void cookie(String name, String value) {
        header("Set-Cookie", name + "=" + value);
    }
    
    public void cookie(String name, String value, int maxAge) {
        header("Set-Cookie", name + "=" + value + "; Max-Age=" + maxAge);
    }

    public void cookie(String path, String name, String value) {
        header("Set-Cookie", name + "=" + value + "; Path=" + path);
    }
    
    public void cookie(String path, String name, String value, int maxAge) {
        header("Set-Cookie", name + "=" + value + "; Path=" + path + "; Max-Age=" + maxAge);
    }

    public void removeCookie(String name) {
        header("Set-Cookie", name + "=; Expires=Mon, 07 Nov 1994 08:49:37 GMT");
    }
    
    public void removeCookie(String path, String name) {
        header("Set-Cookie", name + "=; Path=" + path + "; Expires=Mon, 07 Nov 1994 08:49:37 GMT");
    }
}
