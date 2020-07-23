package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.WebService;
import edu.upenn.cis455.hw1.interfaces.Route;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.upenn.cis455.hw1.interfaces.Filter;

public class MyWebService extends WebService implements Runnable {

    // Class RouteTriple is just an object which contains the method, path, and route of a route
    private ArrayList<RouteTriple> existingRoutes;
    private ArrayList<FilterTriple> beforeFilters;
    private ArrayList<FilterTriple> afterFilters;
    public HashMap<String, MySession> sessionMap;
    private int port;
    private int numThreads;
    private String rootDir;
    private String ip;
    Worker[] threads;
    ServerSocket s;
    
    public MyWebService() {
        this.existingRoutes = new ArrayList<>();
        this.beforeFilters = new ArrayList<>();
        this.afterFilters = new ArrayList<>();
        this.sessionMap = new HashMap<>();
        // Default Values
        this.port = 80;
        this.numThreads = 100;
        this.ip = "0.0.0.0";
        this.threads = new Worker[numThreads];
    }

    public void run() {  
        int numConnections = 1000;
        BlockingQueue q = new BlockingQueue(numConnections);
  
        // Opening up a connection with specified port
        try {
            s = new ServerSocket(this.port);
            System.out.println("Listening for connection on port "+ 8080 +"....");
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        
        // Initializing all the threads and putting them to work
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Worker(q, this, s);
            threads[i].start();
        }
  
        // Listens on the port waiting for user requests
        while (true) {
            // wait for incoming connection
            try {
                q.enq(s.accept());
            } catch (InterruptedException e) {
                System.err.println(e);
                e.printStackTrace();
            } catch (SocketException e) {
                System.out.println("Server has shut down");
                return;
            } catch (IOException e) {
                e.printStackTrace();    
                return;
            }
        }
    }
    
    //
    public int doesRouteExist(String method, String path) {
        for (int i = 0; i < existingRoutes.size(); i++) {
            boolean pathEqual = true;
            
            // Split up path based on slashes
            String[] pathPartsComp = existingRoutes.get(i).getPath().split("\\?")[0].split("/");
            String[] pathParts = path.split("\\?")[0].split("/");
            
            
            // If theyre not hte same length, no way they can be equal
            if (pathPartsComp.length != pathParts.length) {
                pathEqual = false;
                continue;
            }
            
            // See if all parts in between slashes are the same
            for (int j = 0; j < pathParts.length; j++) {
                
                // Only consider two paths equal if names are equal, or both are wildcards
                boolean partsEqual = pathPartsComp[j].equals(pathParts[j]);                            // Equal or * = *
                boolean starColon  = pathPartsComp[j].equals("*") && pathParts[j].startsWith(":");     // * = :
                boolean colonStar  = pathParts[j].equals("*") && pathPartsComp[j].startsWith(":");     // : = * 
                boolean colonColon = pathParts[j].startsWith(":") && pathPartsComp[j].startsWith(":"); // : = :
                
                
                // If none of the booleans pass, then the paths are not equal
                if (!(partsEqual || starColon || colonStar || colonColon)) {
                    pathEqual = false;
                    break;
                }
            }
            
            // Method and path must both be equal
            if (method.equalsIgnoreCase(existingRoutes.get(i).getMethod()) && pathEqual) {
                return i;
            }
        }
        return -1;
    }

    public void start() {
        //this.run();
    }
    
    public void stop() {
        // Interrupt each thread. Causes them to exit while loop
        for (Worker thread: threads) {
            thread.interrupt();
        }
        
        try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public void staticFileLocation(String directory) {
        this.rootDir = directory;
    }

    public void get(String path, Route route) {
        // If there is no route registered under method and path, make one
        // Dont want query values in the route registry
        if (doesRouteExist("GET", path) < 0) {
            existingRoutes.add(new RouteTriple("GET", path.split("\\?")[0], route));
        }   
    }

    public void ipAddress(String ipAddress) {
        this.ip = ipAddress;
    }
    
    public void port(int port) {
        this.port = port;
    }
    
    public void threadPool(int threads) {
        this.numThreads = threads;
    }
    
    public void post(String path, Route route) {
        // If there is no route registered under method and path, make one
        // Dont want query values in the route registry
        if (doesRouteExist("POST", path) < 0) {
            existingRoutes.add(new RouteTriple("POST", path.split("\\?")[0], route));
        }   
    }

    public void put(String path, Route route) {
        // If there is no route registered under method and path, make one
        // Dont want query values in the route registry
        if (doesRouteExist("PUT", path) < 0) {
            existingRoutes.add(new RouteTriple("PUT", path.split("\\?")[0], route));
        }   
    }

    public void delete(String path, Route route) {
        // If there is no route registered under method and path, make one
        // Dont want query values in the route registry
        if (doesRouteExist("DELETE", path) < 0) {
            existingRoutes.add(new RouteTriple("DELETE", path.split("\\?")[0], route));
        }  
    }

    public void head(String path, Route route) {
        // If there is no route registered under method and path, make one
        // Dont want query values in the route registry
        if (doesRouteExist("HEAD", path) < 0) {
            existingRoutes.add(new RouteTriple("HEAD", path.split("\\?")[0], route));
        }  
    }

    public void options(String path, Route route) {
        // If there is no route registered under method and path, make one
        // Dont want query values in the route registry
        if (doesRouteExist("OPTIONS", path) < 0) {
            existingRoutes.add(new RouteTriple("OPTIONS", path.split("\\?")[0], route));
        }  
    }
    
    public void before(Filter filter) {
        // Add any filters, no such thing as overlapping
        beforeFilters.add(new FilterTriple("", "", filter));
    }

    public void after(Filter filter) {
        afterFilters.add(new FilterTriple("", "", filter));
    }
    
    public void before(String path, String acceptType, Filter filter) {
        beforeFilters.add(new FilterTriple(path, acceptType, filter));
    }
    
    public void after(String path, String acceptType, Filter filter) {
        afterFilters.add(new FilterTriple(path, acceptType, filter));
    }
    
    public ArrayList<RouteTriple> getExistingRoutes() {
        return this.existingRoutes;
    }
    
    public ArrayList<FilterTriple> getBeforeFilters() {
        return this.beforeFilters;
    }
    
    public ArrayList<FilterTriple> getAfterFilters() {
        return this.afterFilters;
    }
    
    public String getRootDir() {
        return this.rootDir;
    }
    
    public String getIP() {
        return this.ip;
    }
    
    public Worker[] getThreads() {
        return this.threads;
    }
    
}
