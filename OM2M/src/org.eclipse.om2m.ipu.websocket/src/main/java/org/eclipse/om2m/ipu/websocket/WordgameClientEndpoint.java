package org.eclipse.om2m.ipu.websocket;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

@ClientEndpoint
public class WordgameClientEndpoint {

	private static CountDownLatch latch;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@OnOpen
	public void onOpen(Session session) {
		logger.info("Connected ... " + session.getId());
	}

	@OnMessage
	public void onMessage(String message, Session session) {
		logger.info("Received ...." + message);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		logger.info(String.format("Session %s close because of %s", session.getId(), closeReason));
		// latch.countDown();
	}

	public static void main(String[] args) {
		latch = new CountDownLatch(1);
		ClientManager client = ClientManager.createClient();
		
		try {
			final Session my_session = client.connectToServer(WordgameClientEndpoint.class, new URI("ws://localhost:8025/websockets/game"));
			
			latch.await();
		} catch (DeploymentException | URISyntaxException | InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
		
	}
}