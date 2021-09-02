package org.eclipse.om2m.comm.protocolmanager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LifeTimeTask extends TimerTask {

	/** Logger */
	private static Log LOGGER = LogFactory.getLog(LifeTimeTask.class);

	/** Discovered SCL service */
	private static SclService SCL;

	private static ScheduledExecutorService pool;

	public LifeTimeTask() {
	}

	public LifeTimeTask(ScheduledExecutorService pool) {
		this.pool = pool;
	}

	public static void setScl(SclService scl) {
		LifeTimeTask.SCL = scl;
	}

	/**
	 * 從NSCL上取得所有Protocol的SymbolicName
	 * 
	 * @return
	 */
	private List<String> getAllProtocolSymbolicName() {
		List<String> listProtocols = new ArrayList<String>();
		List<String> listTargetID = searchString("ResourceType", "Protocol");
		for (int i = 0; i < listTargetID.size(); i++) {
			String strTargetID = listTargetID.get(i);
			ResponseConfirm resp = retrieveProtocolBundleURI(strTargetID);
			String strSymbolicName = getSymbolicName(resp);
			if (strSymbolicName != null) {
				listProtocols.add(strSymbolicName);
			}
		}
		return listProtocols;
	}

	private String getSymbolicName(ResponseConfirm resp) {
		if (resp != null) {
			if (resp.getStatusCode().equals(StatusCode.STATUS_OK)) {
				String respData = resp.getRepresentation();
				try {
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbf.newDocumentBuilder();

					Document instanceDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(respData.getBytes("utf-8"))));
					String content64 = instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();

					System.out.println("Content (Base64-encoded):\n" + content64 + "\n");

					final String content = new String(DatatypeConverter.parseBase64Binary(content64));
					System.out.println("Content:\n" + content + "\n");

					/**
					 * Content: <obj> <str val=
					 * "https://dl.dropboxusercontent.com/u/6098562/org.eclipse.om2m.comm.coap-0.8.0-SNAPSHOT.jar"
					 * name="Address"/> <str val="2017/04/21 23:59"
					 * name="Lifetime"/> <str val="org.eclipse.om2m.comm.coap"
					 * name="SymbolicName"/> </obj>
					 */
					String patternStr = "str val=['\"](.+)['\"] name=['\"]SymbolicName['\"]";
					Pattern pattern = Pattern.compile(patternStr);
					Matcher matcher = pattern.matcher(content);

					if (matcher.find()) {
						String strSymbolicName = matcher.group(1);
						return strSymbolicName;
					}

				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParserConfigurationException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	public void run() {
		System.out.println(this.getClass().getName() + " run at " + new Date());
		try {
			List<String> listProtocols = getAllProtocolSymbolicName();
			for (int i = 0; i < listProtocols.size(); i++) {
				String strProtocolName = listProtocols.get(i);

				Bundle[] bundles = Activator.getContext().getBundles();
				for (int j = 0; j < bundles.length; j++) {
					Bundle bundle = bundles[j];

					if (bundle.getSymbolicName().equals(strProtocolName)) {
						List<String> listTargetID = searchString("ResourceID", strProtocolName); // ex.strTargetID=nscl/applications/ProtocolRepository/containers/ProtocolContainer/contentInstances/MQTT
						String strTargetID = listTargetID.get(0);
						output("Check" + strProtocolName + " bundle lifetime at " + new Date());
						ResponseConfirm response2 = retrieveProtocolBundleURI(strTargetID);
						if (response2 != null) {
							if (response2.getStatusCode().equals(StatusCode.STATUS_OK)) {
								String responseData2 = response2.getRepresentation();
								output(responseData2);
								String strLifetime = getProtocolBundleLifetime(responseData2);
								SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
								Date dtLifetime = sdf.parse(strLifetime);
								output(strProtocolName + " bundle lifetime = " + dtLifetime);

								// get current datetime from ntp server
								String TIME_SERVER = "tock.stdtime.gov.tw";
								NTPUDPClient timeClient = new NTPUDPClient();
								InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
								TimeInfo timeInfo = timeClient.getTime(inetAddress);
								long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
								Date dtNow = new Date(returnTime);
								output("current time from ntp = " + dtNow.toString());

								// 檢查lifetime是否到期
								if (dtNow.compareTo(dtLifetime) > 0) {
									bundle.stop();
								} else {
									output(strProtocolName + " bundle lifetime verification successful");
								}
							}
						} else {
							output(strProtocolName + " bundle not found");
						}
						break;
					}
				}
			}

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (BundleException e) {
			e.printStackTrace();
		}

		// 設定下一次的執行時間
		setNextRunTime();
	}

	private void setNextRunTime() {
		// 計算今天一天的剩餘時間
		Date dt = new Date();
		dt.getTime();
		Calendar cal = Calendar.getInstance();
		Date d1 = cal.getTime();
		cal.add(cal.DATE, 1);
		cal.set(cal.HOUR_OF_DAY, 0);
		cal.set(cal.MINUTE, 0);
		cal.set(cal.SECOND, 0);
		Date d2 = cal.getTime();
		long restTime = d2.getTime() - d1.getTime();

		// 在一天之內隨機取一個值
		long rndLong = nextLong(24 * 60 * 60 * 1000);

		// 兩者相加，就是下一次的執行時間
		long delay = restTime + rndLong;
		pool.schedule(new LifeTimeTask(), delay, TimeUnit.MILLISECONDS);
	}

	private long nextLong(long n) {
		Random rng = new Random();
		// error checking and 2^x checking removed for simplicity.
		long bits, val;
		do {
			bits = (rng.nextLong() << 1) >>> 1;
			val = bits % n;
		} while (bits - val + (n - 1) < 0L);
		return val;
	}

	private List<String> searchString(String strPara1, String strPara2) {

		List<String> result = new ArrayList<String>();
		String targetId = Constants.NSCL_ID + Refs.DISCOVERY_REF;

		// Create a request
		// http://127.0.0.1:8080/om2m/nscl/discovery?searchString=SymbolicName/strSymbolicName
		RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, targetId, Constants.ADMIN_REQUESTING_ENTITY);

		// 設定 Request 的參數
		Map<String, List<String>> parameters = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		values.add(strPara1 + "/" + strPara2);
		parameters.put("searchString", values);
		requestIndication.setParameters(parameters);

		// 送出 Request
		ResponseConfirm response = SCL.doRequest(requestIndication);
		if (response != null) {
			if (response.getStatusCode().equals(StatusCode.STATUS_OK)) {
				String strResponse = response.getRepresentation();
				String patternStr = "<reference>(.+)</reference>";
				Pattern pattern = Pattern.compile(patternStr);
				Matcher matcher = pattern.matcher(strResponse);
				while (matcher.find()) {
					String strTargetID = matcher.group(1);
					result.add(strTargetID);
				}
			}
		}
		return result;
	}

	private ResponseConfirm retrieveProtocolBundleURI(String strTargetID) {
		RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, strTargetID, Constants.ADMIN_REQUESTING_ENTITY);
		ResponseConfirm responseURI = SCL.doRequest(requestIndication);
		return responseURI;
	}

	/**
	 * 取得 Protocol Bundle 的lifetime
	 * 
	 * @param responseData
	 * @return
	 */
	private String getProtocolBundleLifetime(String responseData) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbf.newDocumentBuilder();

			Document instanceDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(responseData.getBytes("utf-8"))));
			String content64 = instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();

			System.out.println("Content (Base64-encoded):\n" + content64 + "\n");

			final String content = new String(DatatypeConverter.parseBase64Binary(content64));
			System.out.println("Content:\n" + content + "\n");

			String patternStr = "(\\d{4}(?:/\\d{1,2}){2}.\\d{1,2}:\\d{1,2})";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(content);

			if (matcher.find()) {
				String strLifetime = matcher.group(1);
				output("Lifetime = " + strLifetime);
				return strLifetime;
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void output(String msg) {
		for (int i = 1; i <= 3; i++) {
			System.out.println(msg);
		}
	}
}
