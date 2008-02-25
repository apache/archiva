package org.codehaus.plexus.spring;

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

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

/**
 * A typical plexus component implementation
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public class PlexusBeanImpl
    extends AbstractLogEnabled
    implements PlexusBean, Initializable, Disposable
{
    private String message;

    private String state = "undefined";

    /**
     * @plexus.requirement
     */
    private SpringBean bean;

    public void initialize()
        throws InitializationException
    {
        state = INITIALIZED;
    }

    public void dispose()
    {
        state = DISPOSED;
    }

    /**
     * @see org.codehaus.plexus.spring.PlexusBean#toString()
     */
    public String toString()
    {
        getLogger().info( "Logger has been set" );
        return message + " " + bean.toString();
    }

    /**
     * @return the state
     */
    public String getState()
    {
        return state;
    }

}
