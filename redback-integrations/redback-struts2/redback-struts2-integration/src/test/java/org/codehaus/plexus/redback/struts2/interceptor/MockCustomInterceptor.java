package org.codehaus.plexus.redback.struts2.interceptor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.Interceptor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author <a href='mailto:rahul.thakur.xdev@gmail.com'>Rahul Thakur</a>
 * @version $Id$
 */
@Service("testCustomInterceptor")
public class MockCustomInterceptor
    implements Interceptor
{
    /**
     *
     */
    @Inject
    private MockComponent testComponent;

    public MockCustomInterceptor()
    {
    }

    public MockCustomInterceptor( MockComponent testComponent )
    {
        this.testComponent = testComponent;
    }

    /* (non-Javadoc)
     * @see com.opensymphony.xwork2.interceptor.Interceptor#destroy()
     */
    public void destroy()
    {
        // do nothing
    }

    /* (non-Javadoc)
     * @see com.opensymphony.xwork2.interceptor.Interceptor#init()
     */
    public void init()
    {
        // do nothing
    }

    /**
     * @noinspection ProhibitedExceptionDeclared
     */
    public String intercept( ActionInvocation invocation )
        throws Exception
    {
        String result = "Hello Custom Interceptor";

        testComponent.displayResult( result );

        return result;
    }

    public MockComponent getTestComponent()
    {
        return testComponent;
    }

    // Introduce a Composition Exception , see PLX - 278 
    //    public void setTestComponent( MockComponent testComponent )
    //    {
    //        this.testComponent = testComponent;
    //    }

}
