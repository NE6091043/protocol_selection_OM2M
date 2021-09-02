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

package org.eclipse.om2m.comm.coap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;

//import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
//import ch.ethz.inf.vs.californium.coap.OptionSet;
//import ch.ethz.inf.vs.californium.coap.Request;
//import ch.ethz.inf.vs.californium.coap.Response;
//import ch.ethz.inf.vs.californium.coap.CoAP;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OM2MCoapClient implements RestClientService {

	/** Logger: */
	private static Log LOGGER = LogFactory.getLog(OM2MCoapClient.class);
	/** implemented specific protocol name */
	private static String protocol = "coap";

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

		LOGGER.debug("CoAP Client > " + requestIndication);
		// create the standard final response
		ResponseConfirm responseConfirm = new ResponseConfirm();

		// get the Payload from requestIndication
		String representation = requestIndication.getRepresentation();

		// get the authorization (uri_query option) from requestIndication
		String authorization = requestIndication.getRequestingEntity();

		// create the code in order to create a coap request
		CoAP.Code code = null;

		// get the method from requestIndication
		String method = requestIndication.getMethod();
		// setting the CoAP method in its code and setting the payload if there
		// is any
		switch (method) {
		case "RETRIEVE":
			code = CoAP.Code.GET;
			break;
		case "CREATE":
			code = CoAP.Code.POST;
			break;
		case "UPDATE":
			code = CoAP.Code.PUT;
			break;
		case "DELETE":
			code = CoAP.Code.DELETE;
			break;
		case "EXECUTE":
			code = CoAP.Code.POST;
			break;
		default:
			return new ResponseConfirm();
		}

		// create a coap request
		CoapClient client = new CoapClient();
		client.useCONs();

		NetworkConfig config = NetworkConfig.getStandard();
		// config.setInt(NetworkConfig.Keys.ACK_TIMEOUT, 1200);
		// config.setFloat(NetworkConfig.Keys.ACK_RANDOM_FACTOR, 1.2f);
		System.out.println("MAX_TRANSMIT_WAIT = " + config.getLong(NetworkConfig.Keys.MAX_TRANSMIT_WAIT));
		System.out.println("ACK_TIMEOUT = " + config.getLong(NetworkConfig.Keys.ACK_TIMEOUT));
		System.out.println("ACK_RANDOM_FACTOR = " + config.getFloat(NetworkConfig.Keys.ACK_RANDOM_FACTOR));
		client.setTimeout(config.getLong(NetworkConfig.Keys.MAX_TRANSMIT_WAIT));

		// get the URI from requestIndication
		String url = requestIndication.getUrl();

		// set the request URI
		if (authorization.equals("")) {
			client.setURI(url);
		} else {
			client.setURI(url + "?authorization=" + authorization);
		}

		out("send message using coap");
		
		// get the response
		CoapResponse response = null;
		switch (code) {
		case GET:
			response = client.get();
			break;
		case POST:
			response = client.post(representation, MediaTypeRegistry.TEXT_PLAIN);
			break;
		case PUT:
			response = client.put(representation, MediaTypeRegistry.TEXT_PLAIN);
			break;
		case DELETE:
			response = client.delete();
			break;
		default:
			return new ResponseConfirm();
		}

		if (response == null) {
			// // 將錯誤訊息寫到文字檔裡
			// try {
			// FileWriter fw = new FileWriter("test_coap_error_msg.txt", true);
			// // True則表示用附加的方式寫到檔案原有內容之後
			// BufferedWriter bw = new BufferedWriter(fw); //
			// 將BufferedWeiter與FileWrite物件做連結
			// bw.write("response == null");
			// bw.write("\r\n");
			// bw.flush();
			// bw.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// }

			// responseConfirm.setStatusCode(StatusCode.STATUS_REQUEST_TIMEOUT);

		} else {

			if (response.getOptions().isContentFormat(MediaTypeRegistry.APPLICATION_LINK_FORMAT)) {

				String linkFormat = new String(response.getPayload());
				// fill in the representation of the responseConfirm
				responseConfirm.setRepresentation(linkFormat);
			} else {
				responseConfirm.setRepresentation(new String(response.getPayload()));
			}

			// set the responseConfirm statusCode
			CoAP.ResponseCode returncode = response.getCode();
			responseConfirm.setStatusCode(getRestStatusCode(returncode));
		}

		LOGGER.debug("CoAP Client > " + responseConfirm);
		return responseConfirm;
	}

	/**
	 * Converts a standard CoAP status code into a protocol-independent
	 * {@link StatusCode} object.
	 * 
	 * @param statusCode
	 *            - standard CoAP status code.
	 * @return protocol independent status.
	 */
	public static StatusCode getRestStatusCode(CoAP.ResponseCode CoAPstatusCode) {
		StatusCode standardCode = null;

		// 2.02 2.03 2.04 2.05
		if (CoAPstatusCode == CoAP.ResponseCode.DELETED || CoAPstatusCode == CoAP.ResponseCode.VALID || CoAPstatusCode == CoAP.ResponseCode.CHANGED || CoAPstatusCode == CoAP.ResponseCode.CONTENT) {
			return StatusCode.STATUS_OK;
		} else if (CoAPstatusCode == CoAP.ResponseCode.CREATED) {
			// 2.01
			standardCode = StatusCode.STATUS_CREATED;
		} else if (CoAPstatusCode == CoAP.ResponseCode.BAD_REQUEST) {
			// 4.00
			standardCode = StatusCode.STATUS_BAD_REQUEST;
		} else if (CoAPstatusCode == CoAP.ResponseCode.UNAUTHORIZED) {
			// 4.01
			standardCode = StatusCode.STATUS_PERMISSION_DENIED;
		} else if (CoAPstatusCode == CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT) {
			// 4.15
			standardCode = StatusCode.STATUS_UNSUPPORTED_MEDIA_TYPE;
		} else if (CoAPstatusCode == CoAP.ResponseCode.FORBIDDEN) {
			// 4.03
			standardCode = StatusCode.STATUS_FORBIDDEN;
		} else if (CoAPstatusCode == CoAP.ResponseCode.NOT_FOUND) {
			// 4.04
			standardCode = StatusCode.STATUS_NOT_FOUND;
		} else if (CoAPstatusCode == CoAP.ResponseCode.METHOD_NOT_ALLOWED) {
			// 4.05
			standardCode = StatusCode.STATUS_METHOD_NOT_ALLOWED;
		} else if (CoAPstatusCode == CoAP.ResponseCode.INTERNAL_SERVER_ERROR) {
			// 5.00
			standardCode = StatusCode.STATUS_INTERNAL_SERVER_ERROR;
		} else if (CoAPstatusCode == CoAP.ResponseCode.NOT_IMPLEMENTED) {
			// 5.01
			standardCode = StatusCode.STATUS_NOT_IMPLEMENTED;
		} else if (CoAPstatusCode == CoAP.ResponseCode.BAD_GATEWAY) {
			// 5.02
			standardCode = StatusCode.STATUS_BAD_GATEWAY;
		} else if (CoAPstatusCode == CoAP.ResponseCode.SERVICE_UNAVAILABLE) {
			// 5.03
			standardCode = StatusCode.STATUS_SERVICE_UNAVAILABLE;
		} else if (CoAPstatusCode == CoAP.ResponseCode.GATEWAY_TIMEOUT) {
			// 5.04
			standardCode = StatusCode.STATUS_GATEWAY_TIMEOUT;
		}
		return standardCode;
	}
	
	public static void out(String msg) {
		for (int i = 1; i <= 10; i++) {
			System.out.println(msg);
		}
	}
}
