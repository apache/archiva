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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * Factory class for creating ProxyManager instances.  The usage of a factory ensures that the created instance will
 * have the necessary configuration
 *
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.proxy.ProxyManagerFactory"
 */
public class ProxyManagerFactory
    implements Contextualizable
{
    public static String ROLE = "org.apache.maven.repository.proxy.ProxyManagerFactory";

    private PlexusContainer container;

    /**
     * Used to create a ProxyManager instance of a certain type with a configuration to base its behavior
     *
     * @param proxy_type The ProxyManager repository type
     * @param config     The ProxyConfiguration to describe the behavior of the proxy instance
     * @return The ProxyManager instance of type proxy_type with ProxyConfiguration config
     * @throws ComponentLookupException when the factory fails to create the ProxyManager instance
     */
    public ProxyManager getProxyManager( String proxy_type, ProxyConfiguration config )
        throws ComponentLookupException
    {
        ProxyManager proxy = (ProxyManager) container.lookup( ProxyManager.ROLE );
        config.setLayout( proxy_type );
        proxy.setConfiguration( config );
        return proxy;
    }

    /**
     * @see org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable#contextualize(org.codehaus.plexus.context.Context)
     */
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
