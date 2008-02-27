package org.codehaus.plexus.spring;

import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
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
    implements PlexusBean, Initializable, Disposable, Contextualizable
{
    private String message;

    private String state = "undefined";

    /**
     * @plexus.requirement
     */
    private SpringBean bean;

    private Context context;

    public void initialize()
        throws InitializationException
    {
        state = INITIALIZED;
    }

    public void dispose()
    {
        state = DISPOSED;
    }

    public String describe()
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

    /**
     * {@inheritDoc}
     * @see org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable#contextualize(org.codehaus.plexus.context.Context)
     */
    public void contextualize( Context context )
        throws ContextException
    {
        this.context = context;
    }

    public Context getContext()
    {
        return context;
    }

    public Logger getLogger()
    {
        return super.getLogger();
    }
}
