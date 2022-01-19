import java.awt.Toolkit;

import java.io.IOException;

import java.io.UnsupportedEncodingException;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

import java.util.Random;

import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.HttpException;

import org.apache.commons.httpclient.methods.DeleteMethod;

import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.commons.httpclient.methods.PostMethod;

import org.apache.commons.httpclient.methods.StringRequestEntity;

import org.eclipse.californium.core.coap.Response;



public class DeviceForEventDriven {
	
	private static int size=200;
	
	private static int sleeptime=50;
	

	public static String GSCL_IP = "140.116.247.69";

	public static String GSCL_Port = "18282";



	public static String deviceIP = "192.168.72.8";

	public static String DeviceID = "D2";

	public static String dataContainerID = "DATA";

	public static String descriptorContainerID = "DESCRIPTOR";

	public static String authorization = "admin:admin";



	private static String NSCL_IP = "192.168.247.69";

	private static String NSCL_Port = "18080";

	private static String DeviceID2="Protocol";

	private static String dataContainerID2="ProtocolSelect";

	

	

	private static String DeviceID3="NetworkStatus";

	private static String dataContainerID3="PacketLossRate";

	

	

	static int temperatureValue = 0;



	public DeviceForEventDriven() {



		

	

	}



	public static void main(String[] args) {



		String strPacketLossRate = "0";

		String strLoopCount = "500";

		//2021.5.11 modified

//		Random rand=new Random();

//		int upperbound=1600;

//		int randomdatasize = rand.nextInt(upperbound);

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



		DeviceForEventDriven device = new DeviceForEventDriven();

		device.run(strPacketLossRate, strLoopCount, strDataSize);

	}



	private static void delay(int i) {

		try {

			Thread.sleep(i);

		} catch (InterruptedException e) {

			e.printStackTrace();

		}

	}



	public void run(String strPacketLossRate, String strLoopCount, String strDataSize) {

		Integer iPacketLossRate = Integer.parseInt(strPacketLossRate);

		Integer iLoopCount = Integer.parseInt(strLoopCount);

		Integer iDataSize = Integer.parseInt(strDataSize);

		int statusCode = 200;

		//int statusCode = ChangeWanem.start(0);

		if (statusCode == 200) {

			delay(1000);

			

			//2021.5.18 modified , not needed

			//DeleteResourceOnGSCL("gscl/applications/" + DeviceID);

			//CreateDeviceResource();



			DeleteResourceOnGSCL("gscl/applications/" + DeviceID + "_Temp");

			DeleteResourceOnGSCL("gscl/applications/" + DeviceID2 + "_Temp");

			DeleteResourceOnGSCL("gscl/applications/" + DeviceID3 + "_Temp");

			CreateTempResource();



			while (!checkSubscription("gscl/applications/" + DeviceID + "_Temp")) {

				delay(5000);

			}



			//statusCode = ChangeWanem.start(iPacketLossRate);

			if (statusCode == 200) {

				delay(1000);

				listenToTemp(iLoopCount, iDataSize);

				//System.out.println("currentPacketLossRate = " + iPacketLossRate);



				// 啟動NSCL的ping程式

				//2021.5.25 no needed

//				boolean isStarted = startPingOnGSCL("gscl/applications/Ping/process/start");

//				if (isStarted) {

//					listenToTemp(iLoopCount, iDataSize);

//				}

			} else {

				System.out.println("change packet loss rate error");

			}

		} else {

			System.out.println("change packet loss rate 0 error");

		}

	

	}



