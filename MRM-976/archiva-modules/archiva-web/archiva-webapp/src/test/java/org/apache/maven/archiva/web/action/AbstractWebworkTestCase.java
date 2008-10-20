package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork2.ActionSupport;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * AbstractWebworkTestCase 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractWebworkTestCase
    extends PlexusInSpringTestCase
{
    /**
     * This is a conveinence method for mimicking how the webwork interceptors
     * operate on an action, before the request is processed.
     * 
     * Call this before each major request to the action to be sure you mimic the webwork process correctly. 
     */
    protected void preRequest( ActionSupport action )
    {
        action.clearErrorsAndMessages();
    }

    /**
     * Tests the action to ensure that it has errors.
     * 
     * NOTE: Don't forget to run {@link #preRequest(ActionSupport)} before each request to your action!
     */
    protected void assertHasErrors( ActionSupport action )
    {
        assertNotNull( action.getActionErrors() );
        assertTrue( "Expected an error to occur.", action.getActionErrors().size() > 0 );
    }
    
    /**
     * Tests the action to ensure that it has messages.
     * 
     * NOTE: Don't forget to run {@link #preRequest(ActionSupport)} before each request to your action!
     */
    protected void assertHasMessages( ActionSupport action )
    {
        assertNotNull( action.getActionMessages() );
        assertTrue( "Expected an message to be set.", action.getActionMessages().size() > 0 );
    }

    /**
     * Tests the action to ensure that it has NO errors.
     * 
     * NOTE: Don't forget to run {@link #preRequest(ActionSupport)} before each request to your action!
     */
    protected void assertNoErrors( ActionSupport action )
    {
        List<String> errors = (List<String>) action.getActionErrors();
    
        assertNotNull( errors );
        if ( errors.size() > 0 )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "Should have had no errors. but found the following errors." );
    
            for ( String error : errors )
            {
                msg.append( "\n " ).append( error );
            }
            fail( msg.toString() );
        }
    }

    protected void assertRequestStatus( ActionSupport action, String expectedStatus, String methodName )
        throws Exception
    {
        action.clearErrorsAndMessages();
    
        Method method = action.getClass().getDeclaredMethod( methodName, (Class[]) null );
        Object actualStatus = method.invoke( action, (Object[]) null );
        assertTrue( "return should be of type String", actualStatus instanceof String );
    
        if ( !StringUtils.equals( expectedStatus, (String) actualStatus ) )
        {
            StringBuffer msg = new StringBuffer();
            msg.append( "Unexpected status returned from method <" );
            msg.append( methodName ).append( "> on action <" );
            String clazzname = action.getClass().getName();
            msg.append( clazzname.substring( clazzname.lastIndexOf( '.' ) ) );
            msg.append( ">: expected:<" ).append( expectedStatus ).append( "> but was:<" );
            msg.append( (String) actualStatus ).append( ">. (see attached action messages and errors below)" );
    
            for ( String message : (Collection<String>) action.getActionMessages() )
            {
                msg.append( "\n  [MESSAGE]: " ).append( message );
            }
    
            for ( String error : (Collection<String>) action.getActionErrors() )
            {
                msg.append( "\n  [ERROR]: " ).append( error );
            }
    
            fail( msg.toString() );
        }
    }
}
