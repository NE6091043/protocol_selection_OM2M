package org.eclipse.om2m.ipu.mqtt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.Application;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

public class MqttProxyMonitor_tmp {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(MqttProxyMonitor_tmp.class);

	/** Discovered SCL service */
	static SclService SCL;

	/** application name is "MQTT_Proxy" */
	private static String appId = "MQTT";

	/**
	 * Application point of contact for the leds controller
	 * {@link MqttProxyController}
	 */
	public final static String APOCPATH = "mqtt";

	/** gscl mqtt ip. */
	final static String MQTT_IP = System.getProperty("org.eclipse.om2m.mqttBroker.IP", "127.0.0.1");

	/** gscl mqtt port. */
	final static String MQTT_PORT = System.getProperty("org.eclipse.om2m.mqttBroker.Port", "1883");

	/**
	 * Constructor
	 * 
	 * @param scl
	 *            - discovered SCL
	 */
	public MqttProxyMonitor_tmp(SclService scl) {
		MqttProxyMonitor_tmp.SCL = scl;
	}

	public SclService getScl() {
		return SCL;
	}

	public void setScl(SclService scl) {
		MqttProxyMonitor_tmp.SCL = scl;
	}

	public void createMqttResources() {
		// Build RequestIndication
		RequestIndication requestIndication = new RequestIndication();
		requestIndication.setMethod(Constants.METHOD_CREATE);
		requestIndication.setTargetID(Constants.SCL_ID + Refs.APPLICATIONS_REF);
		requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		requestIndication.setRepresentation(new Application(appId, APOCPATH));
		LOGGER.info("The requestIndication : " + requestIndication);

		// Create the "MQTT_Proxy" application
		ResponseConfirm responseConfirm = SCL.doRequest(requestIndication);
		LOGGER.info(responseConfirm);
	}

	public void deleteMqttResources() {
		// Delete the MQTT_Proxy application
		String targetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/" + appId;
		SCL.doRequest(new RequestIndication(Constants.METHOD_DELETE, targetId, Constants.ADMIN_REQUESTING_ENTITY));
	}

	public static ResponseConfirm publish(String strTopic, String strData) {
		// INFO MqttProxyMonitor.publish 唐君豪
		int qos = 1;
		String broker = "tcp://" + MQTT_IP + ":" + MQTT_PORT;
		String clientId = "sample";
		MemoryPersistence persistence = new MemoryPersistence();
		MqttClient sampleClient = null;
		try {
			sampleClient = new MqttClient(broker, clientId, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			System.out.println("Connecting to broker: " + broker);
			sampleClient.connect(connOpts);
			System.out.println("Connected");
			System.out.println("Publishing message: " + strData);
			MqttMessage message = new MqttMessage(strData.getBytes());
			message.setQos(qos);
			sampleClient.publish(strTopic, message);
			System.out.println("Message published");
			sampleClient.disconnect();
			System.out.println("Disconnected");
		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			if (sampleClient != null) {
				try {
					sampleClient.disconnect();
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
			me.printStackTrace();
		}
		return new ResponseConfirm(StatusCode.STATUS_OK);
	}

	/**
	 * 將MQTT Broker收到的topic和msg放到RequestIndication裡，送給OM2M。
	 */
	public static void sendToOM2M(String topic, String msg) {

		try {

			System.out.println("xxxxxxxxxxxxxxxxx");
			// msg = deCompress(msg);

			System.out.println("topic = " + topic);
			System.out.println("msg = " + msg);

			System.out.println("tttttttttttttttttt");

			if (isJson(msg)) {
				calculateDelay(msg);
			} else {
				// Get the request parameters
				String queryString = getUriQueryString(topic);
				Map<String, List<String>> parameters = getParamsFromQuery(queryString);

				// 取得topic裡的targetId
				String targetId = topic.substring(0, topic.indexOf('?'));
				RequestIndication requestIndication = new RequestIndication();
				if (msg.length() == 0) {
					requestIndication.setMethod(Constants.METHOD_EXECUTE);
					requestIndication.setTargetID(targetId);
				} else {
					requestIndication.setMethod(Constants.METHOD_CREATE);
					requestIndication.setTargetID(targetId);
				}

				// set the requestIndication parameters
				if (parameters.containsKey("Authorization")) {
					requestIndication.setRequestingEntity(parameters.get("Authorization").get(0));
				}

				if (parameters.containsKey("authorization")) {
					requestIndication.setRequestingEntity(parameters.get("authorization").get(0));
				}

				if (!parameters.containsKey("Authorization") && !parameters.containsKey("authorization")) {
					throw new Exception("not found authorization parameter");
				}

				requestIndication.setRepresentation(msg);
				SCL.doRequest(requestIndication);
			}

		} catch (Exception ex) {
			System.out.println(ex);
		}

	}

	private static String deCompress(String msg) {

		String result = "";
		try {
			// 將接受到的字符串轉換為byte數組
			byte[] values = msg.getBytes("iso8859-1");

			// 解壓縮這個byte數組

			System.out.println("111111111111111111");
			values = ZLibUtils.decompress(values);
			System.out.println("222222222222222222");

			result = new String(values, "utf-8");
			System.out.println("333333333333333333");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return msg;
		}

		return result;
	}

	private static String getUriQueryString(String msg) {
		return msg.substring(msg.lastIndexOf('?') + 1);
	}

	public static Map<String, List<String>> getParamsFromQuery(String query) {
		Map<String, List<String>> parameters = new HashMap<String, List<String>>();
		if (query != null) {
			String[] pairs = query.split("[&]");
			for (String pair : pairs) {
				String[] param = pair.split("[=]");

				String key = null;
				String value = null;
				if (param.length > 0) {
					key = param[0];
				}
				if (param.length > 1) {
					value = param[1];
				}
				if (parameters.containsKey(key)) {
					parameters.get(key).add(value);
				} else {
					List<String> values = new ArrayList<String>();
					values.add(value);
					parameters.put(key, values);
				}
			}
		}
		return parameters;
	}

	/**
	 * 計算收到訊息的Delay
	 * 
	 * @param strPayload
	 * @param strDataSize
	 * @throws JSONException
	 * @throws IOException
	 */
	private static void calculateDelay(String strPayload) throws JSONException, IOException {

		JSONObject jsonTodevice = new JSONObject(strPayload);

		if (!jsonTodevice.has("data")) {
			throw new JSONException("no data property");
		}

		// 取得Device送出訊息時的時間
		if (jsonTodevice.has("timestamp")) {
			String strDataSize = String.valueOf(jsonTodevice.getString("data").getBytes().length);

			long previous_time = Long.parseLong(jsonTodevice.getString("timestamp"));

			// 取得目前時間
			long current_time = new Date().getTime();

			// 將時間相減，算出Delay(ms)
			long result = current_time - previous_time;

			// 將結果寫到文字檔裡
			FileWriter fw = new FileWriter("mqtt_received_delay_" + strDataSize + "Bytes.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
			BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
			bw.write(String.valueOf(result));
			bw.write("\r\n");
			bw.flush();
			bw.close();
		}
	}

	/**
	 * 判斷是否是json結構
	 */
	public static boolean isJson(String str) {
		try {
			// 檢查是否有順利轉回成json
			new JSONObject(str);
		} catch (JSONException e) {
			return false;
		}
		return true;
	}
}
