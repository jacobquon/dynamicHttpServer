package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.Filter;

public class FilterTriple {
    private String path;
    private String acceptType;
    private Filter filter;
    
    public FilterTriple(String path, String acceptType, Filter filter) {
        this.path = path;
        this.acceptType = acceptType;
        this.filter = filter;
    }
    
    public String getPath() {
        return this.path;
    }
    
    public String getAcceptType() {
        return this.acceptType;
    }
    
    public Filter getFilter() {
        return this.filter;
    }
    
}
