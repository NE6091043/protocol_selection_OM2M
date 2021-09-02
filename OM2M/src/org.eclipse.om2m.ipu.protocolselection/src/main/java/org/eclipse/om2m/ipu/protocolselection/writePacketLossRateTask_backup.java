package org.eclipse.om2m.ipu.protocolselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.obix.Obj;
import org.eclipse.om2m.commons.obix.Str;
import org.eclipse.om2m.commons.obix.io.ObixEncoder;
import org.eclipse.om2m.commons.resource.Container;
import org.eclipse.om2m.commons.resource.ContentInstance;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;

public class writePacketLossRateTask_backup extends TimerTask {

	private static String pingCount = System.getProperty("org.eclipse.om2m.protocolselection.pingcount", "10");
	private static String NSCL_IP = System.getProperty("org.eclipse.om2m.remoteNsclAddress", "192.168.72.90");

	/** Logger */
	private static Log LOGGER = LogFactory.getLog(writePacketLossRateTask_backup.class);

	/** Discovered SCL service */
	private static SclService SCL;

	private String strSymbolicName = "";

	public String getSymbolicName() {
		return strSymbolicName;
	}

	public void setSymbolicName(String strSymbolicName) {
		this.strSymbolicName = strSymbolicName;
	}

	public writePacketLossRateTask_backup() {
	}

	public static void setScl(SclService scl) {
		writePacketLossRateTask_backup.SCL = scl;
	}

	private static void output(String message) {
		System.out.println(message);
	}

