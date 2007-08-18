package org.apache.maven.archiva.web.interceptor;

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

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.ActionProxy;
import com.opensymphony.xwork.interceptor.Interceptor;
import com.opensymphony.xwork.interceptor.PreResultListener;

import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * WebworkIsDoingStrangeThingsInterceptor 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork.interceptor.Interceptor"
 *                   role-hint="webwork-is-doing-strange-things"
 */
public class WebworkIsDoingStrangeThingsInterceptor
    extends AbstractLogEnabled
    implements Interceptor, PreResultListener
{
    private String hint = "(nohint)";

    public void init()
    {
        getLogger().info( ".init()" );
    }

    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        StringBuffer dbg = new StringBuffer();

        invocation.addPreResultListener( this );

        dbg.append( "[" ).append( hint ).append( "] " );
        dbg.append( ".intercept(" ).append( invocation.getClass().getName() ).append( ")" );
        dbg.append( "\n Action=" ).append( invocation.getAction().getClass().getName() );

        ActionProxy proxy = invocation.getProxy();
        dbg.append( "\n Proxy=" ).append( proxy.getClass().getName() );
        dbg.append( "\n    .namespace  =" ).append( proxy.getNamespace() );
        dbg.append( "\n    .actionName =" ).append( proxy.getActionName() );
        dbg.append( "\n    .method     =" ).append( proxy.getMethod() );
        dbg.append( "\n    .execute result =" ).append( proxy.getExecuteResult() );

        ActionContext context = invocation.getInvocationContext();
        dbg.append( "\n InvocationContext=" ).append( context.getClass().getName() );
        appendMap( "\n    .session=", dbg, context.getSession() );
        appendMap( "\n    .parameters=", dbg, context.getParameters() );

        String result = invocation.invoke();

        dbg.append( "\n ... result=\"" ).append( result ).append( "\"" );
        dbg.append( ", code=" ).append( invocation.getResultCode() );
        getLogger().info( dbg.toString() );
        return result;
    }

    private void appendMap( String heading, StringBuffer dbg, Map map )
    {
        dbg.append( heading );

        if ( map == null )
        {
            dbg.append( "<null>" );
            return;
        }

        if ( map.isEmpty() )
        {
            dbg.append( "<empty>" );
            return;
        }

        Iterator entries = map.entrySet().iterator();
        while ( entries.hasNext() )
        {
            Map.Entry entry = (Entry) entries.next();
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            dbg.append( "\n      [" ).append( key ).append( "]: " );
            if ( value == null )
            {
                dbg.append( "<null>" );
            }
            else
            {
                try
                {
                    dbg.append( value.toString() );
                }
                catch ( NullPointerException e )
                {
                    dbg.append( "<npe>" );
                }
            }
        }

    }

    public void destroy()
    {
        getLogger().info( ".destroy()" );
    }

    public void setHint( String hint )
    {
        this.hint = hint;
    }

    public void beforeResult( ActionInvocation invocation, String resultCode )
    {
        getLogger().info( "before result: invocation: " + invocation + ", resultCode: " + resultCode );
    }
}
