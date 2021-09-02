package org.eclipse.om2m.ipu.mqtt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;
import org.eclipse.om2m.ipu.service.IpuService;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static BundleContext context;

	/** Logger */
	private static Log logger = LogFactory.getLog(Activator.class);

	/** SCL service tracker */
	private ServiceTracker<Object, Object> sclServiceTracker;

	private MqttProxyMonitor mqttProxyMonitor;

	/** the mqtt broker */
	private MqttBroker mqttBroker;

	public static MqttClient mqtt_client;

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		RegisterService();

		// Start the MQTT server
		mqttBroker = new MqttBroker();
		mqttBroker.startBroker();
		logger.info("MQTT broker started.");

		trackSclService(getContext());

		// 連線到broker
		if (Constants.SCL_ID.equalsIgnoreCase("gscl")) {
			URI uri = new URI("tcp://" + Constants.NSCL_IP + ":1883");
			connectToMQTTBroker(uri);
		}
	}

	private void RegisterService() {

		// Register the Rest MQTT Client
		logger.info("Register MQTT RestClientService..");
		getContext().registerService(RestClientService.class.getName(), new OM2MMqttClient(), null);
		logger.info("MQTT RestClientService is registered.");

		// Register the Rest MqttProxyController
		logger.info("Register MqttProxyController..");
		getContext().registerService(IpuService.class.getName(), new MqttProxyController(), null);
		logger.info("MqttProxyController is registered.");

	}

	private void trackSclService(BundleContext bundleContext) {
		/***************************************************************************
		 * track the SCL service
		 **************************************************************************/
		sclServiceTracker = new ServiceTracker<Object, Object>(bundleContext, SclService.class.getName(), null) {
			public void removedService(ServiceReference<Object> reference, Object service) {
				logger.info("SclService removed");
				try {
				} catch (IllegalArgumentException e) {
					logger.error("Error removing SclService", e);
				}
			}

			public Object addingService(ServiceReference<Object> reference) {
				logger.info("SclService discovered");
				SclService scl = (SclService) this.context.getService(reference);
				try {
					mqttProxyMonitor = new MqttProxyMonitor(scl);
					// mqttProxyMonitor.createMqttResources();

				} catch (Exception e) {
					logger.error("Error adding SclService", e);
				}
				return scl;
			}
		};
		// Open service trackers
		sclServiceTracker.open();
		logger.info("SclService opened");
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		if (mqttBroker != null) {
			mqttBroker.stopBroker();
		}
		logger.info("MQTT broker stopped.");

		if (mqttProxyMonitor.getScl() != null) {
			mqttProxyMonitor.deleteMqttResources();
		}
		logger.info("MQTT proxy application stopped.");
	}

	static BundleContext getContext() {
		return context;
	}

	public static boolean mqtt_publish(URI uri, String topic, MqttMessage message) {

		// 檢查連線
		connectToMQTTBroker(uri);

		try {
			out("send message using mqtt");
			mqtt_client.publish(topic, message);
			return true;
		} catch (MqttException me) {
			System.out.println("reason " + me.getReasonCode());
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			me.printStackTrace();

			StringBuilder sb = new StringBuilder();
			sb.append("msg " + me.getMessage());

			// 將錯誤訊息寫到文字檔裡
			try {
				FileWriter fw = new FileWriter("test_mqtt_error_msg.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
				BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
				bw.write(sb.toString());
				bw.write("\r\n");
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void connectToMQTTBroker(URI uri) {
		while (mqtt_client == null || !mqtt_client.isConnected()) {
			try {
				String broker = "tcp://" + uri.getHost() + ":" + uri.getPort();
				System.out.println("Connecting to broker: " + broker);

				String clientId = MqttClient.generateClientId();
				MemoryPersistence persistence = new MemoryPersistence();
				mqtt_client = new MqttClient(broker, clientId, persistence);

				MqttConnectOptions opt = new MqttConnectOptions();
				opt.setMaxInflight(1000);

				// 連接前清空會話信息
				opt.setCleanSession(true);

				// 設置心跳時間
				// A value of 0 disables keepalive processing in the client.
				opt.setKeepAliveInterval(0);

				mqtt_client.setCallback(new OM2MMqttCallback());
				mqtt_client.connect(opt);
				System.out.println("Connected");
				break;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("connection error");
				System.out.println();

				// 將錯誤訊息寫到文字檔裡
				try {
					FileWriter fw = new FileWriter("test_mqtt_error_msg.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
					bw.write(e.getMessage() + ", connection error, reconnect");
					bw.write("\r\n");
					bw.flush();
					bw.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	public static void out(String msg) {
		for (int i = 1; i <= 10; i++) {
			System.out.println(msg);
		}
	}
}