	private boolean startPingOnGSCL(String cmd) {

		try {

			// 啟動GSCL的ping程式

			String url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/" + cmd;

			HttpClient httpclient = new HttpClient();

			PostMethod httpMethod = new PostMethod(url);

			httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");



			int statusCode = httpclient.executeMethod(httpMethod);

			System.out.println("startPingOnGSCL() = " + statusCode);

			return true;

		} catch (UnsupportedEncodingException e) {

			e.printStackTrace();

		} catch (IOException e) {

			e.printStackTrace();

		}

		return false;

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



			// 公告給NSCL

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



			// 公告給NSCL

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

		

			

			

			String url2 = "";

			HttpClient httpclient2;

			PostMethod httpMethod2;

			StringBuilder sb2;

			StringRequestEntity requestEntity2;

			int statusCode2;

			// Create application resource

			url2 = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications";

			httpclient2 = new HttpClient();

			httpMethod2 = new PostMethod(url2);

			httpMethod2.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb2 = new StringBuilder();

			sb2.append("<om2m:application xmlns:om2m='http://uri.etsi.org/m2m' appId='" + DeviceID2 + "_Temp'>");



			// 公告給NSCL

			sb2.append("<om2m:announceTo>");

			sb2.append("<om2m:activated>true</om2m:activated>");

			sb2.append("<om2m:sclList>");

			sb2.append("<reference>nscl</reference>");

			sb2.append("</om2m:sclList>");

			sb2.append("</om2m:announceTo>");



			sb2.append("<om2m:searchStrings>");

			sb2.append("<om2m:searchString>ResourceType/Temp</om2m:searchString>");

			sb2.append("<om2m:searchString>ResourceID/" + DeviceID2 + "_Temp</om2m:searchString>");

			sb2.append("</om2m:searchStrings>");



			sb2.append("</om2m:application>");

			requestEntity2 = new StringRequestEntity(sb2.toString(), "application/xml", "UTF-8");

			httpMethod2.setRequestEntity(requestEntity2);

			statusCode2 = httpclient2.executeMethod(httpMethod2);

			//System.out.println(statusCode);

			// Create protocol container resource

			//int statusCode2;

			url2 = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID2 + "_Temp/containers";

			httpclient2 = new HttpClient();

			httpMethod2 = new PostMethod(url2);

			httpMethod2.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb2 = new StringBuilder();

			sb2.append("<om2m:container xmlns:om2m='http://uri.etsi.org/m2m' om2m:id='" + dataContainerID2 + "'>");

			

			

			sb2.append("<om2m:searchStrings>");

			sb2.append("<om2m:searchString>ResourceType/Container</om2m:searchString>");

			sb2.append("<om2m:searchString>ResourceID/" + DeviceID2 + "_Temp_" + dataContainerID2 + "</om2m:searchString>");

			sb2.append("</om2m:searchStrings>");

			sb2.append("</om2m:container>");

			requestEntity2 = new StringRequestEntity(sb2.toString(), "application/xml", "UTF-8");

			httpMethod2.setRequestEntity(requestEntity2);

			statusCode2 = httpclient2.executeMethod(httpMethod2);

			System.out.println(statusCode2);

			

			

			

			String url3 = "";

			HttpClient httpclient3;

			PostMethod httpMethod3;

			StringBuilder sb3;

			StringRequestEntity requestEntity3;

			int statusCode3;

			// Create application resource

			url3 = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications";

			httpclient3 = new HttpClient();

			httpMethod3 = new PostMethod(url3);

			httpMethod3.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb3 = new StringBuilder();

			sb3.append("<om2m:application xmlns:om2m='http://uri.etsi.org/m2m' appId='" + DeviceID3 + "_Temp'>");



			// 公告給NSCL

			sb3.append("<om2m:announceTo>");

			sb3.append("<om2m:activated>true</om2m:activated>");

			sb3.append("<om2m:sclList>");

			sb3.append("<reference>nscl</reference>");

			sb3.append("</om2m:sclList>");

			sb3.append("</om2m:announceTo>");



			sb3.append("<om2m:searchStrings>");

			sb3.append("<om2m:searchString>ResourceType/Temp</om2m:searchString>");

			sb3.append("<om2m:searchString>ResourceID/" + DeviceID3 + "_Temp</om2m:searchString>");

			sb3.append("</om2m:searchStrings>");



			sb3.append("</om2m:application>");

			requestEntity3 = new StringRequestEntity(sb3.toString(), "application/xml", "UTF-8");

			httpMethod3.setRequestEntity(requestEntity3);

			statusCode3 = httpclient3.executeMethod(httpMethod3);

			//System.out.println(statusCode);

			// Create protocol container resource

			//int statusCode3;

			url3 = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID3 + "_Temp/containers";

			httpclient3 = new HttpClient();

			httpMethod3 = new PostMethod(url3);

			httpMethod3.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

			sb3 = new StringBuilder();

			sb3.append("<om2m:container xmlns:om2m='http://uri.etsi.org/m2m' om2m:id='" + dataContainerID3 + "'>");

			

			

			sb3.append("<om2m:searchStrings>");

			sb3.append("<om2m:searchString>ResourceType/Container</om2m:searchString>");

			sb3.append("<om2m:searchString>ResourceID/" + DeviceID3 + "_Temp_" + dataContainerID3 + "</om2m:searchString>");

			sb3.append("</om2m:searchStrings>");

			sb3.append("</om2m:container>");

			requestEntity3 = new StringRequestEntity(sb3.toString(), "application/xml", "UTF-8");

			httpMethod3.setRequestEntity(requestEntity3);

			statusCode3 = httpclient3.executeMethod(httpMethod3);

			System.out.println(statusCode3);



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

	 * 定時送出溫度資料給GSCL

	 * 

	 * @param iLoopCount

	 * @param iDataSize

	 */

	private void listenToTemp(final Integer iLoopCount, final Integer iDataSize) {

		// 定時送出溫度資料給GSCL
		
		
		new Thread() {
			
			int x=3;
			
//			int lowerbound=0,upperbound=1200;
			
			int n=x;
			public void run() {
				
				while(x>0) {
					
//					if(x%50==0) {
//						lowerbound=500;upperbound=600;
//					}
//					if(x%50==10) {
//						lowerbound=700;upperbound=850;
//					}
//					if(x%50==20) {
//						lowerbound=1000;upperbound=1250;
//					}
//					if(x%50==25) {
//						lowerbound=300;upperbound=550;
//					}
//					if(x%50==37) {
//						lowerbound=800;upperbound=900;
//					}
//					if(x%50==49) {
//						lowerbound=1100;upperbound=1500;
//					}
					
					for (int i = 1; i <= iLoopCount; i++) {

						try {
							
							
//							System.out.println(i+(n-x)*500);	
//
//							System.out.println("ok");	
			

							// Simualte a random measurement of the sensor

							temperatureValue = 10 + (int) (Math.random() * 50);

							//System.out.println("temperatureValue = " + temperatureValue);

//							Random rand=new Random();
//							final int randomdatasize=lowerbound + rand.nextInt((upperbound - lowerbound) + 1);

							// Create a data contentInstance

							try {
								
								if(i==50) {
									size=180;
								}
								
								if(i==100) {
									size=350;
								}
								
								if(i==100) {
									size=500;
								}
								
								//second time
								if(i==250) {
									size=500;
								}
								
								//third time
								if(i==350) {
									size=800;
								}
								
								if(i==400) {
									size=900;
								}
								
								if(i==450) {
									size=1200;
								}

								String url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID + "_Temp/containers/" + dataContainerID + "/contentInstances";

								HttpClient httpclient = new HttpClient();

								PostMethod httpMethod = new PostMethod(url);

								httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");

								StringBuilder sb = new StringBuilder();

								
								sb.append("<obj>");
								sb.append("<str name='data' val='" + foo(size, i) + "'/>");
								sb.append("<str name='index' val='" + (i+(n-x)*500) + "'/>");
								sb.append("<str name='timestamp' val='" + 0 +"'/>");
								sb.append("</obj>");


								StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");
								
								httpMethod.setRequestEntity(requestEntity);

								int status = httpclient.executeMethod(httpMethod);

								

//								System.out.println(status);
//
//								System.out.println("+----------------+");



							} catch (Exception e) {

								e.printStackTrace();

							}



							// Wait for 1 seconds then loop

							Thread.sleep(sleeptime);


						} catch (InterruptedException e) {

							e.printStackTrace();

						}

					}
					--x;
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