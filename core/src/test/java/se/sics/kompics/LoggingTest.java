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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
@RunWith(JUnit4.class)
public class LoggingTest {

    public LoggingTest() {
    }

    @Test
    public void testLoggerName() {
        try {
            Kompics.createAndStart(LoggingComponent.class);
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            //System.err.println(ex.getMessage());
            Kompics.shutdown();
            Assert.fail(ex.getMessage());
        }
    }

    static class LoggingComponent extends ComponentDefinition {

        
        public LoggingComponent() {
            subscribe(startHandler, control);
            
            loggingCtxPutAlways("customAlways", "testv");
        }
        
        protected final Handler<Start> startHandler = new Handler<Start>() {

            @Override
            public void handle(Start event) {
                loggingCtxPut("customInstance", "testv2");
                logger.info("test");
                loggingCtxReset();
                logger.info("test reset");
                Kompics.asyncShutdown();
            }

        };
    }
}
