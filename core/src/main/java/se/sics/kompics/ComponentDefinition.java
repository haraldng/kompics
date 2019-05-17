/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

import java.util.Optional;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import se.sics.kompics.Fault.ResolveAction;
import se.sics.kompics.config.Config;
import se.sics.kompics.config.ConfigUpdate;

/**
 * The <code>ComponentDefinition</code> class.
 *
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @version $Id$
 */
public abstract class ComponentDefinition {

    /**
     * Negative.
     *
     * @param portType
     *            the port type
     *
     * @return the negative < p>
     */
    protected final <P extends PortType> Negative<P> negative(Class<P> portType) {
        return core.createNegativePort(portType);
    }

    protected final <P extends PortType> Negative<P> provides(Class<P> portType) {
        return core.createNegativePort(portType);
    }

    /**
     * Positive.
     *
     * @param portType
     *            the port type
     *
     * @return the positive < p>
     */
    protected final <P extends PortType> Positive<P> positive(Class<P> portType) {
        return core.createPositivePort(portType);
    }

    /**
     * specifies that this component requires a port of type <code>portType</code>.
     *
     * @param <P>
     * @param portType
     * @return
     */
    protected final <P extends PortType> Positive<P> requires(Class<P> portType) {
        return core.createPositivePort(portType);
    }

    /**
     * Trigger.
     *
     * @param event
     *            the event
     * @param port
     *            the port
     */
    protected final <P extends PortType> void trigger(KompicsEvent event, Port<P> port) {
        if (event instanceof Direct.Request) {
            Direct.Request<?> r = (Direct.Request<?>) event;
            r.setOrigin(port.getPair());
            logger.trace("Set port on request {} to {}", r, r.getOrigin());
        } else if (event instanceof Direct.Response) {
            throw new KompicsException(
                    "Direct.Response can not be \"trigger\"ed. It has to \"answer\" a Direct.Request!");
        }
        // System.out.println(this.getClass()+": "+event+" triggert on "+port);
        port.doTrigger(event, core.wid, core);
    }

    protected final <P extends PortType> void answer(Direct.Request<?> event) {
        if (!event.hasResponse()) {
            logger.warn("Can't trigger a response for {} since none was given!", event);
            return;
        }
        event.getOrigin().doTrigger(event.getResponse(), core.wid, core);
    }

    protected final <P extends PortType> void answer(Direct.Request<?> req, Direct.Response resp) {
        req.getOrigin().doTrigger(resp, core.wid, core);
    }

