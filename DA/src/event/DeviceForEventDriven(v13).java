

import java.io.IOException;

import java.io.UnsupportedEncodingException;

import java.util.Random;

import java.util.regex.Matcher;



import java.util.regex.Pattern;



import org.apache.commons.httpclient.HttpClient;

import org.apache.commons.httpclient.HttpException;

import org.apache.commons.httpclient.methods.DeleteMethod;



import org.apache.commons.httpclient.methods.GetMethod;



import org.apache.commons.httpclient.methods.PostMethod;



import org.apache.commons.httpclient.methods.StringRequestEntity;



import org.eclipse.californium.core.coap.Response;







public class DeviceForEventDriven {

	

	

	

	private static int LoopCount=5000;;

	

	private static int DataSize=500;

	

	private static int sleeptime=1000;

	



	public static String GSCL_IP = "140.116.247.69";



	public static String GSCL_Port = "18282";



	public static String deviceIP = "192.168.72.3";



	public static String DeviceID = "D2";



	public static String dataContainerID = "DATA";



	public static String descriptorContainerID = "DESCRIPTOR";



	public static String authorization = "admin:admin";

	

	

	static int plot=10;

	

	public static void shutdown() throws HttpException, IOException{



		String url = "http://140.116.247.69:9000/shutdown";



		HttpClient httpclient = new HttpClient();



		PostMethod httpMethod = new PostMethod(url);



		StringBuilder sb = new StringBuilder();

		

		sb.append(plot);



		StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");



		httpMethod.setRequestEntity(requestEntity);



		int statuscode = httpclient.executeMethod(httpMethod);

		

		String str=new String(httpMethod.getResponseBody());

		

		System.out.println(str);

		

	}





	public DeviceForEventDriven() {

		

	}







	public static void main(String[] args) throws HttpException, IOException {





		DeviceForEventDriven device = new DeviceForEventDriven();



		device.run(LoopCount, DataSize);

		

		



	}







	private static void delay(int i) {



		try {



			Thread.sleep(i);



		} catch (InterruptedException e) {



			e.printStackTrace();



		}



	}







	public void run(Integer iLoopCount, Integer iDataSize) throws HttpException, IOException {







		delay(1000);



		



		//2021.5.18 modified , not needed



		//DeleteResourceOnGSCL("gscl/applications/" + DeviceID);



		//CreateDeviceResource();







		DeleteResourceOnGSCL("gscl/applications/" + DeviceID + "_Temp");



		CreateTempResource();







		while (!checkSubscription("gscl/applications/" + DeviceID + "_Temp")) {



			delay(5000);



		}



		delay(1000);

		

//		start();

		

		SendData(iLoopCount, iDataSize);

		

		



	}

	

	private void start() throws HttpException, IOException {

		

		String url = "http://140.116.247.69:9000/start";



		HttpClient httpclient = new HttpClient();



		PostMethod httpMethod = new PostMethod(url);



		StringBuilder sb = new StringBuilder();



		StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");



		httpMethod.setRequestEntity(requestEntity);



		int statuscode = httpclient.executeMethod(httpMethod);

		

		String str=new String(httpMethod.getResponseBody());

		

		System.out.println(str);

	}

	

	private void stop_decision() throws HttpException, IOException {

		

		String url = "http://140.116.247.69:9000/stop";



		HttpClient httpclient = new HttpClient();



		PostMethod httpMethod = new PostMethod(url);



		StringBuilder sb = new StringBuilder();



		StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");



		httpMethod.setRequestEntity(requestEntity);



		int statuscode = httpclient.executeMethod(httpMethod);

		

		String str=new String(httpMethod.getResponseBody());

		

		System.out.println(str);

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



	 * 定時送出溫度資料給GSCL



	 * 



	 * @param iLoopCount



	 * @param iDataSize



	 */



	private void SendData(final Integer iLoopCount, final Integer iDataSize) {



		// 定時送出溫度資料給GSCL

		

		

		new Thread() {

			

//			int x=1;

			

//			int lowerbound=0,upperbound=1200;

			

//			int n=x;

			public void run() {

				



					

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

				

					Random rand=new Random();

					

					for (int i = 1; i <= iLoopCount+1; i++) {

						

						if(i==iLoopCount+1) {

							try {

								shutdown();

							} catch (IOException e) {

								// TODO Auto-generated catch block

								e.printStackTrace();

							}

							break;

						}

						

						int x=rand.nextInt(2);

						

						if(1<=i && i<=200) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=500;

							}

						}else if(200<i && i<=400) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=500;

							}

						}else if(400<i && i<=800) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=1500;

							}

						}else if(800<i && i<=1000) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=1000;

							}

						}else if(1000<i && i<=1200) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=1500;

							}

						}else if(1200<i && i<=1400) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=500;

							}

						}else if(1400<i && i<=1800) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=500;

							}

						}else if(1800<i && i<=2000) {

							if(x==1) {

								DataSize=1500;

							}else {

								DataSize=500;

							}

						}else if(2000<i && i<=2200) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=500;

							}

						}else if(2200<i && i<=2400) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=500;

							}

						}else if(2400<i && i<=2800) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=1500;

							}

						}else if(2800<i && i<=3000) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=500;

							}

						}else if(3000<i && i<=3200) {

							if(x==1) {

								DataSize=500;

							}else {

								DataSize=1000;

							}

						}else if(3200<i && i<=3400) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=1000;

							}

						}else if(3400<i && i<=3800) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=1500;

							}

						}else if(3800<i && i<=4000) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=500;

							}

						}else if(4000<i && i<=4200) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=500;

							}

						}else if(4200<i && i<=4400) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=500;

							}

						}else if(4400<i && i<=4800) {

							if(x==1) {

								DataSize=1000;

							}else {

								DataSize=1500;

							}

						}else if(4800<i && i<=5000) {

							if(x==1) {

								DataSize=100;

							}else {

								DataSize=500;

							}

						}

						

						try {

							

							

//							System.out.println(i+(n-x)*500);	

//

//							System.out.println("ok");	

			



							// Simualte a random measurement of the sensor



//							temperatureValue = 10 + (int) (Math.random() * 50);



							//System.out.println("temperatureValue = " + temperatureValue);



//							Random rand=new Random();

//							int randomdatasize=0 + rand.nextInt((1000 - 0) + 1);



							// Create a data contentInstance



							try {

								





								String url = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/" + DeviceID + "_Temp/containers/" + dataContainerID + "/contentInstances";



								HttpClient httpclient = new HttpClient();



								PostMethod httpMethod = new PostMethod(url);



								httpMethod.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");



								StringBuilder sb = new StringBuilder();



								

								sb.append("<obj>");

//								sb.append("<str name='data' val='" + foo(DataSize, i) + "'/>");

								sb.append("<str name='data' val='" + str_generator(DataSize, i) + "'/>");

								sb.append("<str name='index' val='" + i + "'/>");

								sb.append("<str name='timestamp' val='" + System.currentTimeMillis() +"'/>");

								sb.append("<str name='efficiency' val='" + 0.0 +"'/>");

								sb.append("</obj>");





								StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(), "application/xml", "UTF-8");

								

								httpMethod.setRequestEntity(requestEntity);



								int status = httpclient.executeMethod(httpMethod);



								System.out.println(i+"---------------------\n");



								System.out.println(status+"---------------------\n");





							} catch (Exception e) {



								e.printStackTrace();



							}







							// Wait for 1 seconds then loop



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