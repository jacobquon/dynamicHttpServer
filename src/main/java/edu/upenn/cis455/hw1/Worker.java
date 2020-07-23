package edu.upenn.cis455.hw1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;

import edu.upenn.cis455.hw1.interfaces.*;

public class Worker extends Thread{
	
    private BlockingQueue q;
    private Worker[] threads;
    private volatile boolean terminated;
    private String fullPath;
    private ServerSocket s;
    private ArrayList<RouteTriple> existingRoutes;
    private ArrayList<FilterTriple> beforeFilters;
    private ArrayList<FilterTriple> afterFilters;
    private String ip;
    private MyWebService webService;
    
    public Worker(BlockingQueue q, MyWebService w, ServerSocket s) {
        this.webService = w;
        this.q = q;
        this.threads = webService.getThreads();
        this.terminated = false;
        this.fullPath = "";
        this.s = s;
        this.existingRoutes = webService.getExistingRoutes();
        this.afterFilters = webService.getAfterFilters();
        this.beforeFilters = webService.getBeforeFilters();
        this.ip = webService.getIP();
    }
    
    public void terminate() {
        this.terminated = true;
    }
    
    public String getPath() {
        return fullPath;
    }
    
    // Find index of a registered route handler (-1 if DNE)
    public int routeSearch(String method, String path) {
        HashMap<Integer, Integer> matches = new HashMap<>();
        for (int i = 0; i < existingRoutes.size(); i++) {
            boolean pathEqual = true;
            int numWildcards = 0;
            
            // Split up path based on slashes
            String[] pathPartsComp = existingRoutes.get(i).getPath().split("\\?")[0].split("/");
            String[] pathParts = path.split("\\?")[0].split("/");
            
            // If theyre not hte same length, no way they can be equal
            if (pathPartsComp.length != pathParts.length) {
                pathEqual = false;
                continue;
            }
            
            // See if all parts in between slashes are the same
            // min to prevent outofboundsexception
            for (int j = 0; j < pathParts.length; j++) {
                // Not a wildcard match
                boolean partsEqual = pathPartsComp[j].equals(pathParts[j]);
                // Wildcard Match
                boolean starX = pathPartsComp[j].equals("*");      // * = x
                boolean xStar = pathParts[j].equals("*");          // x = *
                boolean colonX = pathPartsComp[j].startsWith(":"); // : = x
                boolean xColon = pathParts[j].startsWith(":");     // x = :
                
                // If there is a wildcard match, take note and continue
                // If normal match, continue
                // If no match, set bool equal to false and break;
                if (starX || xStar || colonX || xColon) {
                    numWildcards += 1;
                } else if (partsEqual) {
                    // go on to next part (i.e. continue to next loop)
                } else {
                    pathEqual = false;
                    break;
                }
            }
            
            // Method and path must both be equal
            if (method.equalsIgnoreCase(existingRoutes.get(i).getMethod()) && pathEqual) {
                matches.put(i, numWildcards);
            }
        }
        
        // Was there a match, if so return the match with highest specificity (lowest wildcards)
        if (matches.isEmpty()) {
            return -1;
        } else {
            // Find the match with lowest value
            Entry<Integer, Integer> minWildcards = null;
            for (Entry<Integer, Integer> entry : matches.entrySet()) {
                if (minWildcards == null || minWildcards.getValue() > entry.getValue()) {
                    minWildcards = entry;
                }
            }
            return minWildcards.getKey();
        }
    }
    
    public HashMap<String, String> getParams(String method, String path) {
        // Get the path it matched too
        String matchedPath = existingRoutes.get(routeSearch(method, path)).getPath().split("\\?")[0];
        
        // Split up the 2 paths
        String[] pathParts = path.split("\\?")[0].split("/");
        String[] matchedParts = matchedPath.split("/");
        
        // Go through all the parts and record any which are parameters
        HashMap<String, String> params = new HashMap<>();
        for (int i = 0; i < matchedParts.length; i++) {
            if (matchedParts[i].startsWith(":")) {
                params.put(matchedParts[i], pathParts[i]);
            }
        }
        return params;
    }
    
