import java.io.IOException;

import java.io.UnsupportedEncodingException;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

import java.util.Random;

import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.methods.DeleteMethod;

import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.commons.httpclient.methods.PostMethod;

import org.apache.commons.httpclient.methods.StringRequestEntity;

import org.eclipse.californium.core.coap.Response;



public class DeviceForEventDriven {

	

	public static String GSCL_IP = "140.116.247.69";

	public static String GSCL_Port = "18282";

	
	// loop count
	private static Integer loopcount=5000;
	// size 10
	private static int size=200;	
	// request rate 1s
	private static int sleeptime=500;
	
	public static String deviceIP = "192.168.72.8";

	public static String DeviceID = "D2";

	public static String dataContainerID = "DATA";

	public static String descriptorContainerID = "DESCRIPTOR";

	public static String authorization = "admin:admin";


	static int temperatureValue = 0;



	public DeviceForEventDriven() {


	}



	public static void main(String[] args) {


		DeviceForEventDriven device = new DeviceForEventDriven();

		device.run(loopcount);

	}



	private static void delay(int i) {

		try {

			Thread.sleep(i);

		} catch (InterruptedException e) {

			e.printStackTrace();

		}

	}



	public void run(Integer loopcount) {


		int statusCode = 200;


		if (statusCode == 200) {

			delay(1000);

			//2021.5.18 modified , not needed

			//DeleteResourceOnGSCL("gscl/applications/" + DeviceID);

			//CreateDeviceResource();



			DeleteResourceOnGSCL("gscl/applications/" + DeviceID + "_Temp");

			CreateTempResource();



			while (!checkSubscription("gscl/applications/" + DeviceID + "_Temp")) {

				delay(5000);

			}


			if (statusCode == 200) {

				delay(1000);

				listenToTemp(loopcount);


			} else {

				System.out.println("change packet loss rate error");

			}

		} else {

			System.out.println("change packet loss rate 0 error");

		}


	}


	

	private boolean checkSubscription(String strTargetId) {

		System.out.println("checkSubscription: ");

		try {

			strTargetId = strTargetId + "/containers/DATA/contentInstances/subscriptions";

			String url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/" + strTargetId;

			HttpClient httpclient = new HttpClient();

			GetMethod httpMethod = new GetMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);

			String strBody = httpMethod.getResponseBodyAsString();

			System.out.println(strBody);

			// regex

			String patternStr = "<om2m:namedReference id=\"(.+)\">";

			Pattern pattern = Pattern.compile(patternStr);

			Matcher matcher = pattern.matcher(strBody);

			if (matcher.find()) {

				return true;

			}

		} catch (Exception e) {

			e.printStackTrace();

		}



