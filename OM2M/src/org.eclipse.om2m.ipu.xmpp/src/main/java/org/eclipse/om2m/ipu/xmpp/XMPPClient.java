package org.eclipse.om2m.ipu.xmpp;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.jivesoftware.smack.packet.Message;

public class XMPPClient implements RestClientService {

	/** Logger: */
	private static Log LOGGER = LogFactory.getLog(XMPPClient.class);

	/** implemented specific protocol name */
	private static String protocol = "xmpp";

	public static void main(String[] args) {

	}

	@Override
	public ResponseConfirm sendRequest(RequestIndication requestIndication) {
		LOGGER.info("XMPP Client > " + requestIndication);
		// create the standard final response
		ResponseConfirm responseConfirm = new ResponseConfirm();

		// get the Payload from requestIndication
		String representation = requestIndication.getRepresentation();

		// get the authorization (uri_query option) from requestIndication
		String authorization = requestIndication.getRequestingEntity();

		// get the method from requestIndication
		String method = requestIndication.getMethod();

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

		System.out.println("targetID = " + targetID);
		System.out.println("targetID = " + targetID);
		System.out.println("targetID = " + targetID);

		// 傳送訊息前先檢查連線是否正常
		Activator.checkXMPPConn(uri);
		
		Message msg = new Message();
		msg.setType(Message.Type.chat);

		if (Constants.SCL_ID.equalsIgnoreCase("nscl")) {
			msg.setTo("om2m_gscl" + "@" + OM2MListener.getConn().getServiceName());
		} else if (Constants.SCL_ID.equalsIgnoreCase("gscl")) {
			msg.setTo("om2m_nscl" + "@" + OM2MListener.getConn().getServiceName());
		}

		msg.setFrom(OM2MListener.getConn().getUser());
		msg.setSubject(targetID);
		msg.setBody(representation);

		System.out.println("msg = " + msg.getBody());
		out("send message using xmpp");
		
		// 傳送訊息
		OM2MListener.getConn().sendPacket(msg);

		if (representation.length() <= 0) {
			responseConfirm.setStatusCode(StatusCode.STATUS_OK);
		} else {
			responseConfirm.setStatusCode(StatusCode.STATUS_CREATED);
		}
		LOGGER.info("XMPP Client > " + responseConfirm);

		return responseConfirm;
	}

	@Override
	public String getProtocol() {
		return protocol;
	}

	public static void out(String msg) {
		for (int i = 1; i <= 10; i++) {
			System.out.println(msg);
		}
	}
}
