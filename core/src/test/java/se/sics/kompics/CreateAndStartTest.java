/*
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
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

import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * The <code>CreateAndStartTest</code> class tests component creation and start.
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @version $Id$
 */
public class CreateAndStartTest {

    private static class TestRoot0 extends ComponentDefinition {
        @SuppressWarnings("unused")
        public TestRoot0() {
            root0Created = true;
            semaphore0.release();
        }
    }

    private static boolean root0Created;
    private static Semaphore semaphore0;

    /**
     * Tests the creation of the main component.
     * 
     * @throws Exception
     *             the exception
     */
    @Test
    public void testBootstrap() throws Exception {
        root0Created = false;
        semaphore0 = new Semaphore(0);

        Kompics.createAndStart(TestRoot0.class, 1);

        semaphore0.acquire();
        assertTrue(root0Created);
        Kompics.shutdown();
    }

    private static class TestRoot1 extends ComponentDefinition {
        @SuppressWarnings("unused")
        public TestRoot1() {
            root1Created = true;
            create(TestComponent1.class, Init.NONE);
        }
    }

    private static class TestComponent1 extends ComponentDefinition {
        @SuppressWarnings("unused")
        public TestComponent1() {
            comp1Created = true;
            semaphore1.release();
        }
    }

    private static boolean root1Created;
    private static boolean comp1Created;
    private static Semaphore semaphore1;

    /**
     * Test the creation of a subcomponent.
     * 
     * @throws Exception
     *             the exception
     */
    @Test
    public void testCreate() throws Exception {
        root1Created = false;
        comp1Created = false;
        semaphore1 = new Semaphore(0);

        Kompics.createAndStart(TestRoot1.class, 1);

        semaphore1.acquire();
        Thread.sleep(500);
        assertTrue(root1Created);
        assertTrue(comp1Created);
        Kompics.shutdown();
    }

    private static class TestRoot2 extends ComponentDefinition {
        @SuppressWarnings("unused")
        public TestRoot2() {
            root2Created = true;

            subscribe(startHandler, control);
        }

        Handler<Start> startHandler = new Handler<Start>() {
            public void handle(Start event) {
                root2Started = true;
                semaphore2.release();
            }
        };
    }

    private static boolean root2Created;
    private static boolean root2Started;
    private static Semaphore semaphore2;

    /**
     * Tests that the main component is automatically started.
     * 
     * @throws Exception
     *             the exception
     */
    @Test
    public void testStart() throws Exception {
        root2Created = false;
        root2Started = false;
        semaphore2 = new Semaphore(0);

        Kompics.createAndStart(TestRoot2.class, 1);

        semaphore2.acquire();
        assertTrue(root2Created);
        assertTrue(root2Started);
        Kompics.shutdown();
    }

    private static class TestRoot3 extends ComponentDefinition {
        @SuppressWarnings("unused")
        public TestRoot3() {
            root3Created = true;

            create(TestComponent3.class, Init.NONE);

            subscribe(startHandler, control);
        }

        Handler<Start> startHandler = new Handler<Start>() {
            public void handle(Start event) {
                root3Started = true;
                semaphore3.release();
            }
        };
    }

    private static class TestComponent3 extends ComponentDefinition {
        @SuppressWarnings("unused")
        public TestComponent3() {
            comp3Created = true;

            subscribe(startHandler, control);
        }

        Handler<Start> startHandler = new Handler<Start>() {
            public void handle(Start event) {
                comp3Started = true;
                semaphore3.release();
            }
        };
    }

    private static boolean root3Created;
    private static boolean root3Started;
    private static boolean comp3Created;
    private static boolean comp3Started;
    private static Semaphore semaphore3;

    /**
     * Tests that a child component of the main component is automatically started.
     * 
     * @throws Exception
     *             the exception
     */
    @Test
    public void testCreateAndStart() throws Exception {
        root3Created = false;
        root3Started = false;
        comp3Created = false;
        comp3Started = false;
        semaphore3 = new Semaphore(0);

        Kompics.createAndStart(TestRoot3.class, 1);

        semaphore3.acquire(2);
        assertTrue(root3Created);
        assertTrue(root3Started);
        assertTrue(comp3Created);
        assertTrue(comp3Started);
        Kompics.shutdown();
    }
}
