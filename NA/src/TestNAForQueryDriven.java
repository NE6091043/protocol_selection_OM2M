import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
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

// 這是 network application 的測試程式

public class TestNAForQueryDriven {

	private static String Local_IP = "192.168.72.109";
	private static int Local_Port = 1400;
	private static String context = "/monitor";
	private static String NSCL_IP = "192.168.72.49";
	private static String NSCL_Port = "8080";

	private static String app = "NA";
	private static String container = "DATA";

	public static void main(String[] args) {

		// 設定 packet loss rate
		String strPacketLossRate = "25";

		// 設定迴圈次數
		String strLoopCount = "100";

		// 設定要傳送的資料大小
		String strDataSize = "1421";

		if (args.length > 0) {
			strPacketLossRate = args[0];
		}

		if (args.length > 1) {
			strLoopCount = args[1];
		}

		if (args.length > 2) {
			strDataSize = args[2];
		}

		TestNAForQueryDriven na = new TestNAForQueryDriven();
		na.run(strPacketLossRate, strLoopCount, strDataSize);
	}

	private void run(String strPacketLossRate, String strLoopCount, String strDataSize) {
		Integer iPacketLossRate = Integer.parseInt(strPacketLossRate);
		Integer iLoopCount = Integer.parseInt(strLoopCount);
		Integer iDataSize = Integer.parseInt(strDataSize);

		int statusCode = ChangeWanem.start(0);
		if (statusCode == 200) {

			// 1. 先刪除NSCL上的NA
			unregisterToNSCL();

			// 2. 向 NSCL 註冊
			registerToNSCL();

			// 3. 搜尋資源(discovery)
			List<String> listReferenceURI = discoverResource("reader"); // ex:
			// nscl/scls/gscl/applications/D2_DoorAnnc

			List<String> listLink = getLink(listReferenceURI); // ex:
																// gscl/applications/D2_Temp

			System.out.println("listLink = " + listLink);

			GetDescriptor(listLink);

			// 設定 wanem的packet loss rate
			statusCode = ChangeWanem.start(iPacketLossRate);
			if (statusCode == 200) {
				delay(1000);

				// 4. 啟動NSCL的ping程式
				boolean isStarted = startPingOnNSCL("nscl/applications/Ping/process/start");
				if (isStarted) {
					delay(1000);

					// 5. 送 command 給 device
					for (int i = 1; i <= iLoopCount; i++) {
						System.out.println("i = " + i);
						String cmd = "gscl/applications/D2_Reader/readers/savecards";
						sendCmdToDevice(cmd, iDataSize);
						System.out.println();

						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}

					// // 送指令給NSCL，停止 NSCL裡的ping的程式
					// statusCode = ChangeWanem.start(0);
					// System.out.println("ChangeWanem(0) = " + statusCode);
					// stopPingOnNSCL("nscl/applications/Ping/process/stop");
				}
			}
		}
	}

	private boolean stopPingOnNSCL(String cmd) {
		try {
			// 停止NSCL的ping程式
			String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + cmd;
			HttpClient httpclient = new HttpClient();
			PostMethod httpMethod = new PostMethod(url);
			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			int statusCode = httpclient.executeMethod(httpMethod);
			System.out.println("stopPingOnNSCL() = " + statusCode);
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static void delay(int i) {
		try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private boolean startPingOnNSCL(String cmd) {
		try {
			// 啟動NSCL的ping程式
			String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + cmd;
			HttpClient httpclient = new HttpClient();
			PostMethod httpMethod = new PostMethod(url);
			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			int statusCode = httpclient.executeMethod(httpMethod);
			System.out.println("startPingOnNSCL() = " + statusCode);
			return true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private void sendCmdToDevice(String cmd, int test_data_length) {
		try {
			// 送 command 給 device
			String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + cmd;
			HttpClient httpclient = new HttpClient();
			PostMethod httpMethod = new PostMethod(url);
			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			StringBuilder sb = new StringBuilder();
			sb.append("<obj>");
			sb.append("<str name='data' val='" + str_generator(test_data_length) + "'/>");
			sb.append("<str name='timestamp' val='" + System.currentTimeMillis() + "'/>");
			sb.append("</obj>");

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

	private void GetDescriptor(List<String> listLink) {

		for (String strLink : listLink) {
			try {
				String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + strLink + "/containers/DESCRIPTOR/contentInstances/latest/content";
				HttpClient httpclient = new HttpClient();
				GetMethod httpMethod = new GetMethod(url);
				httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

				// StringBuilder sb = new StringBuilder();
				// sb.append("<om2m:subscription xmlns:om2m='http://uri.etsi.org/m2m'>");
				// sb.append("<om2m:contact>nscl/applications/" + app + context
				// + "</om2m:contact>");
				// sb.append("</om2m:subscription>");

				// StringRequestEntity requestEntity = new
				// StringRequestEntity(sb.toString(), "application/xml",
				// "UTF-8");
				// httpMethod.setRequestEntity(requestEntity);

				int statusCode = httpclient.executeMethod(httpMethod);
				System.out.println(statusCode);
				String strResponseBody = httpMethod.getResponseBodyAsString();
				System.out.println(strResponseBody);

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
				String url = "http://" + NSCL_IP + ":" + NSCL_Port + "/om2m/" + strReference;
				HttpClient httpclient = new HttpClient();
				GetMethod httpMethod = new GetMethod(url);
				httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");
				int statusCode = httpclient.executeMethod(httpMethod);
				System.out.println(statusCode);
				String strResponseBody = httpMethod.getResponseBodyAsString();
				System.out.println(strResponseBody);

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

	static class MyHandler implements HttpHandler {
		public void handle(HttpExchange t) throws IOException {

			String body = "";
			int i;
			char c;
			try {
				InputStream is = t.getRequestBody();

				while ((i = is.read()) != -1) {
					c = (char) i;
					body = (String) (body + c);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Received notification:");
			System.out.println(body);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder dBuilder = dbf.newDocumentBuilder();

				Document notifyDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(body.getBytes("utf-8"))));

				String contentInstance64 = notifyDoc.getElementsByTagName("om2m:representation").item(0).getTextContent();
				System.out.println("ContentInstance (Base64-encoded):\n" + contentInstance64 + "\n");

				String contentInstance = new String(DatatypeConverter.parseBase64Binary(contentInstance64));
				System.out.println("ContentInstance:\n" + contentInstance + "\n");

				Document instanceDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(contentInstance.getBytes("utf-8"))));
				String content64 = instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();
				System.out.println("Content (Base64-encoded):\n" + content64 + "\n");

				final String content = new String(DatatypeConverter.parseBase64Binary(content64));
				System.out.println("Content:\n" + content + "\n");

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
