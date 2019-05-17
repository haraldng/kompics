/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class StartStopTest {

    private static BlockingQueue<KompicsEvent> queue;

    @Test
    public void testStartStop() {
        queue = new LinkedBlockingQueue<KompicsEvent>();
        Kompics.createAndStart(RootComponent.class, 4, 10); // totally arbitrary values
        try {
            KompicsEvent e = queue.take();
            assertTrue(e instanceof TestReply, "First run started.");
            e = queue.take();
            assertTrue(e instanceof Stopped, "First run stopped.");
            e = queue.take();
            assertTrue(e instanceof TestReply, "Second run started.");
            e = queue.take();
            assertTrue(e instanceof Stopped, "Second run stopped.");
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }

        Kompics.shutdown();

        Kompics.createAndStart(RootComponent.class, 4, 10); // totally arbitrary values
        try {
            KompicsEvent e = queue.take();
            assertTrue(e instanceof TestReply, "Third run started.");
            e = queue.take();
            assertTrue(e instanceof Stopped, "Third run stopped.");
            e = queue.take();
            assertTrue(e instanceof TestReply, "Fourth run started.");
            e = queue.take();
            assertTrue(e instanceof Stopped, "Fourth run stopped.");
        } catch (InterruptedException ex) {
            fail(ex.getMessage());
        }

        Kompics.shutdown();

    }

    public static class TestPort extends PortType {

        {
            request(TestRequest.class);
            indication(TestReply.class);
        }
    }

    public static class TestRequest implements KompicsEvent {
    }

    public static class TestReply implements KompicsEvent {
    }

    public static class RootComponent extends ComponentDefinition {

        private boolean first = true;
        private Channel<TestPort> testChannel = null;

        {
            final Positive<TestPort> port = requires(TestPort.class);

            final Component child = create(ForwarderComponent.class, Init.NONE);
            final Component child2 = create(ForwarderComponent.class, Init.NONE);

            testChannel = connect(port.getPair(), child.getPositive(TestPort.class), Channel.TWO_WAY);

            Handler<TestReply> replyHandler = new Handler<TestReply>() {
                @Override
                public void handle(TestReply event) {
                    try {
                        queue.put(event);
                        if (first) {
                            trigger(Stop.event, child.control());
                        } else {
                            trigger(Stop.event, child2.control());
                        }
                    } catch (InterruptedException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            };

            Handler<Start> startHandler = new Handler<Start>() {

                @Override
                public void handle(Start event) {
                    trigger(new TestRequest(), port);
                }
            };
            Handler<Stopped> stoppedHandler = new Handler<Stopped>() {
                @Override
                public void handle(Stopped event) {
                    try {
                        if (first) {
                            first = false;
                            testChannel.disconnect();
                            // disconnect(port.getPair(), child.getPositive(TestPort.class));
                            destroy(child);
                            testChannel = connect(port.getPair(), child2.getPositive(TestPort.class), Channel.TWO_WAY);
                            trigger(new TestRequest(), port);
                        } else {
                            testChannel.disconnect();
                            // disconnect(port.getPair(), child2.getPositive(TestPort.class));
                            destroy(child2);
                        }
                        queue.put(event);
                    } catch (InterruptedException ex) {
                        System.err.println(ex.getMessage());
                    }
                }
            };

            subscribe(replyHandler, port);
            subscribe(startHandler, control);
            subscribe(stoppedHandler, control);
        }
    }

    public static class ForwarderComponent extends ComponentDefinition {

        {
            final Positive<TestPort> down = requires(TestPort.class);
            final Negative<TestPort> up = provides(TestPort.class);

            Component child = create(ResponderComponent.class, Init.NONE);

            connect(down.getPair(), child.getPositive(TestPort.class), Channel.TWO_WAY);

            Handler<TestReply> replyHandler = new Handler<TestReply>() {
                @Override
                public void handle(TestReply event) {
                    trigger(event, up);
                }
            };

            subscribe(replyHandler, down);

            Handler<TestRequest> requestHandler = new Handler<TestRequest>() {
                @Override
                public void handle(TestRequest event) {
                    trigger(event, down);
                }
            };

            subscribe(requestHandler, up);
        }
    }

    public static class ResponderComponent extends ComponentDefinition {

        {
            final Negative<TestPort> port = provides(TestPort.class);

            Handler<TestRequest> requestHandler = new Handler<TestRequest>() {
                @Override
                public void handle(TestRequest event) {
                    trigger(new TestReply(), port);
                }
            };

            subscribe(requestHandler, port);
        }
    }
}
