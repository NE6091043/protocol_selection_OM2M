import java.io.BufferedWriter;

import java.io.FileWriter;

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;

import java.nio.file.Files;

import java.nio.file.Path;

import java.nio.file.Paths;

import java.util.List;



import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.methods.DeleteMethod;

import org.apache.commons.httpclient.methods.PostMethod;

import org.apache.commons.httpclient.methods.StringRequestEntity;

import org.eclipse.californium.core.coap.Response;

import com.sun.net.httpserver.HttpServer;



public class DeviceForQueryDriven {



	public static String GSCL_IP = "140.116.247.69";

	public static String GSCL_Port = "18282";



	public static String deviceIP = "192.168.72.8";

	public static String DeviceID = "D2";

	public static String dataContainerID = "DATA";

	public static String descriptorContainerID = "DESCRIPTOR";

	public static String authorization = "admin:admin";



	private static HttpServer server = null;



	public static int deviceHttpPort = 1401;

	public static String doorContext = "/doors";

	public static String readerContext = "/readers";



	static int temperatureValue = 0;

	static String currentDoorState = "Off";



	// 設定ProtocolSelection的ipuId

	static String ipuId = "selection";



	static int loopcount = 50;



	public DeviceForQueryDriven() {



	}



	public static HttpServer getServer() {

		return server;

	}



	public static void setServer(HttpServer server) {

		DeviceForQueryDriven.server = server;

	}

	

	public static void main(String[] args) throws IOException {



		DeviceForQueryDriven device = new DeviceForQueryDriven();



		// 設定迴圈次數，這個要跟  NA 的迴圈次數設一樣，以便DA收完資料時能夠自動停止程式。

//		String str_test_loop_count = "100";

//

//		if (args.length > 0) {

//			str_test_loop_count = args[0];

//		}

		

		FileWriter writer=new FileWriter("test.csv",false);

		BufferedWriter bw = new BufferedWriter(writer);

		bw.write("Result");

		bw.write(',');

		bw.write("order");

		bw.write(',');

		bw.write("delay");

		bw.write("\n");

		bw.flush();

		bw.close();

		

		for(int i=1;i<loopcount;++i) {

			writer=new FileWriter("test.csv",true);

			bw = new BufferedWriter(writer);

			writer.write("\n");

			writer.write(String.valueOf(i));

			bw.write(',');	

			long x=5000;

			bw.write(String.valueOf(x));

			bw.flush();

			bw.close();

		}

		for(int i=1;i<=loopcount;++i) {

			Path path = Paths.get("test.csv");

			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

			String s=i+","+String.valueOf(0);

			lines.set(i , ","+s);

			Files.write(path, lines, StandardCharsets.UTF_8);

		}

		

		System.out.println("create csv sucess!");

		

		device.run();

	}



	private static void delay(int i) {

		try {

			Thread.sleep(i);

		} catch (InterruptedException e) {

			e.printStackTrace();

		}

	}



	public void run() {

		//int statusCode = ChangeWanem.start(0);

		int statusCode = 200;

		if (statusCode == 200) {

			delay(1000);



			// 0. 啟動HTTP server以便接收notification

			System.out.println("ssssssssssssssssssssssssssssssssssss");

			StartHTTPServer();



			DeleteResourceOnGSCL("gscl/applications/" + DeviceID);

			CreateDeviceResource();



			DeleteResourceOnGSCL("gscl/applications/" + DeviceID + "_Reader");

			CreateReaderResource();



		} else {

			System.out.println("change packet loss rate 0 error");

		}

	}



	private void StartHTTPServer() {

		try {

			System.out.println("Starting HTTP server..");

			server = HttpServer.create(new InetSocketAddress(deviceHttpPort), 0);

			// output is from here

			// 2021.12.7

			server.createContext(readerContext, new ReaderHandlerForQueryDriven());

			server.start();

			System.out.println("The server is now listening on\nPort: " + deviceHttpPort + "\nContext: " + doorContext + "\n");

		} catch (IOException ex) {

			ex.printStackTrace();

		}

	}



