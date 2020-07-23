package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.Route;

public class RouteTriple {
    private String method;
    private String path;
    private Route route;
    
    public RouteTriple(String method, String path, Route route) {
        this.method = method;
        this.path = path;
        this.route = route;
    }
    
    public String getMethod() {
        return this.method;
    }
    
    public String getPath() {
        return this.path;
    }
    
    public Route getRoute() {
        return this.route;
    }
    
}
