package org.eclipse.om2m.ipu.mqtt;

import io.moquette.proto.messages.AbstractMessage.QOSType;
import io.moquette.proto.messages.PublishMessage;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.ErrorInfo;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.ipu.service.IpuService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class MqttProxyController implements IpuService {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(MqttProxyController.class);

	/** Returns the implemented Application Point of Contact id */
	public String getAPOCPath() {
		return MqttProxyMonitor.APOCPATH;
	}

	@Override
	public ResponseConfirm doExecute(RequestIndication requestIndication) {
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		System.out.println("doExecute in MqttProxyController ");
		
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
		
		// try {
		// String[] info = requestIndication.getTargetID().split("/");
		// String strMsg = info[info.length - 1];
		// String strTopic = info[info.length - 3];
		//
		// // publish data on topic
		// // MqttProxyMonitor.publish(strTopic, strMsg);
		//
		// PublishMessage msg = new PublishMessage();
		// msg.setQos(QOSType.LEAST_ONE);
		// msg.setTopicName(strTopic);
		// ByteBuffer sendBuffer = ByteBuffer.wrap(strMsg.getBytes());
		// msg.setPayload(sendBuffer);
		// MqttBroker.getServer().internalPublish(msg);
		//
		// return new ResponseConfirm(StatusCode.STATUS_OK);
		//
		// } catch (Exception e) {
		// LOGGER.error("MQTT_Proxy Error", e);
		// return new ResponseConfirm(new
		// ErrorInfo(StatusCode.STATUS_BAD_REQUEST, "MQTT_Proxy Error"));
		// }
	}

	@Override
	public ResponseConfirm doCreate(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
		
		// try {
		// String strTopic = "", strData = "", patternStr;
		// String content = requestIndication.getRepresentation();
		// String[] info = requestIndication.getTargetID().split("/");
		// String mqttMethod = info[info.length - 1];
		// System.out.println("mqttMethod = " + mqttMethod);
		//
		// // parse representation
		// if (mqttMethod.toLowerCase().equals("publish")) {
		//
		// } else if (mqttMethod.toLowerCase().equals("notify")) {
		// DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// DocumentBuilder dBuilder = dbf.newDocumentBuilder();
		//
		// Document notifyDoc = dBuilder.parse(new InputSource(new
		// ByteArrayInputStream(content.getBytes("utf-8"))));
		// String contentInstance64 =
		// notifyDoc.getElementsByTagName("om2m:representation").item(0).getTextContent();
		// System.out.println("ContentInstance (Base64-encoded):\n" +
		// contentInstance64 + "\n");
		// String contentInstance = new
		// String(DatatypeConverter.parseBase64Binary(contentInstance64));
		// System.out.println("ContentInstance:\n" + contentInstance + "\n");
		//
		// Document instanceDoc = dBuilder.parse(new InputSource(new
		// ByteArrayInputStream(contentInstance.getBytes("utf-8"))));
		// String content64 =
		// instanceDoc.getElementsByTagName("om2m:content").item(0).getTextContent();
		// System.out.println("Content (Base64-encoded):\n" + content64 + "\n");
		//
		// content = new String(DatatypeConverter.parseBase64Binary(content64));
		// }
		//
		// System.out.println("Content:\n" + content + "\n");
		//
		// // get topic and data.
		// Pattern pattern;
		// Matcher matcher;
		//
		// patternStr = "<str name=\"[Tt][Oo][Pp][Ii][Cc]\" val=\"(.+)\"/>";
		// pattern = Pattern.compile(patternStr);
		// matcher = pattern.matcher(content);
		// if (matcher.find()) {
		// strTopic = matcher.group(1);
		// }
		//
		// patternStr = "<str name=\"[Dd][Aa][Tt][Aa]\" val=\"(.+)\"/>";
		// pattern = Pattern.compile(patternStr);
		// matcher = pattern.matcher(content);
		// if (matcher.find()) {
		// strData = matcher.group(1);
		// }
		//
		// // publish data on topic
		// MqttProxyMonitor.publish(strTopic, strData);
		//
		// return new ResponseConfirm(StatusCode.STATUS_OK);
		//
		// } catch (Exception e) {
		// LOGGER.error("IPU MQTT_Proxy Error", e);
		// return new ResponseConfirm(new
		// ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED,
		// "IPU MQTT_Proxy Error"));
		// }
	}

	@Override
	public ResponseConfirm doUpdate(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	@Override
	public ResponseConfirm doDelete(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

	@Override
	public ResponseConfirm doRetrieve(RequestIndication requestIndication) {
		return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_IMPLEMENTED, requestIndication.getMethod() + " Method not Implemented"));
	}

}