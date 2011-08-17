package org.apache.archiva.rest.services;

import org.apache.archiva.rest.api.services.PingService;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.codehaus.redback.rest.services.AbstractRestServicesTest;
import org.codehaus.redback.rest.services.FakeCreateAdminService;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Olivier Lamy
 * @since TODO
 */
public class PingServiceTest
    extends AbstractRestServicesTest
{

    @Before
    public void setUp()
        throws Exception
    {
        super.startServer();

        FakeCreateAdminService fakeCreateAdminService =
            JAXRSClientFactory.create( "http://localhost:" + port + "/services/fakeCreateAdminService/",
                                       FakeCreateAdminService.class );

        Boolean res = fakeCreateAdminService.createAdminIfNeeded();
        assertTrue( res.booleanValue() );
    }

    PingService getPingService()
    {
        return JAXRSClientFactory.create( "http://localhost:" + port + "/services/archivaServices/",
                                          PingService.class );

    }


    @Test
    public void ping()
        throws Exception
    {
        // 1000000L
        //WebClient.getConfig( userService ).getHttpConduit().getClient().setReceiveTimeout(3000);

        String res = getPingService().ping();
        assertEquals( "Yeah Baby It rocks!", res );
    }
}
