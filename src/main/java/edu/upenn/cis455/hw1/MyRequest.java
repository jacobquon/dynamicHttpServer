package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.Request;
import edu.upenn.cis455.hw1.interfaces.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MyRequest extends Request {
    private String requestMethod;
    private String host;
    private String userAgent;
    private int port;
    private String pathInfo;
    private String url;
    private String uri;
    private String protocol;
    private String contentType;
    private String ip;
    private String body;
    private int contentLength;
    private HashMap<String, String> headers;
    private Session session;
    private Map<String, String> params;
    private Map<String, String> queryParams;
    private String queryString;
    private String accept;
    private Map<String, Object> attributes;
    private Map<String, String> cookies;
    private MyWebService webService;
    
    public MyRequest(String requestMethod, String pathInfo, int port, String protocol, MyWebService w) {
        this.requestMethod = requestMethod;
        this.pathInfo = pathInfo;
        this.port = port;
        this.protocol = protocol;
        this.attributes = new HashMap<>();
        this.cookies = new HashMap<>();
        this.webService = w;
    }
    
    public String accept() {
        return this.accept;
    }
    
    public void setAccept(String accept) {
        this.accept = accept;
    }
    
    public String requestMethod() {
      return this.requestMethod;
    }
    
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String host() {
      return this.host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String userAgent() {
      return this.userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int port() {
      return this.port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }

    public String pathInfo() {
      return this.pathInfo;
    }
    
    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }
    
    public String url() {
      return this.url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String uri() {
      return this.uri;
    }
    
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public String protocol() {
      return this.protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String contentType() {
      return this.contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String ip() {
      return this.ip;
    }
    
    public void setIp(String ip) {
        this.ip = ip;
    }

    public String body() {
      return this.body;
    }
    
    public void setBody(String body) {
        this.body = body;
    }

    public int contentLength() {
      return this.contentLength;
    }
    
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }
    
    public String headers(String name) {
      return this.headers.get(name);
    }
    
    public Set<String> headers() {
      return this.headers.keySet();
    }
    
    public void setHeaders(HashMap<String,String> map) {
        this.headers = map;
    }

    public boolean persistentConnection() {
        return persistent;
    }
    
    public Session session() {
        if (this.session == null) {
            return session(true);
        } 
        return this.session;
    }
    
    public void setSession(Session session) {
        this.session = session;
    }
    
    public Session session(boolean create) {
        if (create) {
            // Random string of length 30
            String id = "";
            for (int i = 0; i < 30; i++) {
                id += new Random().nextInt(10);
            }
            Session session = new MySession(id, webService);
            this.session = session;
            return session;
        } 
        return this.session;
    }
    
    public Map<String, String> params() {
      return this.params;
    }
    
    public void setParams(Map<String, String> map) {
        this.params = map;
    }
    
    public String queryParams(String param) {
        if (param == null)
            return null;

        return queryParams.get(param.toLowerCase());
    }
    
    public List<String> queryParamsValues(String param) {
        List<String> outList = new ArrayList<>();
        outList.add(this.queryParams.get(param));
        return outList;
    }
    
    public Set<String> queryParams() {
        return this.queryParams.keySet();
    }
    
    public String queryString() {
        return this.queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }
    
    public void setQueryParams(Map<String, String> map) {
        this.queryParams = map;
    }
    
    public Object attribute(String attrib) {
        return this.attributes.get(attrib);
    }

    public void attribute(String attrib, Object val) {
        this.attributes.put(attrib, val);
    }

    public Set<String> attributes() {
        return this.attributes.keySet();
    }
    
    public Map<String, String> cookies() {
        return this.cookies;
    }
    
    public void addCookie(String name, String ID) {
        this.cookies.put(name, ID);
    }
}
