package org.eclipse.om2m.ipu.mqtt;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class MqttProxyMonitor {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(MqttProxyMonitor.class);

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
	public MqttProxyMonitor(SclService scl) {
		MqttProxyMonitor.SCL = scl;
	}

	public SclService getScl() {
		return SCL;
	}

	public void setScl(SclService scl) {
		MqttProxyMonitor.SCL = scl;
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

			System.out.println("topic = " + topic);
			System.out.println("msg = " + msg);

			if (isNotify(msg)) {
				// 在nscl接收到gscl用mqtt送來的notification。
				// String strContent = parseContent(msg);
				RequestIndication requestIndication = new RequestIndication();
				requestIndication.setMethod(Constants.METHOD_CREATE);
				requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
				requestIndication.setRepresentation(msg);

				requestIndication.setBase("");
				requestIndication.setTargetID(topic);

				System.out.println(SCL.doRequest(requestIndication));

			} else if (isJson(msg)) {
				// 在nscl收到gscl用mqtt送來的測試資料。
				calculateDelay(msg);
			} else {
				// 在gscl接收到nscl用mqtt傳來的command。
				// 或是在gscl接收到device用mqtt送來的om2m指令。
				RequestIndication requestIndication = new RequestIndication();

				// Get the request parameters
				String queryString = getUriQueryString(topic);
				String targetId = topic;
				if (queryString == null) {
					requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
				} else {
					System.out.println("queryString = " + queryString);
					Map<String, List<String>> parameters = getParamsFromQuery(queryString);
					
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

					System.out.println("parameters = " + parameters);
					targetId = topic.substring(0, topic.indexOf('?'));
				}
				System.out.println("targetId = " + targetId);

				if (msg.length() == 0) {
					requestIndication.setMethod(Constants.METHOD_EXECUTE);
					requestIndication.setTargetID(targetId);
				} else {
					requestIndication.setMethod(Constants.METHOD_CREATE);
					requestIndication.setTargetID(targetId);
				}

				requestIndication.setRepresentation(msg);
				System.out.println(SCL.doRequest(requestIndication));
			}

		} catch (Exception ex) {
			System.out.println(ex);
		}
	}

	public static void output(String msg) {
		for (int i = 1; i <= 5; i++) {
			System.out.println(msg);
		}
	}

	private static String parseContent(String msg) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder dBuilder = dbf.newDocumentBuilder();

			Document notifyDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(msg.getBytes("utf-8"))));

			String contentInstance64 = notifyDoc.getElementsByTagName("om2m:representation").item(0).getTextContent();
			System.out.println("ContentInstance (Base64-encoded):\n" + contentInstance64 + "\n");

			String contentInstance = new String(DatatypeConverter.parseBase64Binary(contentInstance64));
			System.out.println("ContentInstance:\n" + contentInstance + "\n");

			Document instanceDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(contentInstance.getBytes("utf-8"))));
			String content64 = instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();
			System.out.println("Content (Base64-encoded):\n" + content64 + "\n");

			final String content = new String(DatatypeConverter.parseBase64Binary(content64));
			System.out.println("Content:\n" + content + "\n");

			return content;

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static boolean isNotify(String msg) {
		// regex
		String patternStr = "<om2m:notify xmlns:om2m=\"http://uri.etsi.org/m2m\" xmlns:xmime=\"http://www.w3.org/2005/05/xmlmime\">";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(msg);
		if (matcher.find()) {
			return true;
		}
		return false;
	}

	private static boolean isBase64(String msg) {
		if ((msg.length() % 4) != 0) {
			return false;
		}

		String decodeMsg = new String(DatatypeConverter.parseBase64Binary(msg));
		String encodeMsg = DatatypeConverter.printBase64Binary(decodeMsg.getBytes());

		if (msg.equalsIgnoreCase(encodeMsg)) {
			return true;
		}

		return false;
	}

	private static String getUriQueryString(String msg) {
		int idx = msg.lastIndexOf('?');
		if (idx < 0) {
			return null;
		} else {
			return msg.substring(msg.lastIndexOf('?') + 1);
		}
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
