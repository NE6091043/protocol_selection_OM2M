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

package org.eclipse.om2m.ipu.websocket;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OM2MWebsocketClient implements RestClientService {

	/** Logger: */
	private static Log LOGGER = LogFactory.getLog(OM2MWebsocketClient.class);

	/** implemented specific protocol name */
	private static String protocol = "ws";

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

		LOGGER.info("Websocket Client > " + requestIndication);

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
			System.out.println("uri.getPath() = " + uri.getPath());
			System.out.println("uri.getQuery() = " + uri.getQuery());
			System.out.println("uri.getScheme() = " + uri.getScheme());
			System.out.println("uri.getUserInfo() = " + uri.getUserInfo());
			System.out.println("uri.getHost() = " + uri.getHost());
			System.out.println("uri.getPort() = " + uri.getPort());
			System.out.println("uri.getAuthority() = " + uri.getAuthority());
			System.out.println("uri.getFragment() = " + uri.getFragment());
		}

		System.out.println("targetID = " + targetID);
		System.out.println("targetID = " + targetID);
		System.out.println("targetID = " + targetID);
		System.out.println("targetID = " + targetID);
		System.out.println("targetID = " + targetID);
		System.out.println("targetID = " + targetID);

		/***************************************************************************
		 * insert targetID(contact) to representation
		 **************************************************************************/
		System.out.println("representation = " + representation);
		if (representation.length() > 0) {
			int idx = representation.indexOf("om2m:notify");
			if (idx >= 0) {
				String xml = representation;
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = null;
				try {
					db = dbf.newDocumentBuilder();
					InputSource is = new InputSource();
					is.setCharacterStream(new StringReader(xml));
					try {
						Document doc = db.parse(is);

						NodeList nList = doc.getElementsByTagName("om2m:notify");

						Node item = null;
						item = doc.createElement("targetID");
						item.appendChild(doc.createTextNode(targetID));
						System.out.println();
						nList.item(0).appendChild(item);

						representation = xmlToStr(doc);

					} catch (SAXException e) {
						// handle SAXException
					} catch (IOException e) {
						// handle IOException
					}
				} catch (ParserConfigurationException e1) {
					// handle ParserConfigurationException
				}
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(representation);
				sb.append("<targetID>" + targetID + "</targetID>");
				representation = sb.toString();
			}
		} else {
			representation = "<targetID>" + targetID + "</targetID>";
		}

		/***************************************************************************
		 * send message
		 **************************************************************************/
		try {
			URI new_uri = new URI(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort());
			System.out.println("new_uri = " + new_uri.toString());

			// 檢查連線
			Activator.connectToWebsocketServer(new_uri);

			// 送出訊息
			Activator.sendStr(new_uri, representation);

			System.out.println("Message send");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		if (representation.replaceAll("<targetID>.*</targetID>", "").length() <= 0) {
			responseConfirm.setStatusCode(StatusCode.STATUS_OK);
		} else {
			responseConfirm.setStatusCode(StatusCode.STATUS_CREATED);
		}

		LOGGER.info("Websocket Client > " + responseConfirm);
		return responseConfirm;
	}

	public static String xmlToStr(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}
}
