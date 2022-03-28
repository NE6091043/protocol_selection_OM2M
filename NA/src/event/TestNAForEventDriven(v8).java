import java.awt.Toolkit;

import java.io.BufferedWriter;

import java.io.ByteArrayInputStream;

import java.io.FileWriter;

import java.io.IOException;

import java.io.InputStream;

import java.io.UnsupportedEncodingException;

import java.net.InetSocketAddress;

import java.util.ArrayList;

import java.util.List;

import java.util.concurrent.TimeUnit;

import java.util.regex.Matcher;

import java.util.regex.Pattern;



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



	private static String Local_IP = "192.168.72.20";

	//private static String Local_IP = "140.116.247.72";

	private static int Local_Port = 1400;

	private static String context = "/monitor";

	private static String NSCL_IP = "140.116.247.69";

	//private static String NSCL_IP = "140.116.247.72";

	private static String NSCL_Port = "18080";



	private static String app = "NA";

	private static String container = "DATA";

	private static double delay_sum=0.0;

	private static double delay_avg=0.0;

	static int iLoopCount = 1;



	private static HttpServer server = null;



	public static HttpServer getServer() {

		return server;

	}



	public static void setServer(HttpServer server) {

		TestNAForEventDriven.server = server;

	}



	public static void main(String[] args) throws InterruptedException {



		TestNAForEventDriven na = new TestNAForEventDriven();



		// 設定迴圈次數，這個要跟 NA 的迴圈次數設一樣，以便DA收完資料時能夠自動停止程式。

		String str_test_loop_count = "1000";



		if (args.length > 0) {

			str_test_loop_count = args[0];

		}



		iLoopCount = Integer.parseInt(str_test_loop_count);

		System.out.println(iLoopCount);

		na.run();

	}



	private void run() throws InterruptedException {



		// 0. 啟動HTTP server以便接收notification

		StartHTTPServer();



		// 1. 先刪除NSCL上的NA

		unregisterToNSCL();



		// 2. 向 NSCL 註冊

		registerToNSCL();



		// 3. 搜尋資源(discovery)

		List<String> listReferenceURI = discoverResource("Temp"); // ex:

		// nscl/scls/gscl/applications/D2_TempAnnc



		System.out.println("listReferenceURI = " + listReferenceURI);

		

		List<String> listLink = getLink(listReferenceURI); // ex:

															// gscl/applications/D2_Temp



		System.out.println("listLink = " + listLink);



		// 4. 訂閱資源(建立subscription resource)

		CreateSubscriptionResource(listLink);

		

		//2021.8.30 modified

//		TimeUnit.SECONDS.sleep(25);

//		

//		//2021.5.25 modified, no needed

//		// 送指令給NSCL，停止 NSCL裡的ping的程式

//		//int statusCode = ChangeWanem.start(0);

//		//int statusCode = 200;

//		//System.out.println("ChangeWanem(0) = " + statusCode);

//		//stopPingOnGSCL("gscl/applications/Ping/process/stop");

//		

//		// 停止 HTTP Server

//		TestNAForEventDriven.getServer().stop(0);

//		System.out.println("finished");

//		

//		//2021.5.25 modified 

//		//used to stop pinging on GSCL

//		double received=(double)number/iLoopCount;

//		received*=100;

//		double loss=100.0-received;

//		System.out.println("Notification loss rate :" + loss +"%");

		

		

	}

	

	private boolean stopPingOnGSCL(String cmd) {

		try {

			// 停止GSCL的ping程式

			String url = "http://" + TestNAForEventDriven.NSCL_IP + ":" + TestNAForEventDriven.NSCL_Port + "/om2m/" + cmd;

			HttpClient httpclient = new HttpClient();

			PostMethod httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");



			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println("stopPingOnGSCL() = " + statusCode);

			return true;

		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

		return false;

	}





	

	private void unregisterToNSCL() {

		System.out.println("unregisterToNSCL...");

		try {

			String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/nscl/applications/NA";

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

					String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + strLink + "/containers/DATA/contentInstances/subscriptions";

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

					String strResponseBody = httpMethod.getResponseBodyAsString();

					// System.out.println(strResponseBody);

					System.out.println("-----------------------------------------------");

				}

			} catch (UnsupportedEncodingException e) {

				e.printStackTrace();

			} catch (IOException e) {

				e.printStackTrace();

			}

		}

