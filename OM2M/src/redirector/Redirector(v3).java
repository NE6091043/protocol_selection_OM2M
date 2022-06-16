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
package org.eclipse.om2m.core.redirector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.xml.bind.DatatypeConverter;

import org.eclipse.om2m.commons.resource.ErrorInfo;
import org.eclipse.om2m.commons.resource.ReferenceToNamedResource;
import org.eclipse.om2m.commons.resource.Refs;
import org.eclipse.om2m.commons.resource.Scl;
import org.eclipse.om2m.commons.resource.Scls;
import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.comm.RestClient;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.dao.DAOFactory;
import org.eclipse.om2m.core.dao.DBAccess;
import org.eclipse.om2m.core.router.Patterns;
import org.eclipse.om2m.core.router.Router;

/**
 * Re-target the REST request to the Distant SCL registered in the {@link Scls}
 * Collection.
 * 
 * @author <ul>
 *         <li>Yassine Banouar < ybanouar@laas.fr > < yassine.banouar@gmail.com
 *         ></li>
 *         <li>Mahdi Ben Alaya < ben.alaya@laas.fr > < benalaya.mahdi@gmail.com
 *         ></li>
 *         </ul>
 */
public class Redirector {

	// TODO add by junhao - start
	// private static String strSelectedProtocol = System.getProperty("org.eclipse.om2m.protocol.selection", "none");

	// TODO add by junhao - end

	/**
	 * Re-targets a request to a Distant SCL registered in the sclCollection.
	 * 
	 * @param requestIndication
	 *            - The generic request to handle.
	 * @return The generic returned response.
	 */
	protected static String return_protocol="ws";
	
	private static int cnt=0;
	
	protected static int datasize=100;
	