	private void CreateReaderResource() {



		System.out.println("Create reader resource on GSCL...");

		System.out.println("-----------------------------------------------");

//		System.out.println("-----------------------------------------------");

		String url = "";

		HttpClient httpclient;

		PostMethod httpMethod;

		StringBuilder sb;

		StringRequestEntity requestEntity;

		int statusCode;

		try {



			// region Create application resource

			url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications";

			httpclient = new HttpClient();

			httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb = new StringBuilder();



			sb.append("<om2m:application xmlns:om2m='http://uri.etsi.org/m2m' appId='" + DeviceID + "_Reader'>");



			// 公告給NSCL

			sb.append("<om2m:announceTo>");

			sb.append("<om2m:activated>true</om2m:activated>");

			sb.append("<om2m:sclList>");

			sb.append("<reference>nscl</reference>");

			sb.append("</om2m:sclList>");

			sb.append("</om2m:announceTo>");



			// 設定searchStrings

			sb.append("<om2m:searchStrings>");

			sb.append("<om2m:searchString>ResourceType/Reader</om2m:searchString>");

			sb.append("<om2m:searchString>ResourceID/" + DeviceID + "_Reader</om2m:searchString>");

			sb.append("</om2m:searchStrings>");



			// 設定aPoCPaths

			sb.append("<om2m:aPoCPaths>");

			sb.append("<om2m:aPoCPath>");

			sb.append("<om2m:path>http://" + deviceIP + ":" + deviceHttpPort + "</om2m:path>");

			sb.append("</om2m:aPoCPath>");

			sb.append("</om2m:aPoCPaths>");



			sb.append("</om2m:application>");



			requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

			httpMethod.setRequestEntity(requestEntity);

			statusCode = httpclient.executeMethod(httpMethod);

//			System.out.println(statusCode);



			// endregion



			// region Create a DESCRIPTOR container

			url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID + "_Reader/containers";

			httpclient = new HttpClient();

			httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb = new StringBuilder();

			sb.append("<om2m:container xmlns:om2m='http://uri.etsi.org/m2m' om2m:id='" + descriptorContainerID + "'>");

			sb.append("</om2m:container>");



			requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

			httpMethod.setRequestEntity(requestEntity);

			statusCode = httpclient.executeMethod(httpMethod);

//			System.out.println(statusCode);

			// endregion



			// region Create a description contentInstance

			url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID + "_Reader/containers/DESCRIPTOR/contentInstances";

			httpclient = new HttpClient();

			httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb = new StringBuilder();

			sb.append("<obj>");

			sb.append("<op name='saveCards' href='gscl/applications/" + DeviceID + "_Reader/readers/savecards' is='create'/>");

			sb.append("</obj>");



			requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

			httpMethod.setRequestEntity(requestEntity);

			statusCode = httpclient.executeMethod(httpMethod);

//			System.out.println(statusCode);

			// endregion



		} catch (Exception e) {

			e.printStackTrace();

		}

	}



	private void DeleteResourceOnGSCL(String targetID) {

		System.out.println("delete resource on GSCL: " + targetID);

		try {

			String url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/" + targetID;

			HttpClient httpclient = new HttpClient();

			DeleteMethod httpMethod = new DeleteMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}



	private void CreateDeviceResource() {

		System.out.println("Create device resource on GSCL...");



		// Create application resource

		try {

			String url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications";

			HttpClient httpclient = new HttpClient();

			PostMethod httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			StringBuilder sb = new StringBuilder();

			sb.append("<om2m:application xmlns:om2m='http://uri.etsi.org/m2m' appId='" + DeviceID + "'>");

			sb.append("</om2m:application>");

			StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

			httpMethod.setRequestEntity(requestEntity);

			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);

		} catch (Exception e) {

			e.printStackTrace();

		}

	}



	static void output(Response response) {

		if (response != null && !response.getPayloadString().equals("")) {

			System.out.println(response.getPayloadString());

		}

	}



	/**

	 * 產生指定大小的資料量

	 */

	static String str_generator(int size) {

		StringBuffer out = new StringBuffer();

		String strChars = ascii_letters() + digits();

		for (int i = 0; i < size; i++) {

			int idx = (int) (Math.random() * strChars.length());

			String str = strChars.substring(idx, idx + 1);

			out.append(str);

		}

		return out.toString();

	}



	static String ascii_letters() {

		StringBuffer out = new StringBuffer();

		for (char c = 'a'; c <= 'z'; c++) {

			out.append(c);

		}



		for (char c = 'A'; c <= 'Z'; c++) {

			out.append(c);

		}



		return out.toString();

	}



	static String digits() {

		StringBuffer out = new StringBuffer();

		for (int c = 0; c <= 9; c++) {

			out.append(c);

		}

		return out.toString();

	}



	static int number = 0;



}

