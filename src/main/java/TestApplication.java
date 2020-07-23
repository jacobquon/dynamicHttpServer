import static edu.upenn.cis455.hw1.WebServiceController.*;

import edu.upenn.cis455.hw1.interfaces.HaltException;

class TestApplication {
	public static void main(String args[]) {

	    port(8080);
	    	    
		get("/", (request,response) -> {
			return "<!DOCTYPE html><html><h>Welcome!</h><p><a href=\"/hello\">Go to page</a></html>";
		});
		
		
		get("/hello", (request, response) -> 
			"Hello World"
		);
    }
}
