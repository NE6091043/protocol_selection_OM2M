package org.eclipse.om2m.core.notifier;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class handle_http {
	
//	protected static String return_protocol="ws";

	private static HttpServer server = null;

//    public static void main(String[] args) throws Exception {
//    	
//    	handle_http httpserver=new handle_http();
//    	httpserver.execute();
//    	
//    }
    
    public static void execute() {
    	StartHTTPServer();
    }
    
    private static void StartHTTPServer() {
    	try {
			server = HttpServer.create(new InetSocketAddress(18787), 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        server.createContext("/test", new MyHandler());
        //server.setExecutor(null); // creates a default executor
        server.start();
    }
    
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	String body="";
        	int i;
        	char c;
            String response = "ok";
            t.sendResponseHeaders(200, response.length());
            InputStream is = t.getRequestBody();
            while ((i = is.read()) != -1) {
                c = (char) i;
                body = (String) (body + c);
              }
            System.out.println(body);
            Notifier.return_protocol=body;
            t.close();
        }
    }

}

