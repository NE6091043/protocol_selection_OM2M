/*******************************************************************************
 * Copyright (c) 2013-2015 LAAS-CNRS (www.laas.fr)
 * 7 Colonel Roche 31077 Toulouse - France
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Monteil (Project co-founder) - Management and initial specification,
 *         conception and documentation.
 *     Mahdi Ben Alaya (Project co-founder) - Management and initial specification,
 *         conception, implementation, test and documentation.
 *     Khalil Drira - Management and initial specification.
 *     Samir Medjiah - Conception and implementation of the CoAP binding.
 *     Rim Frikha - Implementation of the CoAP binding.
 ******************************************************************************/

package org.eclipse.om2m.ipu.mqtt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import io.moquette.proto.messages.AbstractMessage.QOSType;
import io.moquette.proto.messages.PublishMessage;

import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OM2MMqttClient implements RestClientService {

	/** Logger: */
	private static Log LOGGER = LogFactory.getLog(OM2MMqttClient.class);
	/** implemented specific protocol name */
	private static String protocol = "mqtt";

	// public static MqttClient mqtt_client;

	/**
	 * gets the implemented specific protocol name
	 * 
	 * @return protocol name
	 */
	public String getProtocol() {
		return protocol;
	}

	/**
	 * Converts a protocol-independent {@link RequestIndication} object into a
	 * standard CoAP request and sends a standard CoAP request. Converts the
	 * received standard CoAP response into {@link ResponseConfirm} object and
	 * returns it back.
	 * 
	 * @param requestIndication
	 *            - protocol independent request.
	 * @return protocol independent response.
	 */
	public ResponseConfirm sendRequest(RequestIndication requestIndication) {

		LOGGER.info("MQTT Client > " + requestIndication);

		// create the standard final response
		ResponseConfirm responseConfirm = new ResponseConfirm();

		// get the Payload from requestIndication
		String representation = requestIndication.getRepresentation();

		String targetID = requestIndication.getTargetID();

		URI uri = null;
		try {
			uri = new URI(requestIndication.getUrl());
		} catch (URISyntaxException e) {
			LOGGER.error("URI Syntax error", e);
		}

		if (targetID.equals("")) {

			if (uri.getPath().split("/om2m/")[1] != null) {
				targetID = uri.getPath().split("/om2m/")[1];
			}

			System.out.println("uri.toString() = " + uri.toString());
			System.out.println("uri.getHost() = " + uri.getHost());
			System.out.println("uri.getPort() = " + uri.getPort());
			System.out.println("uri.getAuthority() = " + uri.getAuthority());
			System.out.println("uri.getFragment() = " + uri.getFragment());
		}

		// ///////////////////////////////
		String topic = targetID;
		System.out.println("topic = " + topic);
		String content = representation;
		int qos = 1;
		MqttMessage message = new MqttMessage(content.getBytes());
		message.setQos(qos);

		System.out.println("Publishing message: " + content);

		/***************************************************************************
		 * publish message
		 **************************************************************************/

		// while (myPublish(uri, topic, message) == false) {
		//
		// }

		while (Activator.mqtt_publish(uri, topic, message) == false) {
			// 將錯誤訊息寫到文字檔裡
			try {
				FileWriter fw = new FileWriter("test_mqtt_error_msg.txt", true); 
				// True則表示用附加的方式寫到檔案原有內容之後
				BufferedWriter bw = new BufferedWriter(fw); 
				// 將BufferedWeiter與FileWrite物件做連結
				bw.write("republish");
				bw.write("\r\n");
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Message published");

		if (content.length() <= 0) {
			responseConfirm.setStatusCode(StatusCode.STATUS_OK);
		} else {
			responseConfirm.setStatusCode(StatusCode.STATUS_CREATED);
		}
		LOGGER.info("MQTT Client > " + responseConfirm);
		return responseConfirm;
	}

	private boolean myPublish(URI uri, String topic, MqttMessage message) {
		MqttClient mqtt_client = connectToMQTTBroker(uri);
		if (mqtt_client != null) {
			try {
				mqtt_client.publish(topic, message);
				mqtt_client.disconnect();
				return true;
			} catch (MqttPersistenceException e) {
				if (mqtt_client != null && mqtt_client.isConnected()) {
					try {
						mqtt_client.disconnect();
					} catch (MqttException e1) {
						e1.printStackTrace();
					}
				}
				e.printStackTrace();
			} catch (MqttException e) {
				if (mqtt_client != null && mqtt_client.isConnected()) {
					try {
						mqtt_client.disconnect();
					} catch (MqttException e1) {
						e1.printStackTrace();
					}
				}
				e.printStackTrace();
			}
		}
		return false;
	}

	private MqttClient connectToMQTTBroker(URI uri) {
		try {
			String broker = "tcp://" + uri.getHost() + ":" + uri.getPort();
			System.out.println("Connecting to broker: " + broker);

			String clientId = MqttClient.generateClientId();
			MemoryPersistence persistence = new MemoryPersistence();
			MqttClient mqtt_client = new MqttClient(broker, clientId, persistence);

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

			return mqtt_client;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("connection error");
			System.out.println();
		}
		return null;
	}
}
