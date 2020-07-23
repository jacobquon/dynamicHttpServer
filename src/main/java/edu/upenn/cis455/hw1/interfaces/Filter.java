package edu.upenn.cis455.hw1.interfaces;

import edu.upenn.cis455.hw1.interfaces.Request;
import edu.upenn.cis455.hw1.interfaces.Response;

/**
 * A Filter is called by the Web server to process data before the Route
 * Handler is called.  This is typically used to attach attributes ()
 * or to call the HaltException, e.g., if the user is not authorized.
 */
@FunctionalInterface
public interface Filter {
    void handle(Request request, Response response) throws Exception;
}
