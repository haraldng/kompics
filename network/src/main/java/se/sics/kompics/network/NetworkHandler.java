package se.sics.kompics.network;

import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.network.events.Message;
import se.sics.kompics.network.events.NetworkException;
import se.sics.kompics.network.events.NetworkSessionClosed;
import se.sics.kompics.network.events.NetworkSessionOpened;

public class NetworkHandler extends IoHandlerAdapter {

	private static final Logger logger = LoggerFactory
			.getLogger(NetworkHandler.class);

	private Network networkComponent;

	public NetworkHandler(Network networkComponent) {
		super();
		this.networkComponent = networkComponent;
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		InetSocketAddress address = (InetSocketAddress) session
				.getAttribute("address");

		if (address != null)
			logger.debug("Problems with {} connection to {}",
					(Transport) session.getAttribute("protocol"), address);
		logger.error("Exception caught:", cause);

		networkComponent.networkException(new NetworkException(address));
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		super.messageReceived(session, message);
		Transport protocol = (Transport) session.getAttribute("protocol");

		logger.debug("Message received from {}", session.getRemoteAddress());
		networkComponent
				.deliverMessage((Message) message, protocol);
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		super.messageSent(session, message);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		logger.debug("Connection closed to {}", session.getRemoteAddress());
		networkComponent.networkSessionClosed(new NetworkSessionClosed(session
				.getRemoteAddress()));
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);
		logger.debug("Connection opened to {}", session.getRemoteAddress());
		networkComponent.networkSessionOpened(new NetworkSessionOpened(session
				.getRemoteAddress()));
	}
}
