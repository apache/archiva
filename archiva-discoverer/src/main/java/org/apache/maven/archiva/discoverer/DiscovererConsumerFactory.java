package org.apache.maven.archiva.discoverer;

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * DiscovererConsumerFactory - factory for consumers.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.discoverer.DiscovererConsumerFactory"
 */
public class DiscovererConsumerFactory
implements Contextualizable
{
    private PlexusContainer container;
    
    public DiscovererConsumer createConsumer( String name ) throws DiscovererException
    {
        DiscovererConsumer consumer;
        try
        {
            consumer = (DiscovererConsumer) container.lookup(DiscovererConsumer.ROLE, name);
        }
        catch ( ComponentLookupException e )
        {
            throw new DiscovererException("Unable to create consumer [" + name + "]: " + e.getMessage(), e);
        }
        
        return consumer;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
