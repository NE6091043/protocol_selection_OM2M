import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

// 這是 network application 的測試程式

public class TestNAForEventDriven {
	
	//	delay maxmimum
	private static int loopcount=500;
	private static long[] arr=new long[loopcount+1];
	private static String Local_IP = "192.168.72.9";

	//private static String Local_IP = "140.116.247.72";

	private static int Local_Port = 1400;

	private static String context = "/monitor";

	private static String INCSE_IP = "140.116.247.69";

	//private static String INCSE_IP = "140.116.247.72";

	private static String INCSE_Port = "18080";
	
	private static String app = "NA";

	private static double delay_sum=0.0;

	private static double delay_avg=0.0;


	private static HttpServer server = null;
	

	public static HttpServer getServer() {
		return server;
	}
	
	public static long[] getoutputarr() {
		return arr.clone();
	}


	public static void setServer(HttpServer server) {
		TestNAForEventDriven.server = server;
	}


	public static void main(String[] args) throws InterruptedException, IOException {
		
		Arrays.fill(arr,300000);
		FileWriter writer=new FileWriter("test.csv",false);
		BufferedWriter bw = new BufferedWriter(writer);
		bw.write("test");
		bw.write(',');
		bw.write("order");
		bw.write(',');
		bw.write("delay");
		bw.write("\n");
		bw.flush();
		bw.close();
		

		for(int i=1;i<=loopcount;++i) {
			writer=new FileWriter("test.csv",true);
			bw = new BufferedWriter(writer);
			writer.write("\n");
			writer.write(String.valueOf(i));
			bw.write(',');	
			long x=arr[i];
			bw.write(String.valueOf(x));
			bw.flush();
			bw.close();
		}
		
		System.out.println("create csv sucess!");
		

		TestNAForEventDriven na = new TestNAForEventDriven();


		na.run();

	}



	private void run() throws InterruptedException, IOException {
		
		

		// 0. 啟動HTTP server以便接收notification

		StartHTTPServer();
	
		
		// 1. 先刪除INCSE上的NA

		unregisterToINCSE();



		// 2. 向 INCSE 註冊

		registerToINCSE();



		// 3. 搜尋資源(discovery)

		List<String> listReferenceURI = discoverResource("Temp"); // ex:



		System.out.println("listReferenceURI = " + listReferenceURI);

		

		List<String> listLink = getLink(listReferenceURI); // ex:

															// gscl/applications/D2_Temp



		System.out.println("listLink = " + listLink);



		// 4. 訂閱資源(建立subscription resource)

		CreateSubscriptionResource(listLink);

	}

	

