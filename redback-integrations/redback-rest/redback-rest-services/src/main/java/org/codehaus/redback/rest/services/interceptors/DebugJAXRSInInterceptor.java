package org.codehaus.redback.rest.services.interceptors;

import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.message.Message;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
public class DebugJAXRSInInterceptor extends JAXRSInInterceptor
{
    @Override
    public void handleMessage( Message message )
    {
        super.handleMessage( message );
    }
}
