package org.eclipse.om2m.ipu.mqtt;

import io.moquette.server.Server;
import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MqttBroker {

	/** Logger */
	private static Log LOGGER = LogFactory.getLog(MqttBroker.class);

	/** the mqtt broker */
	private static Server server;

	public void startBroker() throws Exception {

		new Thread() {
			public void run() {
				try {
					server = new Server();

					File configFile = new File("./MqttConfig/moquette.conf");
					if (!configFile.exists()) {
						configFile = new File("../../../../../../MqttConfig/moquette.conf");
					}

					server.startServer(configFile);
					LOGGER.info("Using m_config file: " + configFile.getAbsolutePath());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

	}

	public void stopBroker() throws Exception {
		if (server != null) {
			server.stopServer();
		}
	}

	public static Server getServer() {
		return server;
	}
}
