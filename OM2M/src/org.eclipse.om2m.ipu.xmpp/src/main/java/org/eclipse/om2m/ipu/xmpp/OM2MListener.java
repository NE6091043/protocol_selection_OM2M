package org.eclipse.om2m.ipu.xmpp;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.om2m.commons.resource.StatusCode;
import org.eclipse.om2m.commons.rest.RequestIndication;
import org.eclipse.om2m.commons.rest.ResponseConfirm;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

public class OM2MListener {

	/** Discovered SCL service */
	static SclService SCL;

	private static XMPPConnection conn;

	public OM2MListener(SclService scl) {
		OM2MListener.SCL = scl;
	}

	public static SclService getScl() {
		return SCL;
	}

	public static void out(String msg) {
		for (int i = 1; i <= 5; i++) {
			System.out.println(msg);
		}
	}

	public boolean connectToXMPPServer(URI uri) {

		try {

			if (Constants.SCL_ID.equalsIgnoreCase("nscl")) {
				// out("nscl in connectToXMPPServer in OM2MListener.java");
				// out("uri.getHost() = " + uri.getHost());
				// out("uri.getPort() = " + uri.getPort());

				ConnectionConfiguration config = new ConnectionConfiguration(uri.getHost(), uri.getPort());
				setConn(new XMPPConnection(config));
				getConn().connect();

				// // 建立帳號
				// try {
				// getConn().getAccountManager().createAccount("om2m_nscl",
				// "om2m_nscl");
				// } catch (Exception e) {
				//
				// }

				// 登入帳號
				getConn().login("om2m_nscl", "om2m_nscl");

			} else if (Constants.SCL_ID.equalsIgnoreCase("gscl")) {
				// out("gscl in connectToXMPPServer in OM2MListener.java");
				// out("uri.getHost() = " + uri.getHost());
				// out("uri.getPort() = " + uri.getPort());

				ConnectionConfiguration config = new ConnectionConfiguration(uri.getHost(), uri.getPort());
				setConn(new XMPPConnection(config));
				getConn().connect();

				// // 建立帳號
				// try {
				// getConn().getAccountManager().createAccount("om2m_gscl",
				// "om2m_gscl");
				// } catch (Exception e) {
				//
				// }

				// 登入帳號
				getConn().login("om2m_gscl", "om2m_gscl");
			}

			// 在connection連線上添加監聽器
			addChatListener(getConn());

			return true;
		} catch (Exception e) {
			System.out.println("connectToXMPPServer() error in OM2MListener.java");
			return false;
		}
	}

	public static void addChatListener(XMPPConnection conn) {
		// 取得聊天管理器
		ChatManager cm = conn.getChatManager();

		/*
		 * 添加監聽器
		 */
		cm.addChatListener(new ChatManagerListener() {

			@Override
			public void chatCreated(Chat chat, boolean create) {
				chat.addMessageListener(new MessageListener() {

					@Override
					public void processMessage(Chat chat, Message msg) {

						System.out.println("chat.getParticipant() = " + chat.getParticipant());
						System.out.println("msg.getSubject() = " + msg.getSubject());
						System.out.println("msg.getBody() = " + msg.getBody());

						String targetId = msg.getSubject();
						String body = msg.getBody();
						try {
							if (isNotify(body)) {
								// 在nscl接收到gscl用xmpp送來的notification。
								RequestIndication requestIndication = new RequestIndication();
								requestIndication.setMethod(Constants.METHOD_CREATE);
								requestIndication.setRequestingEntity(Constants.ADMIN_REQUESTING_ENTITY);
								requestIndication.setRepresentation(body);

								requestIndication.setBase("");
								requestIndication.setTargetID(targetId);

								System.out.println(getScl().doRequest(requestIndication));
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

									// set the requestIndication
									// parameters
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

								if (body.length() == 0) {
									requestIndication.setMethod(Constants.METHOD_EXECUTE);
									requestIndication.setTargetID(targetId);
								} else {
									requestIndication.setMethod(Constants.METHOD_CREATE);
									requestIndication.setTargetID(targetId);
								}
								requestIndication.setRepresentation(body);
								ResponseConfirm resp = getScl().doRequest(requestIndication);
								System.out.println(resp);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});
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

	public static XMPPConnection getConn() {
		return conn;
	}

	public static void setConn(XMPPConnection conn) {
		OM2MListener.conn = conn;
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
}
