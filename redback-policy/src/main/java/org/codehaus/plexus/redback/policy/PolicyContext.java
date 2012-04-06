package org.codehaus.plexus.redback.policy;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import java.util.HashMap;
import java.util.Map;

/**
 * PolicyContext - A Thread Local Context.
 * Useful for managing policy operations on a thread local point of view. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class PolicyContext
{
    static ThreadLocal<PolicyContext> policyContext = new PolicyContextThreadLocal();

    Map<Object, Object> context;

    public PolicyContext( Map<Object, Object> map )
    {
        context = map;
    }

    public static void setContext( PolicyContext context )
    {
        policyContext.set( context );
    }

    public static PolicyContext getContext()
    {
        PolicyContext ctx = (PolicyContext) policyContext.get();
        if ( ctx == null )
        {
            ctx = new PolicyContext( new HashMap<Object, Object>() );
            setContext( ctx );
        }

        return ctx;
    }

    public Object get( Object key )
    {
        return context.get( key );
    }

    public void put( Object key, Object value )
    {
        context.put( key, value );
    }

    private static class PolicyContextThreadLocal
        extends ThreadLocal<PolicyContext>
    {
        protected PolicyContext initialValue()
        {
            return new PolicyContext( new HashMap<Object, Object>() );
        }
    }
}
