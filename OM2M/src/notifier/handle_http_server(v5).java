package org.eclipse.om2m.core.notifier;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Random;

import org.apache.commons.httpclient.HttpException;

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
	
//	private static double efficiency=0.0;
	
//	private static int throughput=0;

//	private static int datasize=0;
	
	
//	public static boolean flag=false;
	
	
	private static Random rand=new Random();
	
//	private static long total_throughput=0;
//	
//	private static long total_size=0;
    
    public static void execute() {
    	monitor();
    	StartHTTPServer();
    }
    
    public static void tshark() throws IOException {
    	ProcessBuilder pb = new ProcessBuilder("tshark", "-l", "-i", "ens32", "-f","udp port 5683 and not ether src 00:0c:29:be:33:56");
		Process process = pb.start();

		BufferedReader br = null;
		    //tried different numbers for BufferedReader's last parameter
		    br = new BufferedReader(new InputStreamReader(process.getInputStream()), 1);
		    String line = null;
		    while ((line = br.readLine()) != null) {
//line = line.trim().replace(" ","@");
//1@0.000000000@192.168.101.136@→@192.168.101.134@MQTT@1725@Publish@Message@(id=3086)@[nscl/applications/NA/monitor]
		    	String[] data = line.trim().replace(" ","@").split("@");
//		    	System.out.println(data[6]);
//		    	throughput=Integer.parseInt(data[6]);
//		    	total_throughput+=throughput;
		    }
    }
    
    private static void StartHTTPServer() {
    	try {
			server = HttpServer.create(new InetSocketAddress(18787), 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        server.createContext("/mncse", new MyHandler());
        //server.setExecutor(null); // creates a default executor
        server.start();
    }
    
    private static void monitor() {
    	new Thread() {
    		public void run() {
    			for(;;) {
    				if(Notifier.flag==true) {
    					changenetwork(Notifier.cnt);
                		try {
                			state_action.decision();
                		} catch (HttpException e) {
                			// TODO Auto-generated catch block
                			e.printStackTrace();
                		} catch (IOException e) {
                			// TODO Auto-generated catch block
                			e.printStackTrace();
                		}
    				}
    				Notifier.flag=false;
            		try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    	}.start();
    }
    
    
    private static void changenetwork(int cnt) {
    	
    	int x=rand.nextInt(2);
		int y=rand.nextInt(2);

		
		if(1<=cnt && cnt<=100) {
			if(x==0) {
				ChangeWanem.loss_rate=15;
			}else {
				ChangeWanem.loss_rate=20;
			}
			if(y==0) {
				ChangeWanem.bandwidth=500;
			}else {
				ChangeWanem.bandwidth=1000;
			}
		}else if(100<cnt && cnt<=200) {
			if(x==0) {
				ChangeWanem.loss_rate=5;
			}else {
				ChangeWanem.loss_rate=15;
			}
			if(y==0) {
				ChangeWanem.bandwidth=2000;
			}else {
				ChangeWanem.bandwidth=1000;
			}
		}else if(200<cnt && cnt<=300) {
			if(x==0) {
				ChangeWanem.loss_rate=25;
			}else {
				ChangeWanem.loss_rate=20;
			}
			if(y==0) {
				ChangeWanem.bandwidth=1500;
			}else {
				ChangeWanem.bandwidth=1000;
			}
		}else if(300<cnt && cnt<=400) {
			if(x==0) {
				ChangeWanem.loss_rate=20;
			}else {
				ChangeWanem.loss_rate=10;
			}
			if(y==0) {
				ChangeWanem.bandwidth=1000;
			}else {
				ChangeWanem.bandwidth=1500;
			}
		}else if(400<cnt && cnt<=500) {
			if(x==0) {
				ChangeWanem.loss_rate=10;
			}else {
				ChangeWanem.loss_rate=0;
			}
			if(y==0) {
				ChangeWanem.bandwidth=500;
			}else {
				ChangeWanem.bandwidth=100;
			}
		}

		ChangeWanem.start_change();
    }
    
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
        	String body="";
        	int i;
        	char c;
//        	datasize=Notifier.datasize;
//        	total_size+=datasize;
//        	efficiency=(double)datasize/total_throughput;
//        	total_throughput=0;
            String response = String.valueOf(Notifier.datasize)+"//"+String.valueOf(ChangeWanem.loss_rate)+"//"+String.valueOf(ChangeWanem.bandwidth);
//            total_throughput=0;
            t.sendResponseHeaders(200, response.length());
            InputStream is = t.getRequestBody();
            while ((i = is.read()) != -1) {
                c = (char) i;
                body = (String) (body + c);
              }
            System.out.println(body);
            if(!body.equals("unchanged")) {
            	Notifier.return_protocol=body;
            }
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            t.close();
        }
    }
}

