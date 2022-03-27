
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class test_server {

	private static HttpServer server = null;

    public static void main(String[] args) throws Exception {
    	
    	test_server httpserver=new test_server();
    	httpserver.run();
    	
    }
    
    private void run() {
    	StartHTTPServer();
    }
    
    private void StartHTTPServer() {
    	try {
			server = HttpServer.create(new InetSocketAddress(1400), 0);
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
            String response = "This is the response";
            t.sendResponseHeaders(200, response.length());
            InputStream is = t.getRequestBody();
            while ((i = is.read()) != -1) {
                c = (char) i;
                body = (String) (body + c);
              }
            System.out.println(body);
            t.close();
        }
    }

}
