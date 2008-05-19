package se.sics.kompics.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.DefaultIoFilterChainBuilder;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentShareMethod;
import se.sics.kompics.api.annotation.ComponentType;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.events.NetworkDeliverEvent;
import se.sics.kompics.network.events.NetworkSendEvent;

@ComponentType
public class NetworkComponent {

	// private static final Logger logger = LoggerFactory
	// .getLogger(NetworkComponent.class);

	private Component component;

	private Channel sendChannel, deliverChannel;

	/* Acceptors and connectors */
	private NioDatagramAcceptor udpAcceptor;

	private NioDatagramConnector udpConnector;

	private NioSocketAcceptor tcpAcceptor;

	private NioSocketConnector tcpConnector;

	/* Sessions maps */
	private ConcurrentHashMap<Address, IoSession> udpSessions;

	private ConcurrentHashMap<Address, IoSession> tcpSessions;

	private ConcurrentHashMap<Address, ConnectListener> pendingConnections;

	private NetworkHandler networkHandler;

	private SocketAddress localSocketAddress;

	public NetworkComponent(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel sendChannel, Channel deliverChannel) {
		this.sendChannel = sendChannel;
		this.deliverChannel = deliverChannel;

		component.subscribe(sendChannel, "handleNetworkSendEvent");
		component.bind(NetworkDeliverEvent.class, deliverChannel);

		udpSessions = new ConcurrentHashMap<Address, IoSession>();
		tcpSessions = new ConcurrentHashMap<Address, IoSession>();
		pendingConnections = new ConcurrentHashMap<Address, ConnectListener>();

		networkHandler = new NetworkHandler(this);

	}

	@ComponentShareMethod
	public ComponentMembrane share(String name) {
		HashMap<Class<? extends Event>, Channel> map = new HashMap<Class<? extends Event>, Channel>();
		map.put(NetworkSendEvent.class, sendChannel);
		map.put(NetworkDeliverEvent.class, deliverChannel);
		ComponentMembrane membrane = new ComponentMembrane(component, map);
		return component.registerSharedComponentMembrane(name, membrane);
	}

	@ComponentInitializeMethod
	public void init(SocketAddress socketAddress) throws IOException {
		// local address
		localSocketAddress = socketAddress;

		// UDP Acceptor
		udpAcceptor = new NioDatagramAcceptor();
		udpAcceptor.setHandler(networkHandler);
		DefaultIoFilterChainBuilder udpAcceptorChain = udpAcceptor
				.getFilterChain();
		udpAcceptorChain.addLast("protocol", new ProtocolCodecFilter(
				new ObjectSerializationCodecFactory()));
		udpAcceptor.getSessionConfig().setReuseAddress(true);
		udpAcceptor.bind(localSocketAddress);

		// UDP Connector
		udpConnector = new NioDatagramConnector();
		udpConnector.setHandler(networkHandler);
		DefaultIoFilterChainBuilder udpConnectorChain = udpConnector
				.getFilterChain();
		udpConnectorChain.addLast("protocol", new ProtocolCodecFilter(
				new ObjectSerializationCodecFactory()));

		// TCP Acceptor
		tcpAcceptor = new NioSocketAcceptor();
		tcpAcceptor.setHandler(networkHandler);
		DefaultIoFilterChainBuilder tcpAcceptorChain = tcpAcceptor
				.getFilterChain();
		tcpAcceptorChain.addLast("protocol", new ProtocolCodecFilter(
				new ObjectSerializationCodecFactory()));
		tcpAcceptor.getSessionConfig().setReuseAddress(true);
		tcpAcceptor.bind(localSocketAddress);

		// TCP Connector
		tcpConnector = new NioSocketConnector();
		tcpConnector.setHandler(networkHandler);
		DefaultIoFilterChainBuilder tcpConnectorChain = tcpConnector
				.getFilterChain();
		tcpConnectorChain.addLast("protocol", new ProtocolCodecFilter(
				new ObjectSerializationCodecFactory()));
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(NetworkDeliverEvent.class)
	public void handleNetworkSendEvent(NetworkSendEvent event) {
		// logger.debug("Handling NetSendEvent: " +
		// event.getNetworkDeliverEvent()
		// + " from " + event.getSource() + " to "
		// + event.getDestination());

		NetworkDeliverEvent deliverEvent = event.getNetworkDeliverEvent();
		if (event.getDestination().equals(event.getSource())) {
			// deliver locally
			component.triggerEvent(deliverEvent);
			return;
		}

		Transport protocol = event.getProtocol();
		Address destination = event.getDestination();

		// check if connection exists
		if (alreadyConnected(destination, protocol)) {
			// send message
			IoSession session = getSession(destination, protocol);
			session.write(deliverEvent);
		} else {
			// create connection
			if (pendingConnections.containsKey(destination)) {
				// add pending message to pending connection
				pendingConnections.get(destination).addPendingMessage(
						deliverEvent);
			} else {
				// create pending connection
				IoConnector connector = (protocol == Transport.UDP ? udpConnector
						: tcpConnector);
				ConnectFuture connFuture = connector
						.connect(new InetSocketAddress(destination.getIp(),
								destination.getPort()));

				// Create listener for the connection
				ConnectListener listener = new ConnectListener(this, protocol,
						destination);

				// Enqueue the message for later transmission
				listener.addPendingMessage(deliverEvent);

				pendingConnections.put(destination, listener);
				connFuture.addListener(listener);
			}
		}
	}

	void deliverMessage(NetworkDeliverEvent message, Transport protocol) {
		// logger.debug("Delivering message " + message.getClass() + " from "
		// + message.getSource() + " to " + message.getDestination());

		message.setProtocol(protocol);
		component.triggerEvent(message);
	}

	boolean alreadyConnected(Address address, Transport protocol) {
		switch (protocol) {
		case UDP:
			return udpSessions.contains(address);
		case TCP:
			return tcpSessions.contains(address);
		}
		return false;
	}

	IoSession getSession(Address address, Transport protocol) {
		switch (protocol) {
		case UDP:
			return udpSessions.get(address);
		case TCP:
			return tcpSessions.get(address);
		}
		return null;
	}

	void addSession(Address address, IoSession session, Transport protocol) {
		switch (protocol) {
		case UDP:
			udpSessions.put(address, session);
			break;
		case TCP:
			tcpSessions.put(address, session);
			break;
		}
	}

	void removePendingConnection(Address address) {
		pendingConnections.remove(address);
	}
}
