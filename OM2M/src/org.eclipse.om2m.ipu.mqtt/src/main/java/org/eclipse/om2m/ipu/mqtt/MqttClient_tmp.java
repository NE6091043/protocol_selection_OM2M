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

import java.nio.ByteBuffer;
import io.moquette.proto.messages.AbstractMessage.QOSType;
import io.moquette.proto.messages.PublishMessage;
import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MqttClient_tmp implements RestClientService {

	/** Logger: */
	private static Log LOGGER = LogFactory.getLog(MqttClient_tmp.class);
	/** implemented specific protocol name */
	private static String protocol = "mqtt";

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
		ResponseConfirm responseConfirm = new ResponseConfirm(StatusCode.STATUS_OK);

		// get the Payload from requestIndication
		String representation = requestIndication.getRepresentation();

		String targetID = requestIndication.getTargetID();

		// get the method from requestIndication
		String method = requestIndication.getMethod();

		PublishMessage msg = new PublishMessage();
		msg.setQos(QOSType.LEAST_ONE);
		msg.setTopicName(targetID);
		ByteBuffer sendBuffer = ByteBuffer.wrap(representation.getBytes());
		msg.setPayload(sendBuffer);
		MqttBroker.getServer().internalPublish(msg);
		LOGGER.info("MQTT Client > " + responseConfirm);

		return responseConfirm;
	}

	
}
