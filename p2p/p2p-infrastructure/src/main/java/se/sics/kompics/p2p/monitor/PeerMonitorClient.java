package se.sics.kompics.p2p.monitor;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.api.Channel;
import se.sics.kompics.api.Component;
import se.sics.kompics.api.ComponentMembrane;
import se.sics.kompics.api.annotation.ComponentCreateMethod;
import se.sics.kompics.api.annotation.ComponentInitializeMethod;
import se.sics.kompics.api.annotation.ComponentSpecification;
import se.sics.kompics.api.annotation.EventHandlerMethod;
import se.sics.kompics.api.annotation.MayTriggerEventTypes;
import se.sics.kompics.network.Address;
import se.sics.kompics.p2p.chord.events.GetChordNeighborsRequest;
import se.sics.kompics.p2p.chord.events.GetChordNeighborsResponse;
import se.sics.kompics.p2p.monitor.events.ChangeUpdatePeriod;
import se.sics.kompics.p2p.monitor.events.PeerViewNotification;
import se.sics.kompics.p2p.monitor.events.SendView;
import se.sics.kompics.p2p.monitor.events.StartPeerMonitor;
import se.sics.kompics.p2p.monitor.events.StopPeerMonitor;
import se.sics.kompics.p2p.network.events.LossyNetworkDeliverEvent;
import se.sics.kompics.p2p.network.events.LossyNetworkSendEvent;
import se.sics.kompics.timer.TimerHandler;
import se.sics.kompics.timer.events.CancelTimerEvent;
import se.sics.kompics.timer.events.SetTimerEvent;
import se.sics.kompics.timer.events.TimerSignalEvent;

/**
 * The <code>PeerMonitorClient</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@ComponentSpecification
public class PeerMonitorClient {

	private Logger logger;

	private final Component component;

	private Channel timerSignalChannel, lnSendChannel;

	private Channel chordRequestChannel, chordResponseChannel;

	private TimerHandler timerHandler;

	private Address monitorServerAddress;

	private Address localPeerAddress;

	private long updatePeriod;

	public PeerMonitorClient(Component component) {
		super();
		this.component = component;
	}

	@ComponentCreateMethod
	public void create(Channel requestChannel) {
		// use shared timer component
		ComponentMembrane timerMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.Timer");
		Channel timerSetChannel = timerMembrane
				.getChannelIn(SetTimerEvent.class);
		timerSignalChannel = component.createChannel(TimerSignalEvent.class);

		this.timerHandler = new TimerHandler(component, timerSetChannel);

		// use shared LossyNetwork component
		ComponentMembrane lnMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.network.LossyNetwork");
		lnSendChannel = lnMembrane.getChannelIn(LossyNetworkSendEvent.class);
		Channel lnDeliverChannel = lnMembrane
				.getChannelOut(LossyNetworkDeliverEvent.class);

		// use shared Chord component
		ComponentMembrane crMembrane = component
				.getSharedComponentMembrane("se.sics.kompics.p2p.chord.Chord");
		chordRequestChannel = crMembrane
				.getChannelIn(GetChordNeighborsRequest.class);
		chordResponseChannel = component
				.createChannel(GetChordNeighborsResponse.class);

		component.subscribe(lnDeliverChannel, "handleChangeUpdatePeriod");
		component.subscribe(chordResponseChannel,
				"handleGetChordNeighborsResponse");

		component.subscribe(requestChannel, "handleStartPeerMonitor");
		component.subscribe(requestChannel, "handleStopPeerMonitor");
	}

	@ComponentInitializeMethod("monitor.properties")
	public void init(Properties properties, Address localAddress)
			throws UnknownHostException {
		this.localPeerAddress = localAddress;

		logger = LoggerFactory.getLogger(getClass().getName() + "@"
				+ localAddress.getId());

		InetAddress ip = InetAddress.getByName(properties
				.getProperty("monitor.server.ip"));
		int port = Integer.parseInt(properties
				.getProperty("monitor.server.port"));

		updatePeriod = 1000 * Integer.parseInt(properties
				.getProperty("client.refresh.period"));

		monitorServerAddress = new Address(ip, port, BigInteger.ZERO);
		localPeerAddress = localAddress;

		component.subscribe(timerSignalChannel, "handleSendView");
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(SetTimerEvent.class)
	public void handleStartPeerMonitor(StartPeerMonitor event) {
		SendView timerEvent = new SendView(localPeerAddress.getId());

		// timerHandler.setTimer(timerEvent, timerSignalChannel, updatePeriod);
		component.triggerEvent(timerEvent, timerSignalChannel);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(CancelTimerEvent.class)
	public void handleStopPeerMonitor(StopPeerMonitor event) {
		timerHandler.cancelAllOutstandingTimers();
	}

	@EventHandlerMethod
	public void handleChangeUpdatePeriod(ChangeUpdatePeriod event) {
		updatePeriod = event.getNewUpdatePeriod();
	}

	@EventHandlerMethod
	@MayTriggerEventTypes( { GetChordNeighborsRequest.class,
			SetTimerEvent.class })
	public void handleSendView(SendView event) {
		logger.debug("SEND_VIEW");

		GetChordNeighborsRequest request = new GetChordNeighborsRequest(
				chordResponseChannel);
		component.triggerEvent(request, chordRequestChannel);

		// reset the timer for sending the next view notification
		timerHandler.setTimer(event, timerSignalChannel, updatePeriod);
	}

	@EventHandlerMethod
	@MayTriggerEventTypes(LossyNetworkSendEvent.class)
	public void handleGetChordNeighborsResponse(GetChordNeighborsResponse event) {
		logger.debug("GET_CHORD_NEIGHBORS_RESP");

		HashMap<String, Object> map = new HashMap<String, Object>();

		map.put("ChordRing", event);

		PeerViewNotification viewNotification = new PeerViewNotification(
				localPeerAddress, map);

		LossyNetworkSendEvent sendEvent = new LossyNetworkSendEvent(
				viewNotification, monitorServerAddress);
		component.triggerEvent(sendEvent, lnSendChannel);
	}
}
