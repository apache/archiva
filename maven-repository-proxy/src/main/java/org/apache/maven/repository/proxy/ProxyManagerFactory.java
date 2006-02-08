package org.apache.maven.repository.proxy;

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

import org.apache.maven.repository.proxy.configuration.ProxyConfiguration;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Edwin Punzalan
 *
 * @plexus.component role="org.apache.maven.repository.proxy.ProxyManagerFactory"
 */
public class ProxyManagerFactory
    implements Contextualizable
{
    public static String ROLE = "org.apache.maven.repository.proxy.ProxyManagerFactory";

    private PlexusContainer container;

    public ProxyManager getProxyManager( String proxy_type, ProxyConfiguration config )
        throws ComponentLookupException
    {
        ProxyManager proxy = (ProxyManager) container.lookup( ProxyManager.ROLE, proxy_type );
        proxy.setConfiguration( config );
        return proxy;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