    /*
     * java8 protected final <E extends KompicsEvent, P extends PortType> Handler<E> handle(Port<P> port, Class<E> type,
     * Consumer<E> fun) { Handler<E> handler = new FunctionHandler<>(type, fun); subscribe(handler, port); return
     * handler; }
     * 
     * protected final Handler<Start> onStart(Consumer<Start> fun) { return handle(control, Start.class, fun); }
     * 
     * protected final Handler<Stop> onStop(Consumer<Stop> fun) { return handle(control, Stop.class, fun); }
     */
    /**
     * Subscribe.
     *
     * @param handler
     *            the handler
     * @param port
     *            the port
     * @throws ConfigurationException
     */
    protected final <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) {
        if (port instanceof JavaPort) {
            JavaPort<P> p = (JavaPort<P>) port;
            p.doSubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler subscription only works in Java");
        }
    }

    protected final void subscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
        if (port instanceof JavaPort) {
            JavaPort<?> p = (JavaPort<?>) port;
            if (p.owner.equals(this.core)) {
                p.doSubscribe(handler);
            } else {
                throw new ConfigurationException("Cannot subscribe Handlers to other component's ports, "
                        + "since the behaviour of this is unspecifed. "
                        + "(The handler might be executed on the wrong thread)");
            }

        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler subscription only works with matching ports and components");
        }
    }

    protected final void unsubscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
        if (port instanceof JavaPort) {
            JavaPort<?> p = (JavaPort<?>) port;
            p.doUnsubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler unsubscription only works with matching ports and components");
        }
    }

    /**
     * Unsubscribe.
     *
     * @param handler
     *            the handler
     * @param port
     *            the port
     * @throws ConfigurationException
     */
    protected final <E extends KompicsEvent, P extends PortType> void unsubscribe(Handler<E> handler, Port<P> port)
            throws ConfigurationException {
        if (port instanceof JavaPort) {
            JavaPort<P> p = (JavaPort<P>) port;
            p.doUnsubscribe(handler);
        } else {
            throw new ConfigurationException("Port (" + port.toString() + " is not an instance of JavaPort!"
                    + "Handler (un)subscription only works in Java");
        }
    }

    /**
     * Creates the.
     *
     * @param definition
     *            the definition
     * @param initEvent
     *            init event to be passed to constructor
     *
     * @return the component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
        return core.doCreate(definition, Optional.of(initEvent));
    }

    /**
     * Creates the.
     *
     * @param definition
     *            the definition
     * @param initEvent
     *            none
     *
     * @return the component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
        Optional<Init<T>> init = Optional.empty();
        return core.doCreate(definition, init);
    }

    /**
     * Creates the.
     *
     * @param definition
     *            the definition
     * @param initEvent
     *            init event to be passed to constructor
     *
     * @return the component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent,
            ConfigUpdate update) {
        return core.doCreate(definition, Optional.of(initEvent), Optional.of(update));
    }

    /**
     * Creates the.
     *
     * @param definition
     *            the definition
     * @param initEvent
     *            none
     *
     * @return the component
     */
    protected final <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent,
            ConfigUpdate update) {
        Optional<Init<T>> init = Optional.empty();
        return core.doCreate(definition, init, Optional.of(update));
    }

    protected final void destroy(Component component) {
        core.doDestroy(component);
    }

    /**
     *
     * @param <P>
     * @param negative
     * @param positive
     * @deprecated Use {@link #connect(Positive, Negative, ChannelFactory) } instead
     */
    @Deprecated
    protected final <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    /**
     *
     * @param <P>
     * @param negative
     * @param positive
     * @deprecated Use {@link #connect(Positive, Negative, ChannelFactory) } instead
     */
    @Deprecated
    protected final <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    /**
     *
     * @param <P>
     * @param negative
     * @param positive
     * @deprecated Use {@link #connect(Positive, Negative, ChannelSelector, ChannelFactory) } instead
     */
    @Deprecated
    protected <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
            ChannelSelector<?, ?> selector) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    /**
     *
     * @param <P>
     * @param negative
     * @param positive
     * @deprecated Use {@link #connect(Positive, Negative, ChannelSelector, ChannelFactory) } instead
     */
    @Deprecated
    protected <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
            ChannelSelector<?, ?> selector) {
        return Channel.TWO_WAY.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    protected <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
            ChannelSelector<?, ?> selector, ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    protected <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
            ChannelSelector<?, ?> selector, ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative, selector);
    }

    protected <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
            ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    protected <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
            ChannelFactory factory) {
        return factory.connect((PortCore<P>) positive, (PortCore<P>) negative);
    }

    /**
     *
     * @param <P>
     * @param negative
     * @param positive
     * @deprecated Use {@link #disconnect(Channel)} or {@link Channel.disconnect()} instead
     */
    @Deprecated
    protected final <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
        PortCore<P> pos = (PortCore<P>) positive;
        PortCore<P> neg = (PortCore<P>) negative;
        List<Channel<P>> channels = pos.findChannelsTo(neg);
        for (Channel<P> c : channels) {
            c.disconnect();
        }
    }

    /**
     *
     * @param <P>
     * @param negative
     * @param positive
     * @deprecated Use {@link #disconnect(Channel)} or {@link Channel.disconnect()} instead
     */
    @Deprecated
    protected final <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
        PortCore<P> pos = (PortCore<P>) positive;
        PortCore<P> neg = (PortCore<P>) negative;
        List<Channel<P>> channels = neg.findChannelsTo(pos);
        for (Channel<P> c : channels) {
            c.disconnect();
        }
    }

    protected final <P extends PortType> void disconnect(Channel<P> c) {
        c.disconnect();
    }

    protected Negative<ControlPort> control;
    // different sides of the same port...naming is for readability in usage
    protected Negative<LoopbackPort> loopback;
    protected Positive<LoopbackPort> onSelf;

    public final Negative<ControlPort> getControlPort() {
        return control;
    }

    public final ComponentCore getComponentCore() {
        return core;
    }

    public final Config config() {
        return core.config();
    }

    public final UUID id() {
        return core.id();
    }

    public final void suicide() {
        if (core.state == Component.State.ACTIVE || core.state == Component.State.STARTING) {
            trigger(Kill.event, control.getPair());
        } else {
            logger.warn("Could not commit suicide as state is non-active");
        }
    }

    /**
     * Use for custom cleanup. Will be called after all child components have stopped, but before sending a Stopped
     * message to the parent.
     *
     */
    public void tearDown() {
        // Do nothing normally
    }

    /**
     * Override for custom error handling.
     * <p>
     * Default action is ESCALATE.
     * <p>
     * ESCALATE -> Forward fault to parent. IGNORE -> Drop fault. Resume component as if nothing happened. RESOLVED ->
     * Fault has been handled by user. Don't do anything else.
     * <p>
     * 
     * @param fault
     * @return
     */
    public ResolveAction handleFault(Fault fault) {
        return ResolveAction.ESCALATE;
    }

    /**
     * Override for custom update handling.
     * <p>
     * Default action is to propagate the original everywhere and apply to self.
     * <p>
     * 
     * @param update
     * @return
     */
    public UpdateAction handleUpdate(ConfigUpdate update) {
        return UpdateAction.DEFAULT;
    }

    /**
     * Override to perform actions after a ConfigUpdate was applied and forwarded.
     */
    public void postUpdate() {
    }

    public final void updateConfig(ConfigUpdate update) {
        core.doConfigUpdate(update);
    }

    /**
     * Override to allow components of this type to start their own independent {@link se.sics.kompics.config.Config} id
     * lines.
     * <p>
     * This is helpful in simulation, when simulating multiple independent nodes. Make sure that no
     * {@code ConfigUpdate}s are passed to siblings or parents of such nodes! (Override
     * {@link #handleUpdate(se.sics.kompics.config.ConfigUpdate)})
     * <p>
     * 
     * @return Whether to create a new config id line for this component (default: {@code true})
     */
    public boolean separateConfigId() {
        return false;
    }

    public final ComponentProxy proxy = new ComponentProxy() {

        @Override
        public <P extends PortType> void trigger(KompicsEvent e, Port<P> p) {
            ComponentDefinition.this.trigger(e, p);
        }

        @Override
        public <P extends PortType> void answer(Direct.Request<?> event) {
            ComponentDefinition.this.answer(event);
        }

        @Override
        public <P extends PortType> void answer(Direct.Request<?> req, Direct.Response resp) {
            ComponentDefinition.this.answer(req, resp);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
            return ComponentDefinition.this.create(definition, initEvent);
        }

        @Override
        public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent) {
            return ComponentDefinition.this.create(definition, initEvent);
        }

        @Override
        public void destroy(Component component) {
            ComponentDefinition.this.destroy(component);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
            return ComponentDefinition.this.connect(positive, negative);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
            return ComponentDefinition.this.connect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
            ComponentDefinition.this.disconnect(negative, positive);
        }

        @Override
        public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
            ComponentDefinition.this.disconnect(positive, negative);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
                ChannelSelector<?, ?> filter) {
            return ComponentDefinition.this.connect(positive, negative, filter);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive,
                ChannelSelector<?, ?> filter) {
            return ComponentDefinition.this.connect(negative, positive, filter);
        }

        @Override
        public Negative<ControlPort> getControlPort() {
            return ComponentDefinition.this.getControlPort();
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
                ChannelFactory factory) {
            return ComponentDefinition.this.connect(positive, negative, factory);
        }

        @Override
        public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative,
                ChannelSelector<?, ?> selector, ChannelFactory factory) {
            return ComponentDefinition.this.connect(positive, negative, selector, factory);
        }

        @Override
        public <P extends PortType> void disconnect(Channel<P> c) {
            ComponentDefinition.this.disconnect(c);
        }

        @Override
        public <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port) {
            ComponentDefinition.this.subscribe(handler, port);
        }

        @Override
        public void subscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
            ComponentDefinition.this.subscribe(handler, port);
        }

        @Override
        public void unsubscribe(MatchedHandler<?, ?, ?> handler, Port<?> port) {
            ComponentDefinition.this.unsubscribe(handler, port);
        }

        @Override
        public <E extends KompicsEvent, P extends PortType> void unsubscribe(Handler<E> handler, Port<P> port) {
            ComponentDefinition.this.unsubscribe(handler, port);
        }

        @Override
        public UUID id() {
            return ComponentDefinition.this.id();
        }

        @Override
        public <P extends PortType> Positive<P> getPositive(Class<P> portType) {
            return ComponentDefinition.this.getComponentCore().getPositive(portType);
        }

        @Override
        public <P extends PortType> Negative<P> getNegative(Class<P> portType) {
            return ComponentDefinition.this.getComponentCore().getNegative(portType);
        }

        @Override
        public <P extends PortType> Positive<P> requires(Class<P> portType) {
            return ComponentDefinition.this.requires(portType);
        }

        @Override
        public <P extends PortType> Negative<P> provides(Class<P> portType) {
            return ComponentDefinition.this.provides(portType);
        }
    };

    /* Logging Related */
    /**
     * Pre-configured MDC key for the unique component id.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     */
    public static final String MDC_KEY_CID = "kcomponent-id";
    /**
     * Pre-configured MDC key for the current component lifecycle state.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     */
    public static final String MDC_KEY_CSTATE = "kcomponent-state";

    /**
     * Kompics provided slf4j logger with managed diagnostic context.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, String> mdcState = new HashMap<>();
    private final Map<String, String> mdcReset = new HashMap<>();

    /**
     * Should not be necessary to call usually, as ComponentCore will do it.
     * <p>
     * Protected mainly for use by Kompics Scala.
     * <p>
     * Can also be used to set component MDC when executing related off-kompics work (check for concurrency issues,
     * though!).
     */
    protected void setMDC() {
        MDC.setContextMap(mdcState);
    }

    /**
     * Associate key with value in the logging diagnostic context.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     *
     * @param key
     * @param value
     */
    protected void loggingCtxPut(String key, String value) {
        mdcState.put(key, value);
        MDC.put(key, value);
    }

    /**
     * Associate key permanently with value in the logging diagnostic context.
     * <p>
     * Keys set in this way are not removed by {@link #loggingCtxReset()} or {@link #loggingCtxRemove()}.
     * <p>
     * See <a href="https://logback.qos.ch/manual/mdc.html">the logback manuel</a> for how to use this with logback.
     *
     * @param key
     * @param value
     */
    protected void loggingCtxPutAlways(String key, String value) {
        mdcReset.put(key, value);
        mdcState.put(key, value);
        MDC.put(key, value);
    }

    /**
     * Disassociate any value with the key in the logging diagnostic context.
     * <p>
     * 
     * @param key
     */
    protected void loggingCtxRemove(String key) {
        mdcState.remove(key);
        MDC.remove(key);
    }

    /**
     * Get the value associated with key in the current logging diagnostic context.
     * <p>
     * 
     * @param key
     * @return the value associated with key
     */
    protected String loggingCtxGet(String key) {
        return mdcState.get(key);
    }

    /**
     * Reset the current logging diagnostic context.
     * <p>
     * Removes all items added to context by the user that weren't set with {@link #loggingCtxPutAlways(String, String)}
     * <p>
     */
    protected void loggingCtxReset() {
        String state = MDC.get(MDC_KEY_CSTATE);
        mdcState.clear();
        mdcState.putAll(mdcReset);
        MDC.setContextMap(mdcState);
        if (state != null) {
            MDC.put(MDC_KEY_CSTATE, state);
        }
    }

    private void loggingCtxInit() {
        // if (!mdcState.isEmpty()) {
        // mdcReset.putAll(mdcState);
        // }
        mdcReset.put(MDC_KEY_CID, this.id().toString());
        loggingCtxReset();
    }

    /* === PRIVATE === */
    private ComponentCore core;

    /**
     * Instantiates a new component definition.
     */
    protected ComponentDefinition() {
        core = new JavaComponent(this);
        control = core.createControlPort();
        loopback = core.createNegativePort(LoopbackPort.class);
        onSelf = loopback.getPair();
        loggingCtxInit();
    }

    @SuppressWarnings("rawtypes")
    protected ComponentDefinition(Class<? extends ComponentCore> coreClass) {
        try {
            Constructor[] constrs = coreClass.getConstructors();
            for (Constructor constr : constrs) {
                Class[] paramTypes = constr.getParameterTypes();
                if ((paramTypes.length) == 1 && paramTypes[0].isInstance(this)) {
                    core = (ComponentCore) constr.newInstance(this);
                }
            }
            if (core == null) {
                core = coreClass.newInstance();
            }
        } catch (Exception e) {
            // e.printStackTrace();
            // System.out.println(e + ": " + e.getMessage());
            throw new ConfigurationException(e);
        }
        control = core.createControlPort();
        loopback = core.createNegativePort(LoopbackPort.class);
        onSelf = loopback.getPair();
        loggingCtxInit();
    }
}
