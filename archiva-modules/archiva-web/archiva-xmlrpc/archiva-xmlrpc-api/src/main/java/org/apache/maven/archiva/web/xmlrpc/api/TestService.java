package org.apache.maven.archiva.web.xmlrpc.api;

import com.atlassian.xmlrpc.ServiceObject;

@ServiceObject("Test")
public interface TestService
{
    public String ping();
}
