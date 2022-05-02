package org.eclipse.om2m.core.notifier;


import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.eclipse.om2m.core.Activator;

public class ChangeWanem {

	public static String WanemIP = "192.168.101.132";
	
	public static int start=0;
	
	//sleep 0.5s
	public static int sleeptime=500;
	
//	public static int cnt=0;
	
	// 500s => change 1000 times 
	public static int period=1800;
	
	public static int loss_rate=0;
	
	public static int bandwidth=100;

	public ChangeWanem() {
		// TODO Auto-generated constructor stub
	}

//	public static void main(String[] args) {
//	
//		
//	}
	
//	public static void startchange() {
//		new Thread() {
//			public void run() {
//				for (int i = 1; i <= period; i++) {
//					
//					if(loss_rate<0 || i<=200) {
//						bandwidth=0;
//						loss_rate=0.0;
//					}
//					
//					if(i>200 && i<=300) {
//						loss_rate+=((double)15/100);
//						bandwidth=100;
//					}
//					if(i>300 && i<=400) {
//						loss_rate-=((double)15/100);
//						bandwidth=100;
//					}
//					if(i>400 && i<=600) {
//						loss_rate=0.0;
//						bandwidth=100;
//					}
//					if(i>600 && i<=1000) {
//						loss_rate+=((double)25/100);
//						bandwidth=100;
//					}
//					if(i>1000 && i<=1400) {
//						loss_rate-=((double)25/100);
//						bandwidth=200;
//					}
//					if(i>1400) {
//						loss_rate=0.0;
//						bandwidth=200;
//					}
//					
//					change(loss_rate,bandwidth);
//					
//					System.out.println("loss rate: "+loss_rate);
//					System.out.println("bandwidth: "+bandwidth);
//					
//						
//					try {
//						Thread.sleep(sleeptime);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}.start();
//	}
	
	public static void start_change() {
		change(loss_rate,bandwidth);
	}
	
	
	public static int change(Integer iPacketLossRate,Integer bandwidth) {
		String strPacketLossRate = iPacketLossRate.toString();
		String applied_bandwidth= bandwidth.toString();

		String url = "http://" + WanemIP + "/WANem/index-advanced.php";
		HttpClient httpclient = new HttpClient();
		PostMethod httpMethod = new PostMethod(url);
		httpMethod.addRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		httpMethod.addRequestHeader("Referer", url);
		httpMethod.addRequestHeader("Connection", "keep-alive");
		httpMethod.addRequestHeader("Cookie", "PHPSESSID=" + getCookie());

		try {
			NameValuePair[] body = new NameValuePair[35];
			body[0] = new NameValuePair("txtLimit1", "1000");
			body[1] = new NameValuePair("selSym1", "Yes");
			body[2] = new NameValuePair("txtBandwidthAuto1", "Other");
			body[3] = new NameValuePair("txtBandwidth1", applied_bandwidth);
			body[4] = new NameValuePair("txtDelay1", "0");
			body[5] = new NameValuePair("txtLoss1", strPacketLossRate);
			body[6] = new NameValuePair("txtDup1", "0");
			body[7] = new NameValuePair("txtReorder1", "0");
			body[8] = new NameValuePair("txtCorrupt1", "0");
			body[9] = new NameValuePair("txtDelayJitter1", "0");
			body[10] = new NameValuePair("txtLossCorrelation1", "0");
			body[11] = new NameValuePair("txtDupCorrelation1", "0");

			body[12] = new NameValuePair("txtReorderCorrelation1", "0");
			body[13] = new NameValuePair("txtDelayCorrelation1", "0");
			body[14] = new NameValuePair("txtGap1", "0");

			body[15] = new NameValuePair("selDelayDistribution1", "-N/A-");
			body[16] = new NameValuePair("selidtyp1", "none");
			body[17] = new NameValuePair("txtidtmr1", "");

			body[18] = new NameValuePair("txtidsctmr1", "");
			body[19] = new NameValuePair("selrndtyp1", "none");
			body[20] = new NameValuePair("txtrndmttflo1", "");

			body[21] = new NameValuePair("txtrndmttfhi1", "");
			body[22] = new NameValuePair("txtrndmttrlo1", "");
			body[23] = new NameValuePair("txtrndmttrhi1", "");

			body[24] = new NameValuePair("selrcdtyp1", "none");
			body[25] = new NameValuePair("txtrcdmttflo1", "");
			body[26] = new NameValuePair("txtrcdmttfhi1", "");

			body[27] = new NameValuePair("txtrcdmttrlo1", "");
			body[28] = new NameValuePair("txtrcdmttrhi1", "");
			body[29] = new NameValuePair("txtSrc1", "any");

			body[30] = new NameValuePair("txtSrcSub1", "");
			body[31] = new NameValuePair("txtDest1", "any");
			body[32] = new NameValuePair("txtDestSub1", "");

			body[33] = new NameValuePair("txtPort1", "any");
			body[34] = new NameValuePair("btnApply", "Apply settings");
			httpMethod.setRequestBody(body);
			int statusCode = httpclient.executeMethod(httpMethod);
			// System.out.println("statusCode = " + statusCode);

			return statusCode;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static String getCookie() {
		String url = "http://" + WanemIP + "/WANem/index-advanced.php";
		HttpClient httpclient = new HttpClient();
		PostMethod httpMethod = new PostMethod(url);

		try {
			NameValuePair[] body = new NameValuePair[2];
			body[0] = new NameValuePair("selInt", "eth0");
			body[1] = new NameValuePair("btnAdvanced", "Start");
			httpMethod.setRequestBody(body);
			int statusCode = httpclient.executeMethod(httpMethod);
			// System.out.println("statusCode = " + statusCode);
			Header cookie = httpMethod.getResponseHeader("Set-Cookie");
			// System.out.println("cookie.getName() = " + cookie.getName());
			// System.out.println("cookie.getValue() = " + cookie.getValue());

			String phpsessid = "";
			String patternStr = "PHPSESSID=(.*?);";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(cookie.getValue());
			while (matcher.find()) {
				phpsessid = matcher.group(1);
			}
			// System.out.println(phpsessid);
			return phpsessid;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