//		try {

//			System.out.println(listLink.get(2));

//			String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + listLink.get(2) + "/containers/DATA/contentInstances/subscriptions";

//			HttpClient httpclient = new HttpClient();

//			PostMethod httpMethod = new PostMethod(url);

//			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

//

//			StringBuilder sb = new StringBuilder();

//			sb.append("<om2m:subscription xmlns:om2m='http://uri.etsi.org/m2m'>");

//			sb.append("<om2m:contact>nscl/applications/" + app + context + "</om2m:contact>");

//			sb.append("</om2m:subscription>");

//

//			StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

//			httpMethod.setRequestEntity(requestEntity);

//

//			int statusCode = httpclient.executeMethod(httpMethod);

//			System.out.println(statusCode);

//			String strResponseBody = httpMethod.getResponseBodyAsString();

//			// System.out.println(strResponseBody);

//			System.out.println("-----------------------------------------------");

//		} catch (UnsupportedEncodingException e) {

//			e.printStackTrace();

//		} catch (IOException e) {

//			e.printStackTrace();

//		}

	}



	private List<String> getLink(List<String> listReference) {

		List<String> listLink = new ArrayList<String>();

		for (String strReference : listReference) {

			try {

				String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + strReference;

				HttpClient httpclient = new HttpClient();

				GetMethod httpMethod = new GetMethod(url);

				httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

				int statusCode = httpclient.executeMethod(httpMethod);

				System.out.println(statusCode);

				String strResponseBody = httpMethod.getResponseBodyAsString();

				// System.out.println(strResponseBody);



				// get link of resource

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

			String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/nscl/discovery?searchString=ResourceType/" + strResourceType;

			HttpClient httpclient = new HttpClient();

			GetMethod httpMethod = new GetMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);

			String strResponseBody = httpMethod.getResponseBodyAsString();

			// System.out.println(strResponseBody);



			// get reference uri of resource

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

	 * 向NSCL註冊

	 */

	private void registerToNSCL() {



		try {

			System.out.println("registerToNSCL...");



			// Create NA application

			String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/nscl/applications";

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



			writeBodyToFile(body);



			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			try {

				DocumentBuilder dBuilder = dbf.newDocumentBuilder();



				Document notifyDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(body.getBytes("utf-8"))));



				String contentInstance64 = notifyDoc.getElementsByTagName("om2m:representation").item(0).getTextContent();

				// System.out.println("ContentInstance (Base64-encoded):\n" +

				// contentInstance64 + "\n");



				String contentInstance = new String(DatatypeConverter.parseBase64Binary(contentInstance64));

				// System.out.println("ContentInstance:\n" + contentInstance +

				// "\n");



				Document instanceDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(contentInstance.getBytes("utf-8"))));

				String content64 = instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();

				// System.out.println("Content (Base64-encoded):\n" + content64

				// + "\n");



				final String content = new String(DatatypeConverter.parseBase64Binary(content64));

				//System.out.println(content);



				// regex

				String strTimestamp = "";

				String patternStr = "<str name='timestamp' val='(\\d+)'/>";

				Pattern pattern = Pattern.compile(patternStr);

				Matcher matcher = pattern.matcher(content);

				

				String[] tmp=content.split("<str name='index' val='");

				System.out.println(tmp[1]+"            ");

				//2021.9.26

				String idx=tmp[1].split("'/><str name='timestamp'")[0];



				//System.out.println(protocol);

				

				

				if (matcher.find()) {

					strTimestamp = matcher.group(1);

					

					// 取得Device送出訊息時的時間

					//2021.9.26

					long result = Long.parseLong(strTimestamp);

					//2021.5.25 modified 

					//System.out.println("Device time =" + previous_time);

					// 取得目前時間

					//long current_time = System.currentTimeMillis();



					// 將時間相減，算出Delay(ms)

					//2021.9.26

					//long result = current_time - previous_time;

					//2021.5.25 modified 

					//System.out.println("current time =" + current_time);

					//2021.5.18

					//no need to show number ,just need order

					//System.out.println(number);

					

					//String strNumber = content.substring(content.lastIndexOf("<str name='data' val=") + 22 , content.lastIndexOf("<str name='data' val=") + 25);

					//order = Integer.parseInt(strNumber);

					System.out.println("Order: " + idx);

					System.out.println("Content Size:" + content.length());

					// System.out.println("Content:\n" + content + "\n");

					System.out.println("body size = " + body.length());

					System.out.println("time =" + result);

					

					// ignore 50 times before

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

					//2021.9.14 mod

					sb.append("//");

					sb.append(idx);

					StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");

					httpMethod.setRequestEntity(requestEntity);

					int statuscode = httpclient.executeMethod(httpMethod);

					//System.out.print(new String(httpMethod.getResponseBody()));

					

//					String curLoss = "_20_2";

					// 將結果寫到文字檔裡

					//FileWriter fw = new FileWriter("na_delay.txt", true); // True則表示用附加的方式寫到檔案原有內容之後

//					FileWriter fw = new FileWriter("coap_"+content.length()+curLoss+"_na_delay.txt", true); // True則表示用附加的方式寫到檔案原有內容之後

//					writeToFile("xmpp_"+content.length()+curLoss+"_na_delay.txt", String.valueOf(result));

//					

//

//					// Write Order of Message to .txt

//					writeToFile("xmpp_"+content.length()+curLoss+"_na_success.txt", Integer.toString(order));

					//System.out.println("Order: " + order);

					

					System.out.println("-----------------------------------------------");

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

//			// 2021.5.20 modified 

//			// 若收到訊息的次數已到達原先設定的迴圈次數就停止程式

//			if (number >= TestNAForEventDriven.iLoopCount) {

//

//				// 送指令給NSCL，停止 NSCL裡的ping的程式

//				//int statusCode = ChangeWanem.start(0);

//				int statusCode = 200;

//				//System.out.println("ChangeWanem(0) = " + statusCode);

//				stopPingOnGSCL("gscl/applications/Ping/process/stop");

//

//				// 停止 HTTP Server

//				TestNAForEventDriven.getServer().stop(0);

//				System.out.println("finished");

//			}

			



			//2021.5.25 modified 

			//used to stop pinging on GSCL

//			if (order >= TestNAForEventDriven.iLoopCount) {

//

//				// 送指令給NSCL，停止 NSCL裡的ping的程式

//				//int statusCode = ChangeWanem.start(0);

//				int statusCode = 200;

//				//System.out.println("ChangeWanem(0) = " + statusCode);

//				stopPingOnGSCL("gscl/applications/Ping/process/stop");

//				

//				// 停止 HTTP Server

//				TestNAForEventDriven.getServer().stop(0);

//				System.out.println("finished");

//				

//				//2021.5.25 modified 

//				//used to stop pinging on GSCL

//				double received=(double)number/order;

//				received*=100;

//				double loss=100.0-received;

//				System.out.println("Notification loss rate :" + loss +"%");

//			}

		}



		private void writeBodyToFile(String body) throws IOException {

			// 將body寫到文字檔裡

			FileWriter fw = new FileWriter("na_body.txt", true); // True則表示用附加的方式寫到檔案原有內容之後

			BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結

			bw.write(body);

			bw.flush();

			bw.close();

		}



		private void writeToFile(String fileName, String body) throws IOException {

			FileWriter fw = new FileWriter(fileName, true); // True則表示用附加的方式寫到檔案原有內容之後

			BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結

			bw.write(body + "\n");

			bw.flush();

			bw.close();

		}

	}

}