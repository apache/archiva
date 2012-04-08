package org.apache.archiva.redback.struts2.interceptor;

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

import org.springframework.stereotype.Service;

/**
 * @author <a href='mailto:rahul.thakur.xdev@gmail.com'>Rahul Thakur</a>
 * @version $Id: MockComponentImpl.java 1310448 2012-04-06 16:23:16Z olamy $
 */
@Service
public class MockComponentImpl
    implements MockComponent
{
    private String result;

    /* (non-Javadoc)
    * @see org.codehaus.plexus.xwork.interceptor.TestComponent#execute()
    */
    public void displayResult( String result )
    {
        this.result = result;
    }

    public String getResult()
    {
        return result;
    }
}