	@Override
	public void run() {
		output(this.getClass().getName() + " Task run at " + new Date());
		try {

			// 去 ping NSCL/GSCL，在用WANem測試時，改成去ping WANem.
			NSCL_IP = "192.168.72.105";
			String strPacketLossRate = pingPacketLossRate(NSCL_IP);
			output("IP = " + NSCL_IP + ", PacketLossRate = " + strPacketLossRate);

			if (strPacketLossRate != null) {
				// 將 NSCL 的 packet loss rate寫到reource tree裡。
				WritePacketLossRate(strPacketLossRate);
			}
			System.out.println();

			// // discovery all device
			// ResponseConfirm response = discoveryAllDevice();
			// if (response.getStatusCode().equals(StatusCode.STATUS_OK)) {
			// String responseData = response.getRepresentation();
			// // output("discoveryAllDevice = " + responseData);
			// HashMap<String, String> mapAllDeviceIP =
			// getAllDeviceIP(responseData);
			// output("mapAllDeviceIP = " + mapAllDeviceIP);
			// for (String strDeviceName : mapAllDeviceIP.keySet()) {
			// System.out.println();
			// String strIP = mapAllDeviceIP.get(strDeviceName);
			//
			// // 去 ping device，在用WANem測試時，改成去ping WANem.
			// strIP = "192.168.72.208";
			// String strPacketLossRate = pingPacketLossRate(strIP);
			// output("IP = " + strIP + ", PacketLossRate = " +
			// strPacketLossRate);
			//
			// if (strPacketLossRate != null) {
			//
			// // 將device的packet loss rate寫到reource tree裡。
			// WritePacketLossRate(strDeviceName, strPacketLossRate);
			// }
			// System.out.println();
			// }
			// }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void WritePacketLossRate(String strPacketLossRate) {
		String appId = "NetworkStatus";
		String containerId = "PacketLossRate";

		// Build a RequestIndication
		RequestIndication requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);

		Obj obj = null;
		String content = "";
		ContentInstance contentInstance = null;

		/***************************************************************************
		 * 檢查是否有device的packet loss rate的container resource是否存在，如果不存在就新增 check
		 * whether container resource of packet loss rate of device exists.
		 **************************************************************************/
		if (!checkExists(containerId)) {
			addToContainers(appId, containerId);
		}

		/***************************************************************************
		 * Create the "PacketLossRate" contentInstance
		 **************************************************************************/
		obj = new Obj();
		obj.add(new Str("PacketLossRate(%)", strPacketLossRate));
		content = ObixEncoder.toString(obj);
		contentInstance = new ContentInstance(content.getBytes());
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId + Refs.CONTAINERS_REF + "/" + containerId + Refs.CONTENTINSTANCES_REF);
		requestIndication.setRepresentation(contentInstance);
		SCL.doRequest(requestIndication);
	}

	// private void WritePacketLossRate(String strDeviceName, String
	// strPacketLossRate) {
	// String appId = "DeviceNetworkStatus";
	// String containerId = strDeviceName + "_PacketLossRate";
	//
	// // Build a RequestIndication
	// RequestIndication requestIndication = new RequestIndication();
	// requestIndication.setMethod(Constants.METHOD_CREATE);
	// requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
	//
	// Obj obj = null;
	// String content = "";
	// ContentInstance contentInstance = null;
	//
	// /***************************************************************************
	// * 檢查是否有device的packet loss rate的container resource是否存在，如果不存在就新增 check
	// * whether container resource of packet loss rate of device exists.
	// **************************************************************************/
	// if (!checkExists(containerId)) {
	// addToContainers(appId, containerId);
	// }
	//
	// /***************************************************************************
	// * Create the "PacketLossRate" contentInstance
	// **************************************************************************/
	// obj = new Obj();
	// obj.add(new Str("PacketLossRate(%)", strPacketLossRate));
	// content = ObixEncoder.toString(obj);
	// contentInstance = new ContentInstance(content.getBytes());
	// requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF +
	// "/" + appId + Refs.CONTAINERS_REF + "/" + containerId +
	// Refs.CONTENTINSTANCES_REF);
	// requestIndication.setRepresentation(contentInstance);
	// SCL.doRequest(requestIndication);
	// }

	/**
	 * 將資料新增到 containers裡
	 */
	private static void addToContainers(String appId, String containerId) {
		String targetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId + Refs.CONTAINERS_REF;
		Container container = new Container(containerId);
		container.setId(containerId);
		container.setMaxNrOfInstances((long) 6); // 設定contentInstance最多只能有5筆資料。
		SCL.doRequest(new RequestIndication(Constants.METHOD_CREATE, targetId, Constants.ADMIN_REQUESTING_ENTITY, container));
	}

	private static boolean checkExists(String strDeviceName) {
		String targetId;
		targetId = Constants.SCL_ID + Refs.DISCOVERY_REF;

		// Create request
		// http://127.0.0.1:8080/om2m/gscl/discovery?searchString=resourceid/strDeviceName_PacketLossRate
		RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, targetId, Constants.ADMIN_REQUESTING_ENTITY);

		// 設定 Request 的參數
		Map<String, List<String>> parameters = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		values.add(Constants.SEARCH_STRING_RES_ID + strDeviceName);
		parameters.put("searchString", values);
		requestIndication.setParameters(parameters);

		// 送出 Request
		ResponseConfirm response = SCL.doRequest(requestIndication);
		String responseData = response.getRepresentation();
		if (response.getStatusCode().equals(StatusCode.STATUS_OK)) {
			output(responseData);

			// 檢查topic出現的次數
			String patternStr = "<om2m:matchSize>(\\d+)</om2m:matchSize>";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(responseData);
			if (matcher.find()) {
				int iCount = Integer.parseInt(matcher.group(1));
				if (iCount == 0) { // 表示topic沒有包含在Container裡
					return false;
				} else { // 否則表示topic有包含在Container裡
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isLinux() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
	}

	public static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	private String pingPacketLossRate(String strIP) {
		Runtime runtime = Runtime.getRuntime();
		Process process = null; //
		String line = null; // 返回行信息
		InputStream is = null; // 輸入流
		InputStreamReader isr = null; // 字節流
		BufferedReader br = null;
		try {
			if (isWindows()) {
				process = runtime.exec("ping -n " + pingCount + " " + strIP); // PING
			} else if (isLinux()) {
				process = runtime.exec("ping -c " + pingCount + " " + strIP); // PING
			} else {
				return null;
			}

			is = process.getInputStream(); // 實例化輸入流
			isr = new InputStreamReader(is);// 把輸入流轉換成字節流
			br = new BufferedReader(isr);// 從字節中讀取文本
			while ((line = br.readLine()) != null) {
				output(line);

				if (line.contains("packet loss") || line.contains("%")) {
					String patternStr = "(\\d+)%";
					Pattern pattern = Pattern.compile(patternStr);
					Matcher matcher = pattern.matcher(line);
					while (matcher.find()) {
						String strPacketLossRate = matcher.group(1);
						return strPacketLossRate;
					}
				}
			}
			is.close();
			isr.close();
			br.close();

		} catch (IOException e) {
			System.out.println(e);
			runtime.exit(1);
		}
		return null;
	}

	/**
	 * 搜尋所有的device
	 * 
	 * @param strProtocol
	 * @return
	 */
	private ResponseConfirm discoveryAllDevice() {
		// http://127.0.0.1:8080/om2m/gscl/discovery?searchString=resourcetype/device
		String targetId = Constants.SCL_ID + Refs.DISCOVERY_REF;

		// Create a request
		// http://127.0.0.1:8080/om2m/gscl/discovery?searchString=resourcetype/device
		RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, targetId, Constants.ADMIN_REQUESTING_ENTITY);

		// 設定 Request 的參數
		Map<String, List<String>> parameters = new HashMap<String, List<String>>();
		List<String> values = new ArrayList<String>();
		values.add(Constants.SEARCH_STRING_RES_TYPE + "device");
		parameters.put("searchString", values);
		requestIndication.setParameters(parameters);

		// 送出 Request
		ResponseConfirm response = SCL.doRequest(requestIndication);
		return response;
	}

	private HashMap<String, String> getAllDeviceIP(String responseData) {
		HashMap<String, String> mapAllDeviceIP = new HashMap<String, String>();
		String patternStr = "<reference>(.scl/applications/\\w+)</reference>";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(responseData);
		while (matcher.find()) {
			String strTargetID = matcher.group(1);
			int idx = strTargetID.lastIndexOf("/");
			String appId = strTargetID.substring(idx + 1);

			// ex:
			// http://127.0.0.1:8080/om2m/gscl/applications/D1/containers/DATA/contentInstances/latest/content
			RequestIndication requestIndication = new RequestIndication(Constants.METHOD_RETREIVE, strTargetID + "/containers/DATA/contentInstances/latest/content", Constants.ADMIN_REQUESTING_ENTITY);
			ResponseConfirm responseURI = SCL.doRequest(requestIndication);
			if (responseURI.getStatusCode().equals(StatusCode.STATUS_OK)) {
				String strIP = "";
				String patternStr2 = "<str name=[\"'][\\w]*ip[\\w]*[\"'] val=[\"'](\\S+)[\"']/>";
				Pattern pattern2 = Pattern.compile(patternStr2);
				Matcher matcher2 = pattern2.matcher(responseURI.getRepresentation().toLowerCase());
				while (matcher2.find()) {
					strIP = matcher2.group(1);
					System.out.println("appId = " + appId + ", strIP = " + strIP);
					mapAllDeviceIP.put(appId, strIP);
				}
			}
		}
		return mapAllDeviceIP;
	}
}
