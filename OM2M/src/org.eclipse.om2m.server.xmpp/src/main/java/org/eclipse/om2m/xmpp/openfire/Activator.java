package org.eclipse.om2m.xmpp.openfire;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.om2m.core.service.SclService;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.user.User;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {

	private static BundleContext context;

	/** Logger */
	private static Log LOGGER = LogFactory.getLog(Activator.class);

	/** SCL service tracker */
	private ServiceTracker<Object, Object> sclServiceTracker;

	/** the xmpp server */
	private static XMPPServer xmppServer;

	private static int xmmpp_port = Integer.parseInt(System.getProperty("org.eclipse.om2m.xmpp.port", "5222"));

	@Override
	public void start(BundleContext bundleContext) {
		Activator.context = bundleContext;

		StartXMPPServer();

	}

	@Override
	public void stop(BundleContext bundleContext) {
		Activator.context = null;
		StopXMPPServer();
	}

	private void StartXMPPServer() {
		try {

			// Start the XMPP server
			// openfireHome需設為 conf、libs、plugins、resources的路徑
			File configFile = new File("./XMPP");
			if (!configFile.exists()) {
				configFile = new File("../../../../../../XMPP");
			}
			LOGGER.info("Using XMPP config file: " + configFile.getAbsolutePath());
			System.setProperty("openfireHome", configFile.getAbsolutePath());

			if (xmppServer == null) {
				xmppServer = new XMPPServer();
				xmppServer.getInstance().getConnectionManager().setClientListenerPort(xmmpp_port);
			} else {
				xmppServer.getInstance().getConnectionManager().setClientListenerPort(xmmpp_port);
				xmppServer.getInstance().start();
			}
			Thread.sleep(2000); // waiting...

			LOGGER.info("XMPP Server started.");
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	private void StopXMPPServer() {
		try {
			xmppServer.getInstance().stop();
			Thread.sleep(2000); // waiting...
			LOGGER.info("XMPP Server stopped.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static BundleContext getContext() {
		return context;
	}
}
