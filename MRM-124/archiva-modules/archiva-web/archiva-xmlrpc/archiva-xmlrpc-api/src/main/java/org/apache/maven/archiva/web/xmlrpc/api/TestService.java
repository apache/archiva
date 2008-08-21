package org.apache.maven.archiva.web.xmlrpc.api;

import com.atlassian.xmlrpc.ServiceObject;

@ServiceObject(objectName="Test")
public interface TestService
{
    public String ping();
}
