package org.apache.archiva.rest.services;

import org.apache.archiva.rest.api.services.PingService;
import org.springframework.stereotype.Service;

/**
 * @author Olivier Lamy
 * @since TODO
 */
@Service( "pingService#rest" )
public class DefaultPingService
    implements PingService
{
    public String ping()
    {
        return "Yeah Baby It rocks!";
    }
}