		return false;

	}



	private void CreateTempResource() {

		System.out.println("Create temp resource on GSCL...");



		String url = "";

		HttpClient httpclient;

		PostMethod httpMethod;

		StringBuilder sb;

		StringRequestEntity requestEntity;

		int statusCode;

		try {

			// Create application resource

			url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications";

			httpclient = new HttpClient();

			httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb = new StringBuilder();

			sb.append("<om2m:application xmlns:om2m='http://uri.etsi.org/m2m' appId='" + DeviceID + "_Temp'>");



			// ?????????NSCL

			sb.append("<om2m:announceTo>");

			sb.append("<om2m:activated>true</om2m:activated>");

			sb.append("<om2m:sclList>");

			sb.append("<reference>nscl</reference>");

			sb.append("</om2m:sclList>");

			sb.append("</om2m:announceTo>");



			sb.append("<om2m:searchStrings>");

			sb.append("<om2m:searchString>ResourceType/Temp</om2m:searchString>");

			sb.append("<om2m:searchString>ResourceID/" + DeviceID + "_Temp</om2m:searchString>");

			sb.append("</om2m:searchStrings>");



			sb.append("</om2m:application>");

			requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

			httpMethod.setRequestEntity(requestEntity);

			statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);



			// Create DATA container resource

			url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID + "_Temp/containers";

			httpclient = new HttpClient();

			httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb = new StringBuilder();

			sb.append("<om2m:container xmlns:om2m='http://uri.etsi.org/m2m' om2m:id='" + dataContainerID + "'>");



			// ?????????NSCL

			// sb.append("<om2m:announceTo>");

			// sb.append("<om2m:activated>true</om2m:activated>");

			// sb.append("<om2m:sclList>");

			// sb.append("<reference>nscl</reference>");

			// sb.append("</om2m:sclList>");

			// sb.append("</om2m:announceTo>");



			sb.append("<om2m:searchStrings>");

			sb.append("<om2m:searchString>ResourceType/Container</om2m:searchString>");

			sb.append("<om2m:searchString>ResourceID/" + DeviceID + "_Temp_" + dataContainerID + "</om2m:searchString>");

			sb.append("</om2m:searchStrings>");

			sb.append("</om2m:container>");	

			requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

			httpMethod.setRequestEntity(requestEntity);

			statusCode = httpclient.executeMethod(httpMethod);

			System.out.println(statusCode);

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



	/**

	 * ???????????????????????????GSCL

	 * 

	 * @param iLoopCount

	 * @param iDataSize

	 */

	private void listenToTemp(final Integer iLoopCount) {

		// ???????????????????????????GSCL

		new Thread() {

			public void run() {

				for (int i = 1; i <= iLoopCount; i++) {

					try {

						System.out.println(i);	

						System.out.println("ok");	


						// Create a data contentInstance

						try {

							String url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID + "_Temp/containers/" + dataContainerID + "/contentInstances";

							HttpClient httpclient = new HttpClient();

							PostMethod httpMethod = new PostMethod(url);

							httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

							StringBuilder sb = new StringBuilder();

							
							sb.append("<obj>");

							//original

							//sb.append("<str name='data' val='" + str_generator(iDataSize, i) + "'/>");

							
							//2021.12.20
							//mod
							//first time
							if(i==1000) {
								size=700;
								sleeptime=100;
							}
							
							//second time
							if(i==2500) {
								size=300;
								sleeptime=50;
							}
							
							//third time
							if(i==3500) {
								size=500;
								sleeptime=300;
							}

							sb.append("<str name='data' val='" + foo(size, i) + "'/>");

							sb.append("<str name='timestamp' val='" + System.currentTimeMillis() + "'/>");

							//2021.9.14 mod

							sb.append("<str name='index' val='" + i + "'/>");

							sb.append("</obj>");



							StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

							httpMethod.setRequestEntity(requestEntity);

							int status = httpclient.executeMethod(httpMethod);

							

							System.out.println(status);

							System.out.println("+----------------+");



						} catch (Exception e) {

							e.printStackTrace();

						}

						Thread.sleep(sleeptime);


					} catch (InterruptedException e) {

						e.printStackTrace();

					}

				}

			}

		}.start();

	}

	
	  static String foo(int size, int order) {
		    // mod

		    // 2021.11.14

		    // java char size == 2 bytes

		    

		    // mod

		    // 2021.12.9

			// StringBuffer out = new StringBuffer();

			// java char size == 2 bytes

			  

		    String res="";

		    

		    for (int i = 0; i < size; ++i) {

		      res+='a';

		    }

		    return res;

		  }


	



	static void output(Response response) {

		if (response != null && !response.getPayloadString().equals("")) {

			System.out.println(response.getPayloadString());

		}

	}



	/**

	 * ??????????????????????????????

	 */

	static String str_generator(int size, int order) {

		StringBuffer out = new StringBuffer();

		String strChars = ascii_letters() + digits();

		//append order to value

		if(order < 10)

			out.append("00" + Integer.toString(order));

		else if(order < 100)

			out.append("0" + Integer.toString(order));

		else

			out.append(Integer.toString(order));

		

		for (int i = 0; i < size - 3; i++) {

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



}