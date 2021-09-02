package org.eclipse.om2m.ipu.websocket;

import org.glassfish.tyrus.server.Server;

//import org.eclipse.jetty.server.Server;

public class MyWebSocketServer extends Thread {

	private static int websocket_port = Integer.parseInt(System.getProperty("org.eclipse.om2m.websocket.port", "2014"));

	Server server;

	@Override
	public void run() {

		/***************************************************************************
		 * jetty websocket
		 **************************************************************************/
		// try {
		// server = new Server(websocket_port);
		// server.setHandler(new MyWebSocketServerHandler());
		// server.setStopTimeout(0);
		// server.start();
		// server.join();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		/***************************************************************************
		 * tyrus websocket
		 **************************************************************************/
		server = new Server("localhost", websocket_port, "/", null, WordgameServerEndpoint.class);

		try {
			server.start();
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stopWebsocketServer() {

		if (server != null) {
			try {
				server.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
