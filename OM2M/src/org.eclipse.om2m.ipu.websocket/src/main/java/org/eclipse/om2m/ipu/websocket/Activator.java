package org.eclipse.om2m.ipu.websocket;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;
import org.glassfish.tyrus.client.ClientManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

public class Activator implements BundleActivator {

	private static BundleContext context;

	/** Logger */
	private static Log logger = LogFactory.getLog(Activator.class);

	/** SCL service tracker */
	private ServiceTracker<Object, Object> sclServiceTracker;

	/** WebSocket server */
	private MyWebSocketServer mWebSocketServer;

	/** WebSocket client */
	private static ClientManager websocket_client;

	/** WebSocket client session */
	private static Session session;

	public static Session getSession() {
		return session;
	}

	public static void setSession(Session session) {
		Activator.session = session;
	}

	public static ClientManager getWebsocketClient() {
		return websocket_client;
	}

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;

		RegisterService();

		// start local's WebSocket Server
		mWebSocketServer = new MyWebSocketServer();
		mWebSocketServer.start();
		logger.info("WebSocket server started.");

		trackSclService(getContext());

		// connect to remote's WebSocket Server
		if (Constants.SCL_ID.equalsIgnoreCase("gscl")) {
			URI uri = new URI("ws://" + Constants.NSCL_IP + ":2014");
			connectToWebsocketServer(uri);
		}
	}

	private void RegisterService() {
		// Register the Rest WebSocket Client
		logger.info("Register WebSocket RestClientService..");
		getContext().registerService(RestClientService.class.getName(), new OM2MWebsocketClient(), null);
		logger.info("WebSocket RestClientService is registered.");

	}

	private void trackSclService(BundleContext bundleContext) {
		/***************************************************************************
		 * track the SCL service
		 **************************************************************************/
		sclServiceTracker = new ServiceTracker<Object, Object>(bundleContext, SclService.class.getName(), null) {
			public void removedService(ServiceReference<Object> reference, Object service) {
				logger.info("SclService removed");
				try {
				} catch (IllegalArgumentException e) {
					logger.error("Error removing SclService", e);
				}
			}

			public Object addingService(ServiceReference<Object> reference) {
				logger.info("SclService discovered");
				SclService scl = (SclService) this.context.getService(reference);
				try {
					WordgameServerEndpoint.setSCL(scl);
				} catch (Exception e) {
					logger.error("Error adding SclService", e);
				}
				return scl;
			}
		};
		// Open service trackers
		sclServiceTracker.open();
		logger.info("SclService opened");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		if (mWebSocketServer != null) {
			mWebSocketServer.stopWebsocketServer();
		}
		logger.info("Websocket server stopped.");

		if (session != null) {
			session.close();
		}
		logger.info("Websocket client stopped.");
	}

	public static void connectToWebsocketServer(URI uri) {

		while (session == null || !session.isOpen()) {
			try {
				websocket_client = ClientManager.createClient();
				final Session my_session = websocket_client.connectToServer(WordgameClientEndpoint.class, uri);
				my_session.setMaxIdleTimeout(0);
				setSession(my_session);
				System.out.printf("Connecting to : %s%n", uri);

				break;
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("connection error");
				System.out.println();
				
				// 將錯誤訊息寫到文字檔裡
				try {
					FileWriter fw = new FileWriter("test_websocket_error_msg.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
					BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
					bw.write(e.getMessage() + ", connection error, reconnect");
					bw.write("\r\n");
					bw.flush();
					bw.close();
				} catch (IOException e2) {
					e2.printStackTrace();
				}
			}
		}
	}

	public static boolean sendStr(URI uri, String str) {

		try {
			out("send message using websocket");
			getSession().getBasicRemote().sendText(str);
			return true;
		} catch (Exception me) {
			System.out.println("msg " + me.getMessage());
			System.out.println("loc " + me.getLocalizedMessage());
			System.out.println("cause " + me.getCause());
			System.out.println("excep " + me);
			me.printStackTrace();

			StringBuilder sb = new StringBuilder();
			sb.append("msg " + me.getMessage());

			// 將錯誤訊息寫到文字檔裡
			try {
				FileWriter fw = new FileWriter("test_websocket_error_msg.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
				BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
				bw.write(sb.toString());
				bw.write("\r\n");
				bw.flush();
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void out(String msg) {
		for (int i = 1; i <= 10; i++) {
			System.out.println(msg);
		}
	}

}