    public HashMap<String, String> getQueryParams(String method, String path, List<String> clientHeaders, String body) {
        HashMap<String, String> queryParams = new HashMap<>();
        
        // Extract the query string
        String[] splitPath = path.split("\\?");
        if (splitPath.length > 1) {
            String queryString = splitPath[1];
            
            // Extract individual queries
            String[] queries = queryString.split("&");
            for (String query : queries) {
                // Dont want empty queries messing up things
                if (query.contains((CharSequence) "=")) {
                    queryParams.put(query.split("=")[0], query.split("=")[1]);
                }
            }
        }
        
        // Check if there are queries in the body
        if (method.toUpperCase().equals("POST")) {
            for (String header : clientHeaders) {
                // Do we have the query in body header?
                if (header.toUpperCase().startsWith("CONTENT-TYPE") && 
                        header.split(":")[1].toUpperCase().trim().equals("application/x-www-form-urlencoded")) {
                    String[] bodySplit = body.split("&");
                    // Dont want empty queries messing things up
                    for (String query : bodySplit) {
                        if (query.contains((CharSequence) "=")) {
                            queryParams.put(query.split("=")[0], query.split("=")[1]);
                        }
                    }
                }
            }
        }
        
        return queryParams;
    }
    
    public List<FilterTriple> getFilters(List<FilterTriple> filterList, String path, String acceptType) {
        List<FilterTriple> outList = new ArrayList<>();
        for (FilterTriple existingFilter : filterList) {
            // If path and accepttype is empty, then it is a generic filter and is added to list of filters
            if (existingFilter.getPath().isEmpty() && existingFilter.getAcceptType().isEmpty()) {
                outList.add(existingFilter);
            } else {
                boolean pathEqual = true;
                boolean acceptTypeEqual = true;
                
                // Split up path based on slashes
                String[] existingPathParts = existingFilter.getPath().split("\\?")[0].split("/");
                String[] pathParts = path.split("\\?")[0].split("/");
                
                // Split up acceptType based on slashes
                String[] existingTypeParts = existingFilter.getAcceptType().split("/");
                String[] types = acceptType.split(",");
                
                // If theyre not hte same length, no way they can be equal
                if (existingPathParts.length != pathParts.length) {
                    pathEqual = false;
                    continue;
                }
                
                // Check against every accepted type and allow if any are good
                for (String type : types) {
                    String[] typeParts = type.split("/");
                    // assume true to start for each type
                    acceptTypeEqual = true;
                    // See if acceptType parts are the same
                    for (int j = 0; j < typeParts.length; j++) {
                        // Not a wildcard match
                        boolean partsEqual = existingTypeParts[j].equals(typeParts[j]);
                        // Wildcard Match
                        boolean starX = existingTypeParts[j].equals("*");      // * = x
                        boolean xStar = typeParts[j].equals("*");          // x = *
                        boolean colonX = existingTypeParts[j].startsWith(":"); // : = x
                        boolean xColon = typeParts[j].startsWith(":");     // x = :
                        
                        // If there is a match, continue
                        // If no match, set bool equal to false and break;
                        if (partsEqual || starX || xStar || colonX || xColon) {
                            // go on to next part (i.e. continue to next loop)
                        } else {
                            acceptTypeEqual = false;
                            break;
                        }
                    }
                    // If one of the type works, break out and move on
                    if (acceptTypeEqual) {
                        break;
                    }
                }
                
                // See if all parts in between oath slashes are the same
                for (int j = 0; j < pathParts.length; j++) {
                    // Not a wildcard match
                    boolean partsEqual = existingPathParts[j].equals(pathParts[j]);
                    // Wildcard Match
                    boolean starX = existingPathParts[j].equals("*");      // * = x
                    boolean xStar = pathParts[j].equals("*");          // x = *
                    boolean colonX = existingPathParts[j].startsWith(":"); // : = x
                    boolean xColon = pathParts[j].startsWith(":");     // x = :
                    
                    // If there is a match, continue
                    // If no match, set bool equal to false and break;
                    if (partsEqual || starX || xStar || colonX || xColon) {
                        // go on to next part (i.e. continue to next loop)
                    } else {
                        pathEqual = false;
                        break;
                    }
                }
                
                // Method and path must both be equal
                if (acceptTypeEqual && pathEqual) {
                    outList.add(existingFilter);
                }
            }
        }
        
        return outList;
    }
    
    
    public String httpDate() {
        Calendar currentTime = Calendar.getInstance();
        SimpleDateFormat httpFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US);
        httpFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return httpFormat.format(currentTime.getTime());
    }
    
	public void run() {
	    while (!terminated) {
    		try {
    		    Socket connection = q.deq();
    		    
    		    
    		    // read connection
                BufferedReader clientIn = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line = clientIn.readLine();   

                String headerOut = "";
                
                // parse the lines
                if (line == null) {
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;
                }
                String[] initLineTokens = line.split("\\s");
                
                // Faulty input handling
                if (initLineTokens.length < 3) {
                    String errorMessage = "<!DOCTYPE html><html><body><h1>400 Bad Request</h1><p1>Request was missing either the method, path, or http version</p1></body></html>";
                    
                    headerOut += "HTTP/1.1 400 Bad Request\r\n";
                    headerOut += "Date: " + httpDate() + "\r\n";
                    headerOut += "Connection: close\r\n";
                    headerOut += "Content-Type: text/html\r\n";
                    headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                    headerOut += errorMessage;
                    
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;
                    
                }
                
                String method = initLineTokens[0].toUpperCase();
                String path = initLineTokens[1];
                String httpVer = initLineTokens[2];
                if (webService.getRootDir() != null) {
                    fullPath = webService.getRootDir() + path;
                } else {
                    fullPath = path;
                }
                File file = new File(fullPath);

                
                // Gets rid of redundant names
                fullPath = file.getCanonicalPath();
               
                
                /*
                // Security handling
                if ((rootDir.startsWith(fullPath) && !rootDir.equals(fullPath))) {
                    String errorMessage = "<!DOCTYPE html><html><body><h1>403 Forbidden</h1><p1>Cannot go above root directory</p1></body></html>";
                    
                    headerOut += httpVer + " 403 Forbidden\r\n";
                    headerOut += "Date: " + httpDate() + "\r\n";
                    headerOut += "Connection: close\r\n";
                    headerOut += "Content-Type: text/html\r\n";
                    headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                    headerOut += errorMessage;
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;
                }
                */
                
                List<String> clientHeaders = new ArrayList<>();
                String clientBody = "";
                // Handling HTTP/1.1 specifics
                if (httpVer.equalsIgnoreCase("HTTP/1.1")) {
                    // Make a list of all the lines
                    while(clientIn.ready() && (line = clientIn.readLine()) != null) {
                        if (line.trim().isEmpty()) {
                            while (clientIn.ready() /*&& (line = clientIn.readLine()) != null*/) {
                                int inNum = clientIn.read();
                                clientBody += (char) inNum;
                            }
                            break;
                        }
                        clientHeaders.add(line);
                    }
                    
                    
                    // Checking for certain important headers
                    boolean host = false;
                    boolean modified = false;
                    int modifiedIndex = 0;
                    boolean unmodified = false;
                    int unmodifiedIndex = 0;
                    boolean expectContinue = false;
                    for (int i = 0; i < clientHeaders.size(); i++) {
                        if (clientHeaders.get(i).toUpperCase().startsWith("HOST:")) {
                            host = true;
                        } else if (clientHeaders.get(i).toUpperCase().startsWith("IF-MODIFIED-SINCE:")) {
                            modified = true;
                            modifiedIndex = i;
                        } else if (clientHeaders.get(i).toUpperCase().startsWith("IF-UNMODIFIED-SINCE:")) {
                            unmodified = true;
                            unmodifiedIndex = i;
                        } else if (clientHeaders.get(i).toUpperCase().startsWith("EXPECT:")) {
                            expectContinue = true;
                        }
                    }
                    
                    // Error if there is no host header
                    if (!host) {
                        String errorMessage = "<!DOCTYPE html><html><body><h1>400 Bad Request</h1><p1>Request must include a host header</p1></body></html>";
                        
                        headerOut += httpVer + " 400 Bad Request \r\n";
                        headerOut += "Date: " + httpDate() + "\r\n";
                        headerOut += "Connection: close\r\n";
                        headerOut += "Content-Type: text/html\r\n";
                        headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                        headerOut += errorMessage;
                        
                        connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                        connection.close();
                        clientIn.close();
                        fullPath = "";
                        continue;
                    }
                    
                    // Handling modified and unmodified headers
                    Date parseDate = new Date();
                    // handling the response
                    if (modified) {
                                            
                        // Potential formats for the date
                        List<SimpleDateFormat> datePatterns = new ArrayList<>();
                        datePatterns.add(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"));
                        datePatterns.add(new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z"));
                        datePatterns.add(new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"));
                        
                        // Extracting the date from the given header
                        String dateAsString = clientHeaders.get(modifiedIndex);
                        dateAsString = dateAsString.substring(dateAsString.indexOf(":")+1);
                        dateAsString = dateAsString.trim();
    
                        // Parse the string into a Date
                        for(int j = 0; j < datePatterns.size(); j++) {
                            try {
                                parseDate = datePatterns.get(j).parse(dateAsString);
                            } catch (ParseException e) {}   
                        }
                        
                        // handling the response
                        if (parseDate.after(new Date(file.lastModified()))) {
                            String errorMessage = "<!DOCTYPE html><html><body><h1>304 Not Modified</h1><p1>File has not been modified since given date</p1></body></html>";
                            
                            headerOut += httpVer + " 304 Not Modified\r\n";
                            headerOut += "Date: " + httpDate() + "\r\n";
                            headerOut += "Connection: close\r\n";
                            headerOut += "Content-Type: text/html\r\n";
                            headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                            headerOut += errorMessage;
                            
                            connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                            connection.close();
                            clientIn.close();
                            fullPath = "";
                            continue;
                        }
                    } else if (unmodified) {
                                            
                        // Potential formats for the date
                        List<SimpleDateFormat> datePatterns = new ArrayList<>();
                        datePatterns.add(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"));
                        datePatterns.add(new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z"));
                        datePatterns.add(new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy"));
                        
                        // Extracting the date from the given header
                        String dateAsString = clientHeaders.get(unmodifiedIndex);
                        dateAsString = dateAsString.substring(dateAsString.indexOf(":")+1);
                        dateAsString = dateAsString.trim();
    
                        // Parse the string into a Date
                        for(int j = 0; j < datePatterns.size(); j++) {
                            try {
                                parseDate = datePatterns.get(j).parse(dateAsString);
                            } catch (ParseException e) {}   
                        }
                        
                       
                        // Handling the response
                       
                        if (parseDate.before(new Date(file.lastModified()))) {
                            String errorMessage = "<!DOCTYPE html><html><body><h1>412 Precondition Failed</h1><p1>File has been modified since given date</p1></body></html>";
                           
                            headerOut += httpVer + " 412 Precondition Failed\r\n";
                            headerOut += "Date: " + httpDate() + "\r\n";
                            headerOut += "Connection: close\r\n";
                            headerOut += "Content-Type: text/html\r\n";
                            headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                            headerOut += errorMessage;
                            
                            connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                            connection.close();
                            clientIn.close();
                            fullPath = "";
                            continue;
                        }
                    }
                    
                    // Sending a 100 continue response
                    if (expectContinue) {           
                        headerOut += httpVer + " 100 Continue\r\n\r\n";
                        
                        connection.getOutputStream().write(headerOut.getBytes("UTF-8"));;
                    }
                    
                }
                
                // Handling filepath input
                String mimeType = "";
                if (path.equalsIgnoreCase("/shutdown")) {
                    headerOut += httpVer + " 200 Ok\r\n\r\n";
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    
                    // Interrupt each thread. Causes them to exit while loop
                    for (Worker thread: threads) {
                        if (this.equals(thread)) {
                            continue;
                        }
                        thread.interrupt();
                    }
                    
                    terminate();
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    s.close();
                    continue;
                    
                } else if (path.equalsIgnoreCase("/control")) {
                    String html = "<!DOCTYPE html><html><body><h1>Jacob Quon | jquon</h1>";
                    html += "<a href=\"/shutdown\">/shutdown</a>";
                    html += "<ul style=\"list-style-type:none\">";
                    for (Worker thread: threads) {
                        html += "<li>" + thread.getName() + " ----- " + thread.getState() + "   " + thread.fullPath + "</li>";
                    }
                    
                    html += "</ul></body></html>";
                    
                    headerOut += "HTTP/1.1 200 Ok\r\n";
                    headerOut += "Date: " + httpDate() + "\r\n";
                    headerOut += "Connection: close\r\n";
                    headerOut += "Content-Length: " + html.getBytes("UTF-8").length + "\r\n";
                    headerOut += "Content-Type: text/html \r\n\r\n";
                    headerOut += html;
                    
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;
                    
                } else if (routeSearch(method.toUpperCase(), path) >= 0) {
                    int routeIndex = routeSearch(method.toUpperCase(), path);
                    
                    MyRequest request = new MyRequest(method.toUpperCase(), path, s.getLocalPort(), httpVer, webService);
                    
                    // Creating the Map of all the headers in the request
                    HashMap<String, String> headerMap = new HashMap<>();
                    for (int i = 0; i < clientHeaders.size(); i++) {
                        String headerTitle = clientHeaders.get(i).split(":")[0].toUpperCase();
                        String headerContent = clientHeaders.get(i).split(":")[1].trim();
                        headerMap.put(headerTitle, headerContent);
                    }
                    request.setHeaders(headerMap);
                    
                    // Setting values in request
                    if (headerMap.containsKey("User-Agent".toUpperCase())) {
                        request.setUserAgent(headerMap.get("User-Agent".toUpperCase()));
                    }
                    if (headerMap.containsKey("Host".toUpperCase())) {
                        request.setHost(headerMap.get("Host".toUpperCase()));
                    }
                    if (headerMap.containsKey("Content-Type".toUpperCase())) {
                        request.setContentType(headerMap.get("Content-Type".toUpperCase()));
                    }
                    if (headerMap.containsKey("Content-Length".toUpperCase())) {
                        request.setContentLength(Integer.parseInt(headerMap.get("Content-Length".toUpperCase())));
                    }
                    if (headerMap.containsKey("Accept".toUpperCase())) {
                        request.setAccept(headerMap.get("Accept".toUpperCase()));
                    }
                    if (headerMap.containsKey("Cookie".toUpperCase())) {
                        // Could be multiple cookies in the Coookie header
                        String[] cookies = headerMap.get("Cookie".toUpperCase()).split(";");
                        for (String cookie : cookies) {
                            // Extract the id
                            String cookieId = cookie.split("=")[1];
                            String cookieName = cookie.split("=")[0];
                            
                            // Add cookie to the request cookie map
                            request.addCookie(cookieName, cookieId);
                            
                            // Set the session if ID is in the map
                            if (webService.sessionMap.containsKey(cookieId)) {
                                request.setSession(webService.sessionMap.get(cookieId));
                            }
                        }
                    }
                    request.setBody(clientBody);
                    request.setIp(this.ip);
                    request.setUri(fullPath);
                    request.setUrl(fullPath);
                    
                    // Fill in the parameters
                    request.setParams(getParams(method.toUpperCase(), path));

                    // Fill in query parameters
                    request.setQueryParams(getQueryParams(method, path, clientHeaders, clientBody));
                    String[] pathSplitOnQueries = path.split("\\?");
                    if (pathSplitOnQueries.length > 1) {
                        request.setQueryString(pathSplitOnQueries[1]);
                    }
                  
                    
                    MyResponse response = new MyResponse();
                    
                    // Handling Before filters
                    try {
                        List<FilterTriple> matchedBeforeFilters = getFilters(beforeFilters, path, request.accept());
                        for (FilterTriple matchedBeforeFilter : matchedBeforeFilters) {
                            matchedBeforeFilter.getFilter().handle(request, response);
                        }
                    } catch (HaltException e) {
                        headerOut += request.protocol() + " " + e.statusCode() + " Filter Halt Exeption Occured" +"\r\n";
                        headerOut += "Date: " + httpDate() + "\r\n";
                        headerOut += "Connection: close\r\n";
                        headerOut += "Content-Type: text/html\r\n";
                        headerOut += "Content-length: "+ e.body().getBytes("UTF-8").length + "\r\n\r\n";
                        headerOut += e.body();
                        
                        connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                        connection.close();
                        clientIn.close();
                        fullPath = "";
                        continue;
                    }
                    
                    
                    byte[] byteBody;
                    boolean bodyYN = false;
                    if (existingRoutes.get(routeIndex).getRoute().handle(request, response) != null) {
                        bodyYN = true;
                        byteBody = ((String) existingRoutes.get(routeIndex).getRoute().handle(request, response)).getBytes("UTF-8");
                    } else {
                        byteBody = new byte[0];
                    }
                    
                    
                    // Handling after filters;
                    List<FilterTriple> matchedAfterFilters = getFilters(afterFilters, path, request.accept());
                    for (FilterTriple matchedAfterFilter : matchedAfterFilters) {
                        matchedAfterFilter.getFilter().handle(request, response);
                    }
                    
                    // Set info about body
                    if (!response.getRedirected() && bodyYN) {
                        response.setContentLength(byteBody.length);
                        response.bodyRaw(byteBody);
                    }
                    
                    // Adding in the headers
                    response.header("Date", httpDate());
                    response.header("Connection", "close");
                    response.header("Content-Type", response.type());
                    response.header("Content-Length", Integer.toString(response.contentLength()));
                    // Cookie header, only make cookie if there is a session
                    if (request.session() != null) {
                        response.cookie("JSESSIONID", request.session().id());
                    }
                    
                    
                    headerOut += request.protocol() + " " + response.status() + response.getStatusText() + "\r\n"; 
                    headerOut += response.getHeaders() + "\r\n";
                    
                    
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    connection.getOutputStream().write(response.bodyRaw(), 0, response.bodyRaw().length);
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;
                    
                } else if (webService.getRootDir() == null) {
                    String errorMessage = "<!DOCTYPE html><html><body><h1>404 File Not Found</h1><p1>The root directory is null</p1></body></html>";
                    
                    headerOut += httpVer + "404 File Not Found\r\n";
                    headerOut += "Date: " + httpDate() + "\r\n";
                    headerOut += "Connection: close\r\n";
                    headerOut += "Content-Type: text/html\r\n";
                    headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                    headerOut += errorMessage;
                    
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;

                } else if (!file.exists()) {
                    String errorMessage = "<!DOCTYPE html><html><body><h1>404 File Not Found</h1><p1>The given path does not exist</p1></body></html>";
                    
                    headerOut += httpVer + "404 File Not Found\r\n";
                    headerOut += "Date: " + httpDate() + "\r\n";
                    headerOut += "Connection: close\r\n";
                    headerOut += "Content-Type: text/html\r\n";
                    headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                    headerOut += errorMessage;
                    
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;
                    
                } else if (file.isDirectory()) {
                    // Construct an HTML doc with links to the parent directory and all the children
                    String[] directoryChildren = file.list();
                    String html = "<!DOCTYPE html><html><body><h1>Index of " + fullPath + "</h1><ul style=\"list-style-type:none\">";
                    if (path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }
                    if (file.getParent() != null) {
                        html += "<li><a href=\"" + path + "/../\">../</a></li>";
                    }
                    for (int i = 0; i < directoryChildren.length; i++) {
                        html += "<li><a href=\"" + path + "/" + directoryChildren[i] + "\">" + directoryChildren[i] + "</a></li>";
                    }
                    html += "</ul></body></html>";
                    
                    headerOut += httpVer + " 200 Ok\r\n";
                    headerOut += "Date: " + httpDate() + "\r\n";
                    headerOut += "Connection: close\r\n";
                    headerOut += "Content-Length: " + html.length() + "\r\n";
                    headerOut += "Content-Type: text/html\r\n\r\n";
                    headerOut += html;
                   
                    connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                    connection.close();
                    clientIn.close();
                    fullPath = "";
                    continue;
                    
                } else {
                    // Setting the MIME type
                    if (fullPath.endsWith(".jpg")) {
                        mimeType += "image/jpeg";
                    } else if (fullPath.endsWith(".png")) {
                        mimeType += "image/png";
                    } else if (fullPath.endsWith(".gif")) {
                        mimeType += "image/gif";
                    } else if (fullPath.endsWith(".txt")) {
                        mimeType += "text/plain";
                    } else if (fullPath.endsWith(".html") || path.endsWith(".htm")) {
                        mimeType += "text/html";
                    } else if (fullPath.endsWith(".mp4")) {
                        mimeType += "video/mp4";
                    } else {
                        String errorMessage = "<!DOCTYPE html><html><body><h1>501 Not Implemented</h1><p1>The file type submitted is unsupported by the server</p1></body></html>";
                        
                        headerOut += httpVer + " 501 Not Implemented\r\n";
                        headerOut += "Date: " + httpDate() + "\r\n";
                        headerOut += "Connection: close\r\n";
                        headerOut += "Content-Type: text/html\r\n";
                        headerOut += "Content-length: "+errorMessage.getBytes("UTF-8").length + "\r\n\r\n";
                        headerOut += errorMessage;
                        
                        connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                        connection.close();
                        clientIn.close();
                        fullPath = "";
                        continue;
                    }
                } 
                
                
                // Printing header
                headerOut += httpVer + " 200 Ok\r\n";
                headerOut += "Connection: close\r\n";
                headerOut += "Date: " + httpDate() + "\r\n";
                headerOut += "Content-Length: " + file.length() + "\r\n";
                headerOut += "Content-Type: " + mimeType + "\r\n\r\n";
                
                connection.getOutputStream().write(headerOut.getBytes("UTF-8"));
                
                // Only send data if the method is GET
                if (method.equalsIgnoreCase("GET")) {
                    byte[] data = new byte[1000];
                    FileInputStream fileIn = new FileInputStream(fullPath);
                    
                    // Read in the data and send it out
                    int numBytes = 0;
                    while((numBytes = fileIn.read(data)) != -1) {
                        // Allow interruption mid read/write
                        if (isInterrupted()) {
                            fileIn.close();
                            clientIn.close();
                            connection.close();
                            terminate();
                            break;
                        }
                        connection.getOutputStream().write(data,0,numBytes);
                    }
                    
                    if(isInterrupted()) {
                        continue;
                    }
                }
                
                clientIn.close();
                fullPath = "";
                connection.close();
    		} catch (IOException e) {

    		} catch (InterruptedException e) {
                terminate();
    		} catch (Exception e) {
                e.printStackTrace();
            }
    	} 
	}
}
