package org.eclipse.om2m.ipu.websocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.om2m.core.service.SclService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;

@ServerEndpoint(value = "/")
public class WordgameServerEndpoint {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	/** Discovered SCL service */
	static SclService SCL;

	public static SclService getSCL() {
		return SCL;
	}

	public static void setSCL(SclService sCL) {
		SCL = sCL;
	}

	@OnOpen
	public void onOpen(Session session) {
		logger.info("Connected ... " + session.getId());
	}

	@OnMessage
	public void onMessage(String message, Session mSession) {
		System.out.println("Message: " + message);
		// System.out.println("session: " + mSession);

		if (mSession != null) {

			String targetId = parseTargetID(message);
			String body = message;

			try {
				if (isNotify(body)) {

					// 在nscl接收到gscl用websocket送來的notification。
					RequestIndication requestIndication = new RequestIndication();
					requestIndication.setMethod(Constants.METHOD_CREATE);
					requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
					requestIndication.setRepresentation(body);

					requestIndication.setBase("");
					requestIndication.setTargetID(targetId);
					System.out.println("targetId = " + targetId);
					System.out.println(getSCL().doRequest(requestIndication));
				} else {

					// 設定 targetId
					RequestIndication requestIndication = new RequestIndication();

					// Get the request parameters
					String queryString = getUriQueryString(targetId);

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
						targetId = targetId.substring(0, targetId.indexOf('?'));
					}
					System.out.println("targetId = " + targetId);

					if (body.replaceAll("<targetID>.*</targetID>", "").length() == 0) {
						requestIndication.setMethod(Constants.METHOD_EXECUTE);
						requestIndication.setTargetID(targetId);
					} else {
						requestIndication.setMethod(Constants.METHOD_CREATE);
						requestIndication.setTargetID(targetId);
					}

					requestIndication.setRepresentation(body);
					ResponseConfirm resp = getSCL().doRequest(requestIndication);
					System.out.println(resp);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
	}

	@OnError
	public void onError(Throwable t) {
		System.out.println("Error: " + t.getMessage());
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

	private static String parseTargetID(String body) {
		
		// regex targetID
		String patternStr = "<targetID>(.*)</targetID>";
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(body);
		if (matcher.find()) {
			String strTargetID = matcher.group(1);
			return strTargetID;
		}
//		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//		try {
//			DocumentBuilder dBuilder = dbf.newDocumentBuilder();
//			Document notifyDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(body.getBytes("utf-8"))));
//			String strTargetID = notifyDoc.getElementsByTagName("targetID").item(0).getTextContent();
//			System.out.println("strTargetID = " + strTargetID);
//			return strTargetID;
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		} catch (SAXException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return "";
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
}