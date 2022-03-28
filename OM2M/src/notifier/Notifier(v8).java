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
 *     Christophe Chassot - Management and initial specification.
 *     Khalil Drira - Management and initial specification.
 *     Yassine Banouar - Initial specification, conception, implementation, test
 *         and documentation.
 ******************************************************************************/
package org.eclipse.om2m.core.notifier;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.commons.resource.ContentInstance;
import org.eclipse.om2m.commons.resource.Notify;
import org.eclipse.om2m.commons.resource.Resource;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.resource.Subscription;
import org.eclipse.om2m.commons.resource.Subscriptions;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.comm.RestClient;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.dao.DAOFactory;
import org.eclipse.om2m.core.dao.DBAccess;
import org.eclipse.om2m.core.redirector.Redirector;
import org.eclipse.om2m.core.router.Router;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;

//2021.5.11 modified
//download from https://mvnrepository.com/artifact/commons-httpclient/commons-httpclient/3.1
//download from 
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;


/**
 * Notifies subscribers when a change occurs on a resource according to their
 * subscriptions.
 * 
 * @author <ul>
 *         <li>Mahdi Ben Alaya < ben.alaya@laas.fr > < benalaya.mahdi@gmail.com
 *         ></li>
 *         <li>Yassine Banouar < ybanouar@laas.fr > < yassine.banouar@gmail.fr >
 *         </li>
 *         <li>Marouane El kiasse < melkiasse@laas.fr > < kiasmarouane@gmail.com
 *         ></li>
 *         </ul>
 */
public class Notifier {
	/** Logger */
	private static Log LOGGER = LogFactory.getLog(Notifier.class);
	private static Instrumentation instrumentation;
	/**
	 * Finds all resource subscribers and notifies them.
	 * 
	 * @param statusCode
	 *            - Notification status code
	 * @param resource
	 *            - Notification resource
	 */
	
	//2021.5.25 change Wanem loss rate
	static Random rand=new Random();
	static int upperbound=50;	
	public static int lossrate=0;
	
	//2021.5.11 modified
	public static String GSCL_IP = "192.168.101.133";
	public static String GSCL_Port = "8282";
	public static String DeviceID = "D2";
	
	private static String return_protocol="mqtt";
//	private static String foo()  {
//		String test="gscl/applications/" + DeviceID + "_Temp";
//		test = test + "/containers/DATA/contentInstances/latest/";
//		String urltest = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/" + test;
//		HttpClient httpclientTest = new HttpClient();
//		GetMethod httpMethodTest = new GetMethod(urltest);
//		httpMethodTest.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");
//		String strBody="";
//		int statusCode=0;
//		try {
//			statusCode = httpclientTest.executeMethod(httpMethodTest);
//		} catch (HttpException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		try {
//			strBody = httpMethodTest.getResponseBodyAsString();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String[] tmp=strBody.split("<om2m:contentSize>");
//		String output=tmp[1].split("</om2m:contentSize>")[0];
//		//System.out.println(statusCode);
//		//System.out.println("sizeeeeeeeeee-------------------"+output+"--------------------");
//		return output;
//	}
	public static void notify(StatusCode statusCode, Resource resource) {

		// Get the subscriptions uri
		String subscriptionsUri = resource.getUri().substring(0, resource.getUri().lastIndexOf("/")) + Refs.SUBSCRIPTIONS_REF;
		// Get the subscriptions collection from data base
		EntityManager em = DBAccess.createEntityManager();
		em.getTransaction().begin();
		Subscriptions subscriptions = DAOFactory.getSubscriptionsDAO().find(subscriptionsUri, em);

		if (subscriptions != null) {
			ArrayList<Subscription> subscriptionList = new ArrayList<Subscription>();
			// Finds each subscription resource and add it to the
			// subscriptionList.
			for (int i = 0; i < subscriptions.getSubscriptionCollection().getNamedReference().size(); i++) {
				subscriptionList.add(DAOFactory.getSubscriptionDAO().find(subscriptions.getSubscriptionCollection().getNamedReference().get(i).getValue(), em));
			}

			Notify notify;
			Subscription subscription;
			// Create Notify object and sends it to subscribers
			for (int i = 0; i < subscriptionList.size(); i++) {
				notify = new Notify();
				subscription = new Subscription();
				subscription = subscriptionList.get(i);
				notify.setStatusCode(statusCode);
				notify.getRepresentation().setContentType("application/xml");
				// Check the FilterCriteria
				if (subscription.getFilterCriteria() != null) {
					// Check the FilterCriteria "IfMach" attribute"
					if (!subscription.getFilterCriteria().getIfMatch().isEmpty() && subscription.getFilterCriteria().getIfMatch().get(0) != null) {
						// In case of a ContentInstance resource, If "IfMatch"
						// is equals to "content" then notify with the content
						// instead of sending the full resource representation.
						if ("contentInstance".equalsIgnoreCase(resource.getClass().getSimpleName()) && "content".equalsIgnoreCase(subscription.getFilterCriteria().getIfMatch().get(0))) {
							ContentInstance contentInstance = (ContentInstance) resource;
							notify.getRepresentation().setValue(contentInstance.getContent().getValue());
						}
						// Possibility to add other criteria
					}
				} else {
					// Notify if no "FilterCriteria" specified.
					notify.getRepresentation().setValue(resource);
				}
				notify.setSubscriptionReference(subscription.getUri());

				final String contact = subscription.getContact();

				// Create a RequestIndication with the notify as representation
				final RequestIndication requestIndication = new RequestIndication();
				requestIndication.setMethod(Constants.METHOD_CREATE);
				requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
				requestIndication.setRepresentation(notify);
				

				// Send notification on a new Thread
				new Thread() {
					public void run() {
						LOGGER.info("Notification Request:\n" + requestIndication);
						ResponseConfirm responseConfirm = Notifier.notify(requestIndication, contact);
						LOGGER.info("Notification Response:\n" + responseConfirm);
					}
				}.start();
			}
		}
		em.close();
	}
	
	public static void marktime(RequestIndication requestIndication) {
		String[] tmp0=requestIndication.getRepresentation().split("application/xml\">");
		String a=tmp0[1].split("</om2m:representation>")[0];
		byte[] decoded = Base64.decodeBase64(a);
		String b="";
		try {
			b = new String(decoded, "UTF-8");
			//System.out.println(b);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String[] tmp=b.split("<om2m:content xmime:contentType=\"application/xml\">");
		String output=tmp[1].split("</om2m:content>\n")[0];
		byte[] decoded2 = Base64.decodeBase64(output);
		String c="";
		try {
			c=new String(decoded2, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(c);
		String[] tmp2=c.split("<str name='timestamp' val='");
		//set time into it
		String newcontent=tmp2[0]+"<str name='timestamp' val='"+System.currentTimeMillis()+"'/></obj>";
		
		//adding time then encode back
		//System.out.println(newcontent);
		byte[] encode = newcontent.getBytes(Charset.forName("UTF-8"));
		String enc=Base64.encodeBase64String(encode);
		//System.out.println(enc);
		String enc2=tmp[0]+"<om2m:content xmime:contentType=\"application/xml\">"+enc+"</om2m:content>\n" + 
				"</om2m:contentInstance>";
		byte[] encode2 = enc2.getBytes(Charset.forName("UTF-8"));
		String xyz=Base64.encodeBase64String(encode2);
		System.out.println(xyz);
		String input=tmp0[0]+"application/xml\">"+xyz+"</om2m:representation>"+tmp0[1].split("</om2m:representation>")[1];
		requestIndication.setRepresentation(input);
	}

	public static void handle_data(RequestIndication requestIndication,int datasize) throws HttpException, IOException {
		//2021.9.14 mod
		String[] tmp0=requestIndication.getRepresentation().split("application/xml\">");
		String a=tmp0[1].split("</om2m:representation>")[0];
		byte[] decoded = Base64.decodeBase64(a);
		String b="";
		try {
			b = new String(decoded, "UTF-8");
			//System.out.println(b);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String[] tmp=b.split("<om2m:content xmime:contentType=\"application/xml\">");
		String output=tmp[1].split("</om2m:content>\n")[0];
		byte[] decoded2 = Base64.decodeBase64(output);
		String c="";
		try {
			c=new String(decoded2, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//2021.9.26 mod
		String[] tmp2=c.split("<str name='index' val='");
		int idx=Integer.parseInt(tmp2[1].split("'/><str name='timestamp'")[0]);
		
		String url = "http://140.116.247.69:9000/get_data_size_return_action";
		HttpClient httpclient = new HttpClient();
		PostMethod httpMethod = new PostMethod(url);
		StringBuilder sb = new StringBuilder();
		sb.append(datasize);
		//2021.9.14 mod
		sb.append("//");
		sb.append(idx);
		StringRequestEntity requestEntity = new StringRequestEntity(sb.toString(),"application/xml", "UTF-8");
		httpMethod.setRequestEntity(requestEntity);
		int statuscode = httpclient.executeMethod(httpMethod);
		return_protocol=new String(httpMethod.getResponseBody());
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println(return_protocol);
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
	}
	public static ResponseConfirm notify(RequestIndication requestIndication, String contact) {
		
		// // 將要通知的訊息寫到文字檔裡
		// try {
		// FileWriter fw = new FileWriter("test_notify_msg.txt", true); //
		// True則表示用附加的方式寫到檔案原有內容之後
		// BufferedWriter bw = new BufferedWriter(fw); //
		// 將BufferedWeiter與FileWrite物件做連結
		// bw.write(requestIndication.getRepresentation());
		// bw.flush();
		// bw.close();
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// }

		// ex: contact = nscl/applications/NA/monitor
		
		//String strSelectedProtocol = System.getProperty("org.eclipse.om2m.protocol.selection", "none");
		//String strSelectedProtocol="";
		
		
//		String test="gscl/applications/" + DeviceID + "_Temp";
//		test = test + "/containers/DATA/contentInstances/latest/";
//		String urltest = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/" + test;
//		HttpClient httpclientTest = new HttpClient();
//		GetMethod httpMethodTest = new GetMethod(urltest);
//		httpMethodTest.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");
//		String strBody="";
//		int statusCode=0;
//		try {
//			statusCode = httpclientTest.executeMethod(httpMethodTest);
//		} catch (HttpException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		try {
//			strBody = httpMethodTest.getResponseBodyAsString();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		String[] tmp=strBody.split("<om2m:contentSize>");
//		String output=tmp[1].split("</om2m:contentSize>")[0];
//		//System.out.println(statusCode);
//		System.out.println(output);
		
		
		//default protocol set to mqtt
		//String protocol="mqtt";
		//String body=foo();
		//int x=Integer.parseInt(body);
		//ChangeWanem loss rate
		
		String representation = requestIndication.getRepresentation();
//		String[] tmp=representation.split("<om2m:contentSize>");
//		String output=tmp[1].split("</om2m:contentSize>")[0];
		
		// 8.26 modify no need state now
		// 8/29 send data size to agent and get action from it
//		try {
//			protocol=handle_data(representation.length());
//		} catch (HttpException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		
		try {
			handle_data(requestIndication,representation.length());
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//int status_code=ChangeWanem.start(lossrate);
//		int status_code=200;
//		int x=400;
		//2021.9.26
		marktime(requestIndication);
		if(return_protocol.equals("coap")) {
			contact = String.format("%s://%s:%s/om2m/%s", "coap", Constants.NSCL_IP, Constants.NSCL_COAP_PORT, contact);
			//SetProtocoltoDelay(requestIndication,"coap");
			//protocol="coap";
		}
		if(return_protocol.equals("mqtt")) {
			contact = String.format("%s://%s:%s/om2m/%s", "mqtt", Constants.NSCL_IP, "1883", contact);
			//SetProtocoltoDelay(requestIndication,"mqtt");
			//protocol="mqtt";
		}
		if(return_protocol.equals("ws")) {
			contact = String.format("%s://%s:%s/om2m/%s", "ws", Constants.NSCL_IP, "2014", contact);
			//SetProtocoltoDelay(requestIndication,"ws");
			//protocol="websocket";
		}
		if(return_protocol.equals("xmpp")) {
			contact = String.format("%s://%s:%s/om2m/%s", "xmpp", Constants.NSCL_IP, "5222", contact);
			//SetProtocoltoDelay(requestIndication,"xmpp");
			//protocol="xmpp";
		}
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
////		System.out.println(requestIndication.getRepresentation());
//		System.out.println(contact);
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");
//		System.out.println("----*************************----------");

//		//create loss rate cin
//		try {
//			String url3 = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/NetworkStatus_Temp/containers/PacketLossRate/contentInstances";
//			HttpClient httpclient3 = new HttpClient();
//			PostMethod httpMethod3 = new PostMethod(url3);
//			httpMethod3.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");
//			StringBuilder sb3 = new StringBuilder();
//
//			sb3.append("<obj>");
//			sb3.append("<str name='Change Status' val='" + status_code + "'/>");
//			sb3.append("<str name='Loss Rate' val='" + lossrate + "%'/>");
//			sb3.append("</obj>");
//
//			StringRequestEntity requestEntity3 = new StringRequestEntity(sb3.toString(), "application/xml", "UTF-8");
//			httpMethod3.setRequestEntity(requestEntity3);
//			int status3 = httpclient3.executeMethod(httpMethod3);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
		
		
//		//create protocol cin
//		try {
//			String url2 = "http://" + GSCL_IP + ":" + GSCL_Port + "/om2m/gscl/applications/Protocol_Temp/containers/ProtocolSelect/contentInstances";
//			HttpClient httpclient2 = new HttpClient();
//			PostMethod httpMethod2 = new PostMethod(url2);
//			httpMethod2.addRequestHeader("Authorization", "Basic YWRtaW46YWRtaW4");
//			StringBuilder sb2 = new StringBuilder();
//
//			sb2.append("<obj>");
//			sb2.append("<str name='Protocol' val='" + "sssssss" + "'/>");
//			sb2.append("</obj>");
//
//			StringRequestEntity requestEntity2 = new StringRequestEntity(sb2.toString(), "application/xml", "UTF-8");
//			httpMethod2.setRequestEntity(requestEntity2);
//			int status2 = httpclient2.executeMethod(httpMethod2);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		
		
//		if (!strSelectedProtocol.equals("none")) {
//
//			if (strSelectedProtocol.equals("mqtt")) {
//				contact = String.format("%s://%s:%s/om2m/%s", "mqtt", Constants.NSCL_IP, "1883", contact);
//			} else if (strSelectedProtocol.equals("coap")) {
//				contact = String.format("%s://%s:%s/om2m/%s", "coap", Constants.NSCL_IP, Constants.NSCL_COAP_PORT, contact);
//			} else if (strSelectedProtocol.equals("xmpp")) {
//				contact = String.format("%s://%s:%s/om2m/%s", "xmpp", Constants.NSCL_IP, "5222", contact);
//			} else if (strSelectedProtocol.equals("websocket")) {
//				contact = String.format("%s://%s:%s/om2m/%s", "ws", Constants.NSCL_IP, "2014", contact);
//			} else {
//				// contact = String.format("%s://%s:%s/om2m/%s", "mqtt",
//				// Constants.NSCL_IP, "1883", contact);
//				contact = selectProtocol(, contact);
//			}
//		}

		// Check whether the subscription contact is protocol-dependent or not.
		if (contact.matches(".*://.*")) {
			// Contact = protocol-dependent -> direct notification using the
			// rest client.
			requestIndication.setBase(contact);
			requestIndication.setTargetID("");
			return new RestClient().sendRequest(requestIndication);
		} else {
			// Contact = protocol-independent -> Check whether the targeted SCL
			// is local or remote.
			String sclId = contact.split("/")[0];
			requestIndication.setTargetID(contact);
			if (Constants.SCL_ID.equals(sclId)) {
				// scl = local -> perform request on the local scl.
				return new Router().doRequest(requestIndication);
			} else {
				// scl = remote -> retarget request to the remote scl.
				return new Redirector().retarget(requestIndication);
			}
		}
	}

	
	//not used 2021.8.29
//	public static String selectProtocol(RequestIndication requestIndication, String contact) {
//		// 取得開始時間
//		long timeForSelectProtocolStart = System.currentTimeMillis();
//
//		String new_contact = contact;
//		String sclId = contact.split("/")[0];
//
//		// gscl 送資料給 nscl
//		if (Constants.SCL_ID.equalsIgnoreCase("gscl") && sclId.equalsIgnoreCase("nscl")) {
//
//			// 取得gscl與nscl之間的average packet loss rate
//			Integer iAveragePacketLossRate = getAveragePacketLossRate();
//			if (iAveragePacketLossRate != null) {
//
//				// 決定要使用哪一個協定
//				String selectedProtocol = decideProtocol(iAveragePacketLossRate, requestIndication.getRepresentation());
//				System.out.println("selectedProtocol = " + selectedProtocol);
//
//				if (selectedProtocol.equalsIgnoreCase("mqtt")) {
//					//new_contact = String.format("%s://%s:%s/om2m/%s", selectedProtocol, Constants.NSCL_IP, "1883", contact);
//					new_contact = String.format("%s://%s:%s/om2m/%s", selectedProtocol, Constants.NSCL_IP, "1884", contact);
//				} else if (selectedProtocol.equalsIgnoreCase("coap")) {
//					new_contact = String.format("%s://%s:%s/om2m/%s", selectedProtocol, Constants.NSCL_IP, Constants.NSCL_COAP_PORT, contact);
//				}
//
//				System.out.println("new_contact (gscl -> nscl) = " + new_contact);
//				System.out.println("new_contact (gscl -> nscl) = " + new_contact);
//				System.out.println("new_contact (gscl -> nscl) = " + new_contact);
//				System.out.println("new_contact (gscl -> nscl) = " + new_contact);
//				System.out.println("new_contact (gscl -> nscl) = " + new_contact);
//
//				// 取得結束時間
//				long timeForSelectProtocolEnd = System.currentTimeMillis();
//
//				// 將時間相減，算出選擇協定總共花了多少時間
//				long timeForSelectProtocolResult = timeForSelectProtocolEnd - timeForSelectProtocolStart;
//				System.out.println("timeForSelectProtocolResult = " + String.valueOf(timeForSelectProtocolResult));
//
//				// 將gscl與nscl之間的最近五次的平均 packet loss rate寫到文字檔裡
//				try {
//					FileWriter fw = new FileWriter("test_averagePacketLossRate.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
//					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
//					bw.write(iAveragePacketLossRate.toString());
//					bw.write("\r\n");
//					bw.flush();
//					bw.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//				// 將選擇的Protocol寫到文字檔裡
//				try {
//					FileWriter fw = new FileWriter("test_protocol.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
//					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
//					bw.write(selectedProtocol.toString());
//					bw.write("\r\n");
//					bw.flush();
//					bw.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//
//				// 將選擇協定所花費的時間寫到文字檔裡
//				try {
//					FileWriter fw = new FileWriter("test_selectionCost.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
//					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
//					bw.write(String.valueOf(timeForSelectProtocolResult));
//					bw.write("\r\n");
//					bw.flush();
//					bw.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		return new_contact;
//	}
	

	/**
	 * 在gscl上取得gscl與 nscl之間的average packet loss rate
	 */
	private static Integer getAveragePacketLossRate() {

		Router scl = new Router();

		// Build a RequestIndication
		RequestIndication request = new RequestIndication();
		request.setMethod(Constants.METHOD_RETREIVE);
		request.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		String strTargetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/NetworkStatus" + Refs.CONTAINERS_REF + "/PacketLossRate" + Refs.CONTENTINSTANCES_REF;
		// gscl/applications/NetworkStatus/containers/PacketLossRate/contentInstances

		/***************************************************************************
		 * 取出 content
		 **************************************************************************/
		ArrayList<String> listContent64 = new ArrayList<String>();

		try {
			// send the RequestIndication
			request.setTargetID(strTargetId);
			String strBody = scl.doRequest(request).getRepresentation();

			// regex
			String strContentInstanceId = "";
			// String patternStr = "<om2m:content>(.+)</om2m:content>";
			String patternStr = "<om2m:content.*>(.+)</om2m:content>";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(strBody);
			while (matcher.find()) {
				strContentInstanceId = matcher.group(1);
				listContent64.add(strContentInstanceId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (listContent64.size() <= 0) {
			out("listContent64.size = " + listContent64.size());
			return null;
		}

		/***************************************************************************
		 * base64解碼並取出packet loss rate的值
		 **************************************************************************/
		ArrayList<Integer> listPacketLossRate = new ArrayList<Integer>();
		for (String strContent64 : listContent64) {
			String content = new String(DatatypeConverter.parseBase64Binary(strContent64));
			System.out.println("Content:\n" + content + "\n");
			String patternStr = "<str val=[\"'](\\d+)[\"'] name=[\"']packetlossrate.*[\"']/>";
			Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(content.toLowerCase());
			if (matcher.find()) {
				String strPacketLossRate = matcher.group(1);
				listPacketLossRate.add(Integer.parseInt(strPacketLossRate));
			}
		}
		System.out.println("listPacketLossRateSize = " + listPacketLossRate.size());
		System.out.println("listPacketLossRate = " + listPacketLossRate);

		/***************************************************************************
		 * 將packet loss rate做平均
		 **************************************************************************/
		if (listPacketLossRate.size() > 0) {
			int sum = 0;
			for (int i = 0; i < listPacketLossRate.size(); i++) {
				sum += listPacketLossRate.get(i);
			}
			int iAveragePacketLossRate = (int) Math.round((double) sum / listPacketLossRate.size());
			System.out.println("AveragePacketLossRate = " + iAveragePacketLossRate);
			return iAveragePacketLossRate;
		}

		return null;
	}

	private static String decideProtocol(int iPacketLossRate, String strData) {
		int dataBytes = strData.getBytes().length;

		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);
		System.out.println("dataBytes = " + dataBytes);

		if (dataBytes < 1024) {
			if (iPacketLossRate >= 23) {
				return "coap";
			} else {
				return "mqtt";
			}
		} else {
			return "mqtt";
		}
	}

	public static void out(String str) {
		for (int i = 1; i <= 7; i++) {
			System.out.println(str);
		}
	}
}