package org.apache.maven.archiva.proxy;

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.wagon.Wagon;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Olivier Lamy
 *         * @since 1.4
 */
@Service( "wagonFactory" )
public class DefaultWagonFactory
    implements WagonFactory
{

    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    public DefaultWagonFactory( PlexusSisuBridge plexusSisuBridge )
    {
        this.plexusSisuBridge = plexusSisuBridge;
    }

    public Wagon getWagon( String protocol )
        throws WagonFactoryException
    {
        try
        {
            // with sisu inject bridge hint is file or http
            // so remove wagon#
            protocol = StringUtils.remove( protocol, "wagon#" );
            return plexusSisuBridge.lookup( Wagon.class, protocol );
        }
        catch ( PlexusSisuBridgeException e )
        {
            throw new WagonFactoryException( e.getMessage(), e );
        }
    }
}
