package org.eclipse.om2m.ipu.protocolselection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.obix.Obj;
import org.eclipse.om2m.commons.obix.Str;
import org.eclipse.om2m.commons.obix.io.ObixEncoder;
import org.eclipse.om2m.commons.resource.Application;
import org.eclipse.om2m.commons.resource.Container;
import org.eclipse.om2m.commons.resource.ContentInstance;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;

public class ProtocolSelectionMonitor {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(ProtocolSelectionMonitor.class);

	/** Discovered SCL service */
	static SclService SCL;

	/**
	 * Application point of contact for the controller
	 * {@link MqttProxyController}
	 */
	public final static String APOCPATH = "process";

	/**
	 * Constructor
	 * 
	 * @param scl
	 *            - discovered SCL
	 */
	public ProtocolSelectionMonitor(SclService scl) {
		ProtocolSelectionMonitor.SCL = scl;
	}

	public SclService getScl() {
		return SCL;
	}

	public void setScl(SclService scl) {
		ProtocolSelectionMonitor.SCL = scl;
	}

	public void start() {
		createResource();
		// writePacketLossRatePeriod();
		// warmup();
	}

	private void warmup() {
		Random ran = new Random();
		for (int i = 1; i <= 5; i++) {
			Integer iPacketLossRate = ran.nextInt(5) + 28;
			WritePacketLossRate(String.valueOf(iPacketLossRate));
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

	private static void output(String message) {
		System.out.println(message);
	}

	/***
	 * Bundle啟動後，定時將GSCL與NSCL之間的packet loss rate寫到resource tree裡。
	 */
	private void writePacketLossRatePeriod() {
		long period = 1000; // 每秒執行一次
		ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
		writePacketLossRateTask task = new writePacketLossRateTask();
		pool.scheduleWithFixedDelay(task, 60000, period, TimeUnit.MILLISECONDS);
	}

	public void createResource() {

		// Build a RequestIndication
		RequestIndication requestIndication;

		// 建立 NetworkStatus 的 application resource
		requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		requestIndication.setRepresentation(new Application("NetworkStatus"));
		LOGGER.info("The requestIndication : " + requestIndication);
		SCL.doRequest(requestIndication);

		// 建立 protocol 的 application resource
		requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		requestIndication.setRepresentation(new Application("Ping", APOCPATH));
		LOGGER.info("The requestIndication : " + requestIndication);
		SCL.doRequest(requestIndication);
	}

	public void deleteResources() {
		String targetId = "";
		// Delete NetworkStatus of application resource
		targetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + "NetworkStatus";
		SCL.doRequest(new RequestIndication(Constants.METHOD_DELETE, targetId, Constants.ADMIN_REQUESTING_ENTITY));

		// Delete protocol of application resource
		targetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + "protocol";
		SCL.doRequest(new RequestIndication(Constants.METHOD_DELETE, targetId, Constants.ADMIN_REQUESTING_ENTITY));
	}

	public static String getDeviceLatestData(String appId) {
		// Build a RequestIndication
		RequestIndication request = new RequestIndication(Constants.METHOD_RETREIVE, Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId + Refs.CONTAINERS_REF + "/" + "DATA"
				+ Refs.CONTENTINSTANCES_REF + "/latest/content", Constants.ADMIN_REQUESTING_ENTITY);

		LOGGER.info("getDeviceLatestData [" + request + "]");

		// send a request
		return SCL.doRequest(request).getRepresentation();
	}

	public static Integer getDevicePacketLossRate(String deviceID) {

		// Build a RequestIndication
		RequestIndication request = new RequestIndication();
		request.setMethod(Constants.METHOD_RETREIVE);
		request.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		String strTargetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/DeviceNetworkStatus" + Refs.CONTAINERS_REF + "/" + deviceID + "_PacketLossRate" + Refs.CONTENTINSTANCES_REF;
		/***************************************************************************
		 * 取得所有 Device Packet loss rate的contentInstance的ID
		 **************************************************************************/
		ArrayList<String> listContentInstance = new ArrayList<String>();

		try {
			// send a request
			request.setTargetID(strTargetId);
			String strBody = SCL.doRequest(request).getRepresentation();

			// regex
			String strContentInstanceId = "";
			String patternStr = "om2m:contentInstance om2m:id=\"(\\w\\w_\\d+)\"";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(strBody);
			while (matcher.find()) {
				strContentInstanceId = matcher.group(1);
				listContentInstance.add(strContentInstanceId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		/***************************************************************************
		 * 取得所有contentInstance的內容
		 **************************************************************************/
		ArrayList<Integer> listPacketLossRate = new ArrayList<Integer>();
		for (String strContentInstanceId : listContentInstance) {
			try {
				// send a request
				request.setTargetID(strTargetId + "/" + strContentInstanceId + "/content");
				String strBody = SCL.doRequest(request).getRepresentation();
				String strPacketLossRate = "";
				String patternStr = "<str val=[\"'](\\d+)[\"'] name=[\"']packetlossrate.*[\"']/>";
				Pattern pattern = Pattern.compile(patternStr);
				Matcher matcher = pattern.matcher(strBody.toLowerCase());
				while (matcher.find()) {
					strPacketLossRate = matcher.group(1);
					listPacketLossRate.add(Integer.parseInt(strPacketLossRate));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("listPacketLossRateSize = " + listPacketLossRate.size());
		System.out.println("listPacketLossRate = " + listPacketLossRate);

		/***************************************************************************
		 * 將packet loss rate做平均
		 **************************************************************************/
		int sum = 0;
		for (int i = 0; i < listPacketLossRate.size(); i++) {
			sum += listPacketLossRate.get(i);
		}
		int iAveragePacketLossRate = (int) Math.round((double) sum / listPacketLossRate.size());
		System.out.println("AveragePacketLossRate = " + iAveragePacketLossRate);

		return iAveragePacketLossRate;
	}
}
