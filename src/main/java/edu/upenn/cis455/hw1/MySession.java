package edu.upenn.cis455.hw1;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import edu.upenn.cis455.hw1.interfaces.Session;

public class MySession extends Session {
    private String id;
    private long creationTime;
    private long lastAccessedTime;
    private int inactiveInterval;
    private HashMap<String, Object> attributes;
    private MyWebService webService;
    
    public MySession(String id, MyWebService w) {
        this.id = id;
        this.creationTime = (new Date()).getTime();
        this.webService = w;
        this.attributes = new HashMap<>();
    }
    
    
    public String id() {
        access();
        return this.id;
    }
    
    public long creationTime() {
        access();
      return this.creationTime;
    }
    
    public long lastAccessedTime() {
        long time = this.lastAccessedTime;
        access();
        return time;
    }
    
    public void invalidate() {
        webService.sessionMap.remove(this.id);
    }
    
    public int maxInactiveInterval() {
        access();
        return this.inactiveInterval;
    }
    
    public void maxInactiveInterval(int interval) {
        access();
        this.inactiveInterval = interval;
    }
    
    public void access() {
        this.lastAccessedTime = (new Date()).getTime();
    }
    
    public void attribute(String name, Object value) {
        access();
        this.attributes.put(name, value);
    }
    
    public Object attribute(String name) {
        access();
        return this.attributes.get(name);
    }
    
    public Set<String> attributes() {
        access();
        return this.attributes.keySet();
    }
    
    public void removeAttribute(String name) {
        access();
        this.attributes.remove(name);
    }
}
