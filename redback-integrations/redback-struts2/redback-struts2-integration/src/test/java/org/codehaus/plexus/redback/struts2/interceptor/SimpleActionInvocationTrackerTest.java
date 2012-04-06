package org.codehaus.plexus.redback.struts2.interceptor;

/*
 * Copyright 2006-2007 The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;
import org.codehaus.plexus.redback.struts2.ActionContextStub;
import org.codehaus.plexus.redback.struts2.ActionInvocationStub;
import org.codehaus.plexus.redback.struts2.ActionProxyStub;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class SimpleActionInvocationTrackerTest
    extends TestCase
{
    private static final int HISTORY_SIZE = 2;

    private ActionInvocationTracker tracker;

    
    

    protected String getPlexusConfigLocation()
    {
        return "plexus.xml";
    }

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
        tracker = new SimpleActionInvocationTracker();
    }

    @Test
    public void testAddActionInvocation()
        throws Exception
    {
        tracker.setHistorySize( HISTORY_SIZE );

        tracker.addActionInvocation( new ActionInvocationStub() );
        assertEquals( 1, tracker.getHistoryCount() );

        // first entry int the stack
        SavedActionInvocation actionInvocation = tracker.getActionInvocationAt( 0 );
        Map<String,Object> parametersMap = actionInvocation.getParametersMap();

        assertEquals( ActionProxyStub.ACTION_NAME, actionInvocation.getActionName() );
        assertEquals( ActionProxyStub.METHOD, actionInvocation.getMethodName() );
        assertEquals( ActionContextStub.VALUE_1, parametersMap.get( ActionContextStub.PARAMETER_1 ) );
        assertEquals( ActionContextStub.VALUE_2, parametersMap.get( ActionContextStub.PARAMETER_2 ) );
        assertEquals( ActionContextStub.VALUE_3, parametersMap.get( ActionContextStub.PARAMETER_3 ) );

        ActionInvocationStub actionInvocationStub = new ActionInvocationStub();

        ActionProxyStub proxyStub = (ActionProxyStub) actionInvocationStub.getProxy();
        proxyStub.setActionName( "new_action" );
        proxyStub.setMethod( "new_method" );

        ActionContextStub actionContextStub = (ActionContextStub) actionInvocationStub.getInvocationContext();
        actionContextStub.getParameters().put( "new_parameter", "new_value" );

        tracker.addActionInvocation( actionInvocationStub );
        assertEquals( tracker.getHistoryCount(), HISTORY_SIZE );

        // second entry in the stack
        actionInvocation = tracker.getActionInvocationAt( 1 );
        parametersMap = actionInvocation.getParametersMap();

        assertEquals( "new_action", actionInvocation.getActionName() );
        assertEquals( "new_method", actionInvocation.getMethodName() );
        assertEquals( ActionContextStub.VALUE_1, parametersMap.get( ActionContextStub.PARAMETER_1 ) );
        assertEquals( ActionContextStub.VALUE_2, parametersMap.get( ActionContextStub.PARAMETER_2 ) );
        assertEquals( ActionContextStub.VALUE_3, parametersMap.get( ActionContextStub.PARAMETER_3 ) );
        assertEquals( "new_value", parametersMap.get( "new_parameter" ) );

        // first entry int the stack
        actionInvocation = tracker.getActionInvocationAt( 0 );
        parametersMap = actionInvocation.getParametersMap();

        assertEquals( ActionProxyStub.ACTION_NAME, actionInvocation.getActionName() );
        assertEquals( ActionProxyStub.METHOD, actionInvocation.getMethodName() );
        assertEquals( ActionContextStub.VALUE_1, parametersMap.get( ActionContextStub.PARAMETER_1 ) );
        assertEquals( ActionContextStub.VALUE_2, parametersMap.get( ActionContextStub.PARAMETER_2 ) );
        assertEquals( ActionContextStub.VALUE_3, parametersMap.get( ActionContextStub.PARAMETER_3 ) );
    }

    @Test
    public void testHistoryCounter()
        throws Exception
    {
        tracker.setHistorySize( HISTORY_SIZE );
        tracker.addActionInvocation( new ActionInvocationStub() );
        assertEquals( 1, tracker.getHistoryCount() );

        tracker.setHistorySize( HISTORY_SIZE );
        tracker.addActionInvocation( new ActionInvocationStub() );
        assertEquals( HISTORY_SIZE, tracker.getHistoryCount() );

        tracker.addActionInvocation( new ActionInvocationStub() );
        tracker.addActionInvocation( new ActionInvocationStub() );
        tracker.addActionInvocation( new ActionInvocationStub() );
        assertEquals( HISTORY_SIZE, tracker.getHistoryCount() );

        tracker.addActionInvocation( new ActionInvocationStub() );
        tracker.addActionInvocation( new ActionInvocationStub() );
        tracker.addActionInvocation( new ActionInvocationStub() );
        assertEquals( HISTORY_SIZE, tracker.getHistoryCount() );
    }

}
