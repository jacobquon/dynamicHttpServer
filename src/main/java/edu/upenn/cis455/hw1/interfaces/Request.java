package edu.upenn.cis455.hw1.interfaces;

import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Request {
    /**
     * Indicates we have a persistent HTTP 1.1 connection
     */
    protected boolean persistent = false;
    
    /**
     * The request method (GET, POST, ...)
     */
    public abstract String requestMethod();

    /**
     * @return The host
     */
    public abstract String host();  
    
    /**
     * @return The user-agent
     */
    public abstract String userAgent();
    
    /**
     * @return The server port
     */
    public abstract int port();
    
    /**
     * @return The path
     */
    public abstract String pathInfo();
    
    /**
     * @return The URL
     */
    public abstract String url();
    
    /**
     * @return The URI up to the query string
     */
    public abstract String uri();
    
    /**
     * @return The protocol name and version from the request
     */
    public abstract String protocol();

    /**
     * @return The MIME type of the body
     */
    public abstract String contentType();
    
    /**
     * @return The client's IP address
     */
    public abstract String ip();
    
    /**
     * @return The request body sent by the client
     */
    public abstract String body();
    
    /**
     * @return The length of the body
     */
    public abstract int contentLength();
    
    /**
     * @return Get the item from the header
     */
    public abstract String headers(String name);
    
    public abstract Set<String> headers();
    
    /**
     * Indicates we have a persistent HTTP 1.1 connection
     */
    public boolean persistentConnection() {
        return persistent;
    }
    
    /**
     * Sets whether we have a persistent HTTP 1.1 connection
     */
    public void persistentConnection(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * @return Gets the session associated with this request
     */
    public abstract Session session();
    
    /**
     * @return Gets or creates a session for this request
     */
    public abstract Session session(boolean create);
    
    /**
     * @return  a map containing the route parameters
     */
    public abstract Map<String, String> params();
    
    /**
     * @return  the named parameter
     * Example: parameter 'name' from the following pattern: (get '/hello/:name')
     */
    public String params(String param) {
        if (param == null)
            return null;
        
        if (param.startsWith(":"))
            return params().get(param.toLowerCase());
        else
            return params().get(':' + param.toLowerCase());
    }
    
    /**
     * @return Query parameter from the URL
     */
    public abstract String queryParams(String param);
    
    public String queryParamOrDefault(String param, String def) {
        String ret = queryParams(param);
        
        return (ret == null) ? def : ret;
    }
    
    /**
     * @return Get the list of values for the query parameter
     */
    public abstract List<String> queryParamsValues(String param);
    
    public abstract Set<String> queryParams();
    
    /**
     * @return The raw query string
     */
    public abstract String queryString();
    
    /**
     * Add an attribute to the request (eg in a filter)
     */
    public abstract void attribute(String attrib, Object val);
    
    /**
     * @return Gets an attribute attached to the request
     */
    public abstract Object attribute(String attrib);
    
    /**
     * @return All attributes attached to the request
     */
    public abstract Set<String> attributes();
    
    public abstract Map<String, String> cookies();
    
    public String cookie(String name) {
        if (name == null || cookies() == null)
            return null;
        else
            return cookies().get(name);
    }
}