	private static String getgoogletime() {
		String cmd="sh /home/user/time.sh";
		Runtime run = Runtime.getRuntime();
		Process pr=null;
		try {
			pr = run.exec(cmd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			pr.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = "";
		try {
			line+=buf.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
//		System.out.println(line);
	}
	
	public static void marktime(RequestIndication requestIndication) {
		String[] tmp0=requestIndication.getRepresentation().split("<str name='timestamp' val='");
		String google_time=getgoogletime();
		String newcontent=tmp0[0]+"<str name='timestamp' val='"+google_time+"'/></obj>";
		requestIndication.setRepresentation(newcontent);
	}
	
	public static void handle_data(RequestIndication requestIndication,int datasize) throws HttpException, IOException {
		String[] tmp0=requestIndication.getRepresentation().split("str name='index' val='");
		int idx=Integer.parseInt(tmp0[1].split("'/><str name='timestamp'")[0]);
		
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
	
	public ResponseConfirm retarget(RequestIndication requestIndication) {
		// Get scls collection from db
		EntityManager em = DBAccess.createEntityManager();
		em.getTransaction().begin();
		Scls scls = DAOFactory.getSclsDAO().find(Constants.SCL_ID + Refs.SCLS_REF, em);

		boolean found = false;
		String sclId = requestIndication.getTargetID().split("/")[0];
		List<ReferenceToNamedResource> sclCollection = scls.getSclCollection().getNamedReference();
		for (int i = 0; i < sclCollection.size(); i++) {
			if (sclCollection.get(i).getId().equalsIgnoreCase(sclId)) {
				found = true;
				break;
			}
		}
		String sclURI = null;
		if (found) {
			sclURI = Constants.SCL_ID + Refs.SCLS_REF + "/" + sclId;
		} else {
			if (!Constants.SCL_TYPE.equalsIgnoreCase("NSCL") && sclCollection.size() > 0) {
				sclURI = Constants.SCL_ID + Refs.SCLS_REF + "/" + sclCollection.get(0).getId();
			} else {
				em.close();
				return new ResponseConfirm(new ErrorInfo(StatusCode.STATUS_NOT_FOUND, "Scl " + sclId + " does not exist in scls"));
			}
		}
		// Found remote scl
		Scl scl = DAOFactory.getSclDAO().find(sclURI, em);
		em.close();
		String base = scl.getPocs().getReference().get(0) + "/";
		requestIndication.setBase(base);

		// TODO add by junhao - start
		// ex: base = http://127.0.0.1:8181/om2m/
		out("base = " + base + " in Router.java");
		requestIndication = test(requestIndication);
		// TODO add by junhao - end

		// Retarget the request
		return new RestClient().sendRequest(requestIndication);
	}

	private RequestIndication test(RequestIndication requestIndication) {
		// Determine the appropriate resource controller
		String strTargetID = requestIndication.getTargetID();
		int idx = strTargetID.indexOf('/');
		if (idx > 0) {
			strTargetID = Constants.SCL_ID + "/" + strTargetID.substring(strTargetID.indexOf('/') + 1);
		}

		String strController = getResourceController(strTargetID, requestIndication.getMethod(), requestIndication.getRepresentation());
		// Select the resource controller method and invoke it.
		if (strController != null) {
			System.out.println("strController = " + strController);
			if (strController.equalsIgnoreCase("APocController")) {
				try {
					requestIndication = test2(requestIndication);
				} catch (Exception e) {
					System.out.println("Controller Internal Error");
				}
			}
		} else {
			System.out.println("controller == null, in Redirector.java");
		}
		return requestIndication;
	}

//	private RequestIndication test2(RequestIndication requestIndication) {
//		RequestIndication result = requestIndication;
//		String targetID = requestIndication.getTargetID();
//		
////  	2021.12.7
////		String strSelectedProtocol = System.getProperty("org.eclipse.om2m.protocol.selection", "none");
//		
//		
//		// ex: targetID = gscl/applications/LAMP_0/lamps/true
//		if (!strSelectedProtocol.equals("none")) { // nscl send command to gscl
//
//			String sclId = targetID.split("/")[0];
//
//			if (Constants.SCL_ID.equalsIgnoreCase("nscl") && sclId.equalsIgnoreCase("gscl")) {
//
//				// URI baseURI = getBaseURI(requestIndication);
//				URI baseURI = null;
//				try {
//					baseURI = new URI(requestIndication.getBase());
//				} catch (URISyntaxException e) {
//					e.printStackTrace();
//				}
//				// ex: baseURI = http://127.0.0.1:8181/om2m/
//
//				if (strSelectedProtocol.equals("mqtt")) {
//					targetID = String.format("%s://%s:%s/om2m/%s", "mqtt", baseURI.getHost(), "1884", targetID);
//				} else if (strSelectedProtocol.equals("coap")) {
//					targetID = String.format("%s://%s:%s/om2m/%s", "coap", baseURI.getHost(), "5684", targetID);
////					targetID = String.format("%s://%s:%s/om2m/%s", "coap", baseURI.getHost(), "5683", targetID);
////					targetID = String.format("%s://%s:%s/om2m/%s", "mqtt", baseURI.getHost(), "1884", targetID);
////					targetID = String.format("%s://%s:%s/om2m/%s", "coap+tcp", baseURI.getHost(), "5683", targetID);
//				} else if (strSelectedProtocol.equals("xmpp")) {
//					targetID = String.format("%s://%s:%s/om2m/%s", "xmpp", baseURI.getHost(), "5222", targetID);
//					//targetID = String.format("%s://%s:%s/om2m/%s", "xmpp", baseURI.getHost(), "5223", targetID);
//				} else if (strSelectedProtocol.equals("websocket")) {
//					targetID = String.format("%s://%s:%s/om2m/%s", "ws", baseURI.getHost(), "2015", targetID);
//				} else {
//					targetID = selectProtocol(requestIndication, targetID);
//				}
//
//				result.setBase(targetID);
//				result.setTargetID("");
//			}
//		}
//
//		return result;
//	}
	
	private RequestIndication test2(RequestIndication requestIndication) {
		RequestIndication result = requestIndication;
		String targetID = requestIndication.getTargetID();
		
//  	2021.12.7
//		String strSelectedProtocol = System.getProperty("org.eclipse.om2m.protocol.selection", "none");
		
		
		String sclId = targetID.split("/")[0];

		if (Constants.SCL_ID.equalsIgnoreCase("nscl") && sclId.equalsIgnoreCase("gscl")) {

			// URI baseURI = getBaseURI(requestIndication);
			URI baseURI = null;
			try {
				baseURI = new URI(requestIndication.getBase());
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			// ex: baseURI = http://127.0.0.1:8181/om2m/
//			Random random = new Random();   
//			int x = random.nextInt(100)+1;    
//			if(x<25) {
//				return_protocol="mqtt";
//			}
//			if(25<=x && x<50) {
//				return_protocol="coap";
//			}
//			if(50<=x && x<75) {
//				return_protocol="xmpp";
//			}
//			if(x>=75) {
//				return_protocol="ws";
//			}
			
			String representation = requestIndication.getRepresentation();
			datasize=representation.length();
			
//			try {
//				++cnt;
//				handle_data(requestIndication,representation.length());
//			} catch (HttpException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
//			
//			 if(cnt%100==0) {
//			 	ChangeWanem.dochange(1);
//			 }else {
//			 	if(cnt%100==25) {
//			 		ChangeWanem.dochange(2);
//			 	}
//			 	if(cnt%100==50) {
//			 		ChangeWanem.dochange(3);
//			 	}
//			 	if(cnt%100==75) {
//			 		ChangeWanem.dochange(4);
//			 	}
//			 }
			
//			marktime(requestIndication);
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
//			System.out.println(requestIndication.getRepresentation());
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
//			System.out.println("----*************************----------");
			
			if (return_protocol.equals("mqtt")) {
				targetID = String.format("%s://%s:%s/om2m/%s", "mqtt", baseURI.getHost(), "1884", targetID);
			}
			if (return_protocol.equals("coap")) {
				targetID = String.format("%s://%s:%s/om2m/%s", "coap", baseURI.getHost(), "5684", targetID);
//					targetID = String.format("%s://%s:%s/om2m/%s", "coap", baseURI.getHost(), "5683", targetID);
//					targetID = String.format("%s://%s:%s/om2m/%s", "mqtt", baseURI.getHost(), "1884", targetID);
//					targetID = String.format("%s://%s:%s/om2m/%s", "coap+tcp", baseURI.getHost(), "5683", targetID);
			} 
			if (return_protocol.equals("xmpp")) {
				targetID = String.format("%s://%s:%s/om2m/%s", "xmpp", baseURI.getHost(), "5222", targetID);
				//targetID = String.format("%s://%s:%s/om2m/%s", "xmpp", baseURI.getHost(), "5223", targetID);
			} 
			if (return_protocol.equals("ws")) {
				targetID = String.format("%s://%s:%s/om2m/%s", "ws", baseURI.getHost(), "2015", targetID);
			} 

			result.setBase(targetID);
			result.setTargetID("");
		}


		return result;
	}

	public static String selectProtocol(RequestIndication requestIndication, String contact) {
		// 取得開始時間
		long timeForSelectProtocolStart = System.currentTimeMillis();

		String new_contact = contact;
		String sclId = contact.split("/")[0];
		out("sclId = " + sclId);

		// nscl 送資料給 gscl
		if (Constants.SCL_ID.equalsIgnoreCase("nscl") && sclId.equalsIgnoreCase("gscl")) {

			// 取得nscl與gscl之間的average packet loss rate
			Integer iAveragePacketLossRate = getAveragePacketLossRate();
			if (iAveragePacketLossRate != null) {

				// URI baseURI = getBaseURI(requestIndication);
				URI baseURI = null;
				try {
					baseURI = new URI(requestIndication.getBase());
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				// ex: baseURI = http://127.0.0.1:8181/om2m/
				// 決定要使用哪一個協定
				String selectedProtocol = decideProtocol(iAveragePacketLossRate, requestIndication.getRepresentation());
				System.out.println("selectedProtocol = " + selectedProtocol);

				if (selectedProtocol.equalsIgnoreCase("mqtt")) {
					new_contact = String.format("%s://%s:%s/om2m/%s", "mqtt", baseURI.getHost(), "1883", contact);
				} else if (selectedProtocol.equalsIgnoreCase("coap")) {
					new_contact = String.format("%s://%s:%s/om2m/%s", "coap", baseURI.getHost(), "5683", contact);
//					new_contact = String.format("%s://%s:%s/om2m/%s", "mqtt", baseURI.getHost(), "1884", contact);
				} else if (selectedProtocol.equals("xmpp")) {
					new_contact = String.format("%s://%s:%s/om2m/%s", "xmpp", baseURI.getHost(), "5222", contact);
				} else if (selectedProtocol.equals("websocket")) {
					new_contact = String.format("%s://%s:%s/om2m/%s", "ws", baseURI.getHost(), "2015", contact);
				}

//				System.out.println("new_contact (nscl -> gscl) = " + new_contact);
//				System.out.println("new_contact (nscl -> gscl) = " + new_contact);
//				System.out.println("new_contact (nscl -> gscl) = " + new_contact);
//				System.out.println("new_contact (nscl -> gscl) = " + new_contact);
//				System.out.println("new_contact (nscl -> gscl) = " + new_contact);

				// 取得結束時間
				long timeForSelectProtocolEnd = System.currentTimeMillis();

				// 將時間相減，算出選擇協定總共花了多少時間
				long timeForSelectProtocolResult = timeForSelectProtocolEnd - timeForSelectProtocolStart;
				System.out.println("timeForSelectProtocolResult = " + String.valueOf(timeForSelectProtocolResult));

				// 將gscl與nscl之間的最近五次的平均 packet loss rate寫到文字檔裡
				try {
					FileWriter fw = new FileWriter("test_averagePacketLossRate.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
					bw.write(iAveragePacketLossRate.toString());
					bw.write("\r\n");
					bw.flush();
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// 將選擇的Protocol寫到文字檔裡
				try {
					FileWriter fw = new FileWriter("test_protocol.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
					bw.write(selectedProtocol.toString());
					bw.write("\r\n");
					bw.flush();
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// 將選擇協定所花費的時間寫到文字檔裡
				try {
					FileWriter fw = new FileWriter("test_selectionCost.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
					bw.write(String.valueOf(timeForSelectProtocolResult));
					bw.write("\r\n");
					bw.flush();
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return new_contact;
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

	//
	// private URI getBaseURI(RequestIndication requestIndication) {
	// // Get scls collection from db
	// EntityManager em = DBAccess.createEntityManager();
	// em.getTransaction().begin();
	// Scls scls = DAOFactory.getSclsDAO().find(Constants.SCL_ID +
	// Refs.SCLS_REF, em);
	//
	// boolean found = false;
	// String sclId = requestIndication.getTargetID().split("/")[0];
	// List<ReferenceToNamedResource> sclCollection =
	// scls.getSclCollection().getNamedReference();
	// for (int i = 0; i < sclCollection.size(); i++) {
	// if (sclCollection.get(i).getId().equalsIgnoreCase(sclId)) {
	// found = true;
	// break;
	// }
	// }
	// String sclURI = null;
	// if (found) {
	// sclURI = Constants.SCL_ID + Refs.SCLS_REF + "/" + sclId;
	// } else {
	// if (!Constants.SCL_TYPE.equalsIgnoreCase("NSCL") && sclCollection.size()
	// > 0) {
	// sclURI = Constants.SCL_ID + Refs.SCLS_REF + "/" +
	// sclCollection.get(0).getId();
	// } else {
	// em.close();
	// return null;
	// }
	// }
	//
	// // Found remote scl
	// Scl scl = DAOFactory.getSclDAO().find(sclURI, em);
	// em.close();
	// String base = scl.getPocs().getReference().get(0) + "/";
	// // ex: base = http://127.0.0.1:8181/om2m/
	// out("base = " + base + " in Router.java");
	// // requestIndication.setBase(base);
	// URI uri = null;
	// try {
	// uri = new URI(base);
	// } catch (URISyntaxException e) {
	// e.printStackTrace();
	// }
	// return uri;
	// }

	public static void out(String str) {
		for (int i = 1; i <= 7; i++) {
			System.out.println(str);
		}
	}

	public static String getResourceController(String uri, String method, String representation) {

		System.out.println("uri=" + uri);

		// Match the resource controller with an uri pattern and return it,
		// otherwise return null*
		if (Patterns.match(Patterns.SCL_BASE_PATTERN, uri)) {
			return "SclBaseController";
		}

		if (Patterns.match(Patterns.SCLS_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)) {
			return "SclsController";
		}
		if (Patterns.match(Patterns.SCL_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE) || (Patterns.match(Patterns.SCLS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE))) {
			return "SclController";
		}
		if (Patterns.match(Patterns.APPLICATIONS_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)) {
			return "ApplicationsController";
		}
		// In some cases it is required to know the resource name to detemine
		// the required resource controller.
		// This is the reason why resource representation is added as parameter
		// for some methods.
		if (Patterns.match(Patterns.APPLICATION_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.APPLICATIONS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && !representation.contains(":applicationAnnc"))) {
			return "ApplicationController";
		}
		if (Patterns.match(Patterns.APPLICATION_ANNC_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.APPLICATIONS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && representation.contains(":applicationAnnc"))) {
			return "ApplicationAnncController";
		}
		if (Patterns.match(Patterns.IPU_PATTERN, uri)) {
			// will forward to a RestClientController or IPUController;
			return "APocController";
		}
		if (Patterns.match(Patterns.CONTAINERS_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)) {
			return "ContainersController";
		}
		if (Patterns.match(Patterns.CONTAINER_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.CONTAINERS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && !representation.contains(":containerAnnc"))) {
			return "ContainerController";
		}
		if (Patterns.match(Patterns.CONTAINER_ANNC_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.CONTAINERS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && representation.contains(":containerAnnc"))) {
			return "ContainerAnncController";
		}
		if (Patterns.match(Patterns.LOCATION_CONTAINER_PATTERN, uri)) {
			return "LocationContainerController";
		}
		if (Patterns.match(Patterns.LOCATION_CONTAINER_ANNC_PATTERN, uri)) {
			return "LocationContainerAnncController";
		}
		if (Patterns.match(Patterns.CONTENT_INSTANCES_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)) {
			return "ContentInstancesController";
		}
		if (Patterns.match(Patterns.CONTENT_INSTANCE_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.CONTENT_INSTANCES_PATTERN, uri) && method.equals(Constants.METHOD_CREATE))) {
			return "ContentInstanceController";
		}
		if (Patterns.match(Patterns.CONTENT_PATTERN, uri)) {
			return "ContentController";
		}
		if (Patterns.match(Patterns.SUBSCRIPTIONS_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)) {
			return "SubscriptionsController";
		}
		if (Patterns.match(Patterns.SUBSCRIPTION_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.SUBSCRIPTIONS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE))) {
			return "SubscriptionController";
		}
		if (Patterns.match(Patterns.ACCESS_RIGHTS_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)) {
			return "AccessRightsController";
		}
		if (Patterns.match(Patterns.ACCESS_RIGHT_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.ACCESS_RIGHTS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && !representation.contains(":accessRightAnnc"))) {
			return "AccessRightController";
		}
		if (Patterns.match(Patterns.ACCESS_RIGHT_ANNC_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.ACCESS_RIGHTS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && representation.contains(":accessRightAnnc"))) {
			return "AccessRightAnncController";
		}
		if (Patterns.match(Patterns.GROUPS_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)) {
			return "GroupsController";
		}
		if (Patterns.match(Patterns.GROUP_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.GROUPS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && !representation.contains(":groupAnnc"))) {
			return "GroupController";
		}
		if (Patterns.match(Patterns.GROUP_ANNC_PATTERN, uri) && !method.equals(Constants.METHOD_CREATE)
				|| (Patterns.match(Patterns.GROUPS_PATTERN, uri) && method.equals(Constants.METHOD_CREATE) && representation.contains(":groupAnnc"))) {
			return "GroupAnncController";
		}
		if (Patterns.match(Patterns.MEMBERS_CONTENT_PATTERN, uri)) {
			return "MembersContentController";
		}
		if (Patterns.match(Patterns.DISCOVERY_PATTERN, uri)) {
			return "DiscoveryController";
		}
		if (Patterns.match(Patterns.MGMT_OBJS_PATTERN, uri)) {
			return "MgmtObjsController";
		}
		if (Patterns.match(Patterns.MGMT_OBJ_PATTERN, uri)) {
			return "MgmtObjController";
		}
		if (Patterns.match(Patterns.PARAMETERS_PATTERN, uri)) {
			return "ParametersController";
		}
		if (Patterns.match(Patterns.PARAMETER_PATTERN, uri)) {
			return null;
		}
		if (Patterns.match(Patterns.MGMT_CMD_PATTERN, uri)) {
			return "MgmtCmdController";
		}
		if (Patterns.match(Patterns.EXEC_INSTANCES_PATTERN, uri)) {
			return "ExecInstancesController";
		}
		if (Patterns.match(Patterns.EXEC_INSTANCE_PATTERN, uri)) {
			return "ExecInstanceController";
		}
		if (Patterns.match(Patterns.ATTACHED_DEVICES_PATTERN, uri)) {
			return "AttachedDevicesController";
		}
		if (Patterns.match(Patterns.ATTACHED_DEVICE_PATTERN, uri)) {
			return "AttachedDeviceController";
		}
		if (Patterns.match(Patterns.NOTIFICATION_CHANNELS_PATTERN, uri)) {
			return "NotificationChannelsController";
		}
		if (Patterns.match(Patterns.NOTIFICATION_CHANNEL_PATTERN, uri)) {
			return "NotificationChannelController";
		}
		if (Patterns.match(Patterns.M2M_POCS_PATTERN, uri)) {
			return "M2MPocsController";
		}
		if (Patterns.match(Patterns.M2M_POC_PATTERN, uri)) {
			return "M2MPocController";
		}

		return null;
	}

	/**
	 * 在nscl上取得nscl與 gscl之間的average packet loss rate
	 */
	private static Integer getAveragePacketLossRate() {

		Router scl = new Router();

		// Build a RequestIndication
		RequestIndication request = new RequestIndication();
		request.setMethod(Constants.METHOD_RETREIVE);
		request.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
		String strTargetId = Constants.SCL_ID + Refs.APPLICATIONS_REF + "/NetworkStatus" + Refs.CONTAINERS_REF + "/PacketLossRate" + Refs.CONTENTINSTANCES_REF;
		// nscl/applications/NetworkStatus/containers/PacketLossRate/contentInstances

		/***************************************************************************
		 * 取出 content
		 **************************************************************************/
		ArrayList<String> listContent64 = new ArrayList<String>();

		try {
			// send the RequestIndication
			request.setTargetID(strTargetId);
			String strBody = scl.doRequest(request).getRepresentation();

			//System.out.println("strBody = " + strBody);
			
			// regex
			String strContentInstanceId = "";
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
//			System.out.println("Content:\n" + content + "\n");
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
}
