package org.apache.archiva.event;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

public class AsynchronousEventBusTest extends TestCase
{
    public void testSubscribeUnsubscribe() throws Exception
    {
        AsynchronousEventBus bus = new AsynchronousEventBus(1);
        MockObserver observer = new MockObserver();

        assertEquals(0, bus.getObservers().size());

        bus.subscribe(observer);
        assertTrue(bus.getObservers().contains(observer));

        bus.unsubscribe(observer);
        assertFalse(bus.getObservers().contains(bus));
    }

    public void testAllEventsAreObserved() throws Exception
    {
        AsynchronousEventBus bus = new AsynchronousEventBus(1);
        MockObserver observer = new MockObserver();
        bus.subscribe(observer);
        
        for (int i = 0; i < 10; i++)
        {
            bus.emit(new EventEmitter() {}, new EventMessage() {});
        }

        while (observer.observedEvents.size() != 10)
        {
        }

        assertEquals(10, observer.observedEvents.size());
    }

    class MockObserver implements EventObserver
    {
        final List<Event> observedEvents = Collections.synchronizedList(new ArrayList());
        
        public void observe(Event event)
        {
            observedEvents.add(event);
        }
    }
}
