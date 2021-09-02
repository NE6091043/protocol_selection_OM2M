package org.eclipse.om2m.ipu.protocolselection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
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

public class writePacketLossRateTask extends TimerTask {

	private static Process process = null;

	public static Process getProcess() {
		return process;
	}

	public static void setProcess(Process process) {
		writePacketLossRateTask.process = process;
	}

	// 設定要 ping 的次數
	private static String pingCount = System.getProperty("org.eclipse.om2m.protocolselection.pingcount", "10");

	// private static String pingIP =
	// System.getProperty("org.eclipse.om2m.remoteNsclAddress",
	// "192.168.72.90");

	// 原本是要去 ping NSCL/GSCL，但在用WANem測試時，先改成去ping WANem.
	private static String pingIP = "192.168.72.105";

	/** Logger */
	private static Log LOGGER = LogFactory.getLog(writePacketLossRateTask.class);

	/** Discovered SCL service */
	private static SclService SCL;

	private String strSymbolicName = "";

	public String getSymbolicName() {
		return strSymbolicName;
	}

	public void setSymbolicName(String strSymbolicName) {
		this.strSymbolicName = strSymbolicName;
	}

	public writePacketLossRateTask() {
	}

	public static void setScl(SclService scl) {
		writePacketLossRateTask.SCL = scl;
	}

	private static void output(String message) {
		System.out.println(message);
	}

	@Override
	public void run() {
		output(this.getClass().getName() + " Task run at " + new Date());
		try {

			String strFileName = "test_pingResult_" + pingIP + ".txt";

			// ping IP
			String strPingResult = pingPacketLossRate(pingIP);

			// 將 ping的結果寫到文字檔裡
			writeFile(strPingResult, strFileName, true);

			int iLineNumber = -1;
			int iTTLNumber = -1;

			if (isWindows()) {
				// 計算行數
				iLineNumber = calculateLineNumberForWindows(strFileName);
				System.out.println("iLineNumber = " + iLineNumber);

			} else if (isLinux()) {
				
				// 計算行數
				iLineNumber = calculateLineNumberForLinux(strFileName);
				System.out.println("iLineNumber = " + iLineNumber);
			}

			// 計算ping成功的有幾行
			iTTLNumber = calculateTTLNumber(strFileName);
			System.out.println("iTTLNumber = " + iTTLNumber);
			
			// 計算沒ping成功的有幾行
			int iLossNumber = iLineNumber - iTTLNumber;

			// 計算 packet loss rate
			double dPacketLossRate = (double) iLossNumber / iLineNumber;
			String strPacketLossRate = String.valueOf(dPacketLossRate);
			System.out.println("packet loss rate = " + dPacketLossRate);
			output("IP = " + pingIP + ", PacketLossRate = " + strPacketLossRate);

			if (strPacketLossRate != null) {
				// 將 packet loss rate寫到reource tree裡。
				WritePacketLossRate(strPacketLossRate);

				// 將 packet loss rate寫到檔案裡。
				writeFile(strPacketLossRate, "test_packetlossrate_" + pingIP + ".txt", true);
			}

			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeFile(String line, String strFileName, boolean append) {

		try {
			// 將結果寫到文字檔裡
			FileWriter fw = new FileWriter(strFileName, append); // True則表示用附加的方式寫到檔案原有內容之後
			BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
			bw.write(line);
			bw.write("\r\n");
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int calculateTTLNumber(String strFileName) {
		int iTTLNumber = -1;

		try {
			BufferedReader in = new BufferedReader(new FileReader(strFileName));
			String str;
			int num = 0;
			while ((str = in.readLine()) != null) { // readLine()依序讀取檔案內的一行文字

				if (str.toUpperCase().contains("TTL")) {
					num++; // 每讀一行，num就加1
				}
			}
			iTTLNumber = num;
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return iTTLNumber;
	}

	private int calculateLineNumberForLinux(String strFileName) {

		int iLineNumber = -1;

		try {
			BufferedReader in = new BufferedReader(new FileReader(strFileName));
			String str;
			String strIcmpSeq = "";

			while ((str = in.readLine()) != null) { // readLine()依序讀取檔案內的一行文字
				if (str.length() > 0) {

					// regex
					String patternStr = "icmp_seq=(\\d+).ttl";
					Pattern pattern = Pattern.compile(patternStr);
					Matcher matcher = pattern.matcher(str);
					if (matcher.find()) {
						strIcmpSeq = matcher.group(1);
					}
				}
			}
			in.close();

			iLineNumber = Integer.parseInt(strIcmpSeq);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return iLineNumber;
	}

	private int calculateLineNumberForWindows(String strFileName) {
		int iLineNumber = -1;

		try {
			BufferedReader in = new BufferedReader(new FileReader(strFileName));
			String str;
			int num = 0;
			while ((str = in.readLine()) != null) { // readLine()依序讀取檔案內的一行文字

				if (str.length() > 0) {
					System.out.println(str);
					num++; // 每讀一行，num就加1
				}
			}
			iLineNumber = num - 1;
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return iLineNumber;
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
		StringBuilder sbPingResult = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		// Process process = null; //
		String line = null; // 返回行信息
		InputStream is = null; // 輸入流
		InputStreamReader isr = null; // 字節流
		BufferedReader br = null;
		try {
			if (isWindows()) {
				process = runtime.exec("ping " + strIP + " -w 1000 -t"); // PING
			} else if (isLinux()) {
				process = runtime.exec("ping " + strIP + " -W 1"); // PING
			} else {
				return null;
			}

			is = process.getInputStream(); // 實例化輸入流
			isr = new InputStreamReader(is);// 把輸入流轉換成字節流
			br = new BufferedReader(isr);// 從字節中讀取文本
			while ((line = br.readLine()) != null) {

				// 輸出結果
				output(line);

				sbPingResult.append(line);
				sbPingResult.append("\r\n");
			}
			is.close();
			isr.close();
			br.close();

		} catch (IOException e) {
			System.out.println(e);
			runtime.exit(1);
		}
		return sbPingResult.toString();
	}

}