	private void unregisterToINCSE() {

		System.out.println("unregisterToINCSE...");

		try {

			String url = "http://" + INCSE_IP + ":" + INCSE_Port + "/om2m/nscl/applications/NA";

			HttpClient httpclient = new HttpClient();

			DeleteMethod httpMethod = new DeleteMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}



	private void StartHTTPServer() {

		try {

			System.out.println("Starting HTTP server..");

			server = HttpServer.create(new InetSocketAddress(Local_Port), 0);

			server.createContext(context, new MyHandler());

			server.start();

			System.out.println("The server is now listening on\nPort: " + Local_Port + "\nContext: " + context + "\n");

		} catch (IOException ex) {

			ex.printStackTrace();

		}

	}



	private void CreateSubscriptionResource(List<String> listLink) {



		System.out.println("CreateSubscriptionResource...");



		for (String strLink : listLink) {

			try {

				if(strLink.equals("gscl/applications/D2_Temp")) {

					System.out.println(strLink);

					String url = "http://" + INCSE_IP + ":" + INCSE_Port + "/om2m/" + strLink + "/containers/DATA/contentInstances/subscriptions";

					HttpClient httpclient = new HttpClient();

					PostMethod httpMethod = new PostMethod(url);

					httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");


					StringBuilder sb = new StringBuilder();

					sb.append("<om2m:subscription xmlns:om2m='http://uri.etsi.org/m2m'>");

					sb.append("<om2m:contact>nscl/applications/" + app + context + "</om2m:contact>");

					sb.append("</om2m:subscription>");


					StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

					httpMethod.setRequestEntity(requestEntity);


					int statusCode = httpclient.executeMethod(httpMethod);

					System.out.println(statusCode);

					System.out.println("-----------------------------------------------");

				}

			} catch (UnsupportedEncodingException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

	}



	private List<String> getLink(List<String> listReference) {

		List<String> listLink = new ArrayList<String>();

		for (String strReference : listReference) {

			try {

				String url = "http://" + INCSE_IP + ":" + INCSE_Port + "/om2m/" + strReference;

				HttpClient httpclient = new HttpClient();

				GetMethod httpMethod = new GetMethod(url);

				httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

				int statusCode = httpclient.executeMethod(httpMethod);

				System.out.println(statusCode);

				String strResponseBody = httpMethod.getResponseBodyAsString();

				String patternStr = "<om2m:link>(.+)</om2m:link>";

				Pattern pattern = Pattern.compile(patternStr);

				Matcher matcher = pattern.matcher(strResponseBody);

				if (matcher.find()) {

					String strTargetID = matcher.group(1);

					listLink.add(strTargetID);

				}

				
			} catch (UnsupportedEncodingException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

		return listLink;

	}


	private List<String> discoverResource(String strResourceType) {

		System.out.println("discoverResource....");



		List<String> listReferenceURI = new ArrayList<String>();

		try {

			String url = "http://" + INCSE_IP + ":" + INCSE_Port + "/om2m/nscl/discovery?searchString=ResourceType/" + strResourceType;

			HttpClient httpclient = new HttpClient();

			GetMethod httpMethod = new GetMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);

			String strResponseBody = httpMethod.getResponseBodyAsString();

			String patternStr = "<reference>(.+)</reference>";

			Pattern pattern = Pattern.compile(patternStr);

			Matcher matcher = pattern.matcher(strResponseBody);

			while (matcher.find()) {

				String strReferenceURI = matcher.group(1);

				listReferenceURI.add(strReferenceURI);

			}



		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}



		return listReferenceURI;

	}



	/**

	 * 向INCSE註冊

	 */

	private void registerToINCSE() {



		try {

			System.out.println("registerToINCSE...");



			// Create NA application

			String url = "http://" + INCSE_IP + ":" + INCSE_Port + "/om2m/nscl/applications";

			HttpClient httpclient = new HttpClient();

			PostMethod httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");



			StringBuilder sb = new StringBuilder();

			sb.append("<om2m:application xmlns:om2m='http://uri.etsi.org/m2m' appId='" + app + "'>");

			sb.append("<om2m:aPoCPaths>");

			sb.append("<om2m:aPoCPath>");

			sb.append("<om2m:path>http://" + Local_IP + ":" + Local_Port + "</om2m:path>");

			sb.append("</om2m:aPoCPath>");

			sb.append("</om2m:aPoCPaths>");

			sb.append("</om2m:application>");



			StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

			httpMethod.setRequestEntity(requestEntity);



			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);



		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

	}



	static int number = 0;

	static class MyHandler implements HttpHandler {
		
		
		public void handle(HttpExchange t) throws IOException {

			String body = "";

			int i;

			char c;

			//2021.5.25 modified 

			//used to stop pinging on GSCL

			int order=0;

			

			try {

				InputStream is = t.getRequestBody();



				while ((i = is.read()) != -1) {

					c = (char) i;

					body = (String) (body + c);

				}

			} catch (Exception e) {

				e.printStackTrace();

			}

			++number;
		

			System.out.println("Received notification:" +number+" times");



			//System.out.println(body);





			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			try {

				DocumentBuilder dBuilder = dbf.newDocumentBuilder();



				Document notifyDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(body.getBytes("utf-8"))));



				String contentInstance64 = notifyDoc.getElementsByTagName("om2m:representation").item(0).getTextContent();





				String contentInstance = new String(DatatypeConverter.parseBase64Binary(contentInstance64));




				Document instanceDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(contentInstance.getBytes("utf-8"))));

				String content64 = instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();


				final String content = new String(DatatypeConverter.parseBase64Binary(content64));

			



				// regex

				String strTimestamp = "";

				String patternStr = "<str name='timestamp' val='(\\d+)'/>";

				Pattern pattern = Pattern.compile(patternStr);

				Matcher matcher = pattern.matcher(content);

				

				String[] tmp=content.split("<str name='index' val='");



				String idx=tmp[1].split("'/></obj>")[0];


				String[] data = content.split("<str name='data' val='");
		        String payload = data[1].split("'/>")[0];

				

				if (matcher.find()) {

					strTimestamp = matcher.group(1);




					long previous_time = Long.parseLong(strTimestamp);


					long current_time = System.currentTimeMillis();



					long result = current_time - previous_time;

					System.out.println("Order: " + idx);

					System.out.println("Content Size:" + payload.length());

			          // System.out.println("Content:\n" + content + "\n");

			        System.out.println("body size = " + body.length());

					System.out.println("time =" + result);

					

					if(number>50) {

						delay_sum+=result;

						delay_avg=delay_sum/(number-50);

						System.out.println("total_avg_delay =" + delay_avg);
	

					}

					String url = "http://140.116.247.69:9000/receive_delay_as_reward";

					HttpClient httpclient = new HttpClient();

					PostMethod httpMethod = new PostMethod(url);

					StringBuilder sb = new StringBuilder();

					sb.append(result);

					sb.append("//");

					sb.append(idx);
					
					sb.append("//");
					sb.append(number);
					sb.append("//");
					sb.append(order);

					StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");

					httpMethod.setRequestEntity(requestEntity);

					int statuscode = httpclient.executeMethod(httpMethod);

					if(result<=300000) {
						int line_idx=Integer.parseInt(idx);
						Path path = Paths.get("test.csv");
						List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
						String s=idx+","+String.valueOf(result);
						lines.set(line_idx , s);
						Files.write(path, lines, StandardCharsets.UTF_8);
					}
					

					System.out.println("---------------------"+statuscode+"------------------------");				
				}



				t.sendResponseHeaders(204, -1);

			} catch (ParserConfigurationException e) {

				e.printStackTrace();

				t.sendResponseHeaders(501, -1);

			} catch (SAXException e) {

				e.printStackTrace();

				t.sendResponseHeaders(501, -1);

			} catch (IOException e) {

				e.printStackTrace();

				t.sendResponseHeaders(501, -1);

			}


		}		
		


	}

}