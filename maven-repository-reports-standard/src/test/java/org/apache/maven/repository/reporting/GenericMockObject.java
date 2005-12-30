package org.apache.maven.repository.reporting;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Edwin Punzalan
 */
public class GenericMockObject
    implements InvocationHandler
{
    Map invocations = new HashMap();

    public GenericMockObject()
    {
        //default constructor
    }

    public GenericMockObject( Map returnMap )
    {
        invocations = returnMap;
    }

    public void setExpectedReturns( Method method, List returnList )
    {
        invocations.put( method, returnList );
    }

    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        if ( !invocations.containsKey( method ) )
        {
            throw new UnsupportedOperationException( "No expected return values defined." );
        }

        List returnList = (List) invocations.get( method );
        if ( returnList.size() < 1 )
        {
            throw new UnsupportedOperationException( "Too few expected return values defined." );
        }
        return returnList.remove( 0 );
    }
}
