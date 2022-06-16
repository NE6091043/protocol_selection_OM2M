package org.eclipse.om2m.ipu.xmpp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.comm.service.RestClientService;
import org.eclipse.om2m.core.constants.Constants;
import org.eclipse.om2m.core.service.SclService;
import org.eclipse.om2m.ipu.service.IpuService;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static BundleContext context;

	/** Logger */
	private static Log logger = LogFactory.getLog(Activator.class);

	/** SCL service tracker */
	private ServiceTracker<Object, Object> sclServiceTracker;

	/** Discovered SCL service */
	static SclService SCL;

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

		trackSclService(getContext());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;

		if (OM2MListener.getConn() != null) {
			OM2MListener.getConn().disconnect();
		}
	}

	private void RegisterService() {
		// Register the Rest XMPP Client
		logger.info("Register XMPP RestClientService..");
		getContext().registerService(RestClientService.class.getName(), new XMPPClient(), null);
		logger.info("XMPP RestClientService is registered.");

		// Register the Controller
		logger.info("Register XMPPController..");
		getContext().registerService(IpuService.class.getName(), new XMPPController(), null);
		logger.info("XMPPController is registered.");
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
				SclService scl_service = (SclService) this.context.getService(reference);
				SCL = scl_service;
				try {
					OM2MListener om2m_listener = new OM2MListener(scl_service);

					if (Constants.SCL_ID.equalsIgnoreCase("gscl")) {

						// 在自己的xmpp server建立自己的帳號並登入
						CreateOwnXMPPAccount("om2m_gscl");

						// 在NSCL的xmpp server建立自己的帳號並登入
						URI uri = new URI("http://192.168.101.134:5222");
						om2m_listener.connectToXMPPServer(uri);

					} else if (Constants.SCL_ID.equalsIgnoreCase("nscl")) {

						// 在自己的xmpp server建立自己的帳號並登入
						CreateOwnXMPPAccount("om2m_nscl");
					}
				} catch (Exception e) {
					logger.error("Error adding SclService", e);
				}
				return scl_service;
			}

		};
		// Open service trackers
		sclServiceTracker.open();
		logger.info("SclService opened");
	}

	private void CreateOwnXMPPAccount(String strAccount) {
		ConnectionConfiguration config = new ConnectionConfiguration("127.0.0.1", 5222);
		XMPPConnection conn = new XMPPConnection(config);
		try {
			conn.connect();
		} catch (XMPPException e1) {
			System.out.println("CreateOwnXMPPAccount error");
		}

		// 建立帳號
		try {
			conn.getAccountManager().createAccount("om2m_nscl", "om2m_nscl");
			conn.getAccountManager().createAccount("om2m_gscl", "om2m_gscl");
		} catch (Exception e) {

		}

		// 登入帳號
		try {
			conn.login(strAccount, strAccount);
		} catch (XMPPException e) {
			System.out.println("Login server error in CreateOwnXMPPAccount() in Activator.java");
		}

		// 在connection連線上添加監聽器
		OM2MListener.addChatListener(conn);
	}

	public synchronized static void checkXMPPConn(URI uri) {
		try {
			while (OM2MListener.getConn() == null || !OM2MListener.getConn().isConnected()) {
				// out("in checkXMPPConn()");

				// 將結果寫到文字檔裡
				FileWriter fw = new FileWriter("test_xmpp_reconnection.txt", true); // True則表示用附加的方式寫到檔案原有內容之後
				BufferedWriter bw = new BufferedWriter(fw); // 將BufferedWeiter與FileWrite物件做連結
				bw.write("in checkXMPPConn()");
				bw.write("\r\n");
				bw.flush();
				bw.close();

				OM2MListener om2m_listener = new OM2MListener(SCL);
				if (om2m_listener.connectToXMPPServer(uri) == true) {
					break;
				}
			}
		} catch (Exception e) {
			System.out.println("checkXMPPConn() error");
		}
	}

	public static void out(String msg) {
		for (int i = 1; i <= 10; i++) {
			System.out.println(msg);
		}
	}

}
