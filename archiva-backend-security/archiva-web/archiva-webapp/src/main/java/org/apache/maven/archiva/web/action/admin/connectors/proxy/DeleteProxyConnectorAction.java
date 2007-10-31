package org.apache.maven.archiva.web.action.admin.connectors.proxy;

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

import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;

/**
 * DeleteProxyConnectorAction 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="deleteProxyConnectorAction"
 */
public class DeleteProxyConnectorAction
    extends AbstractProxyConnectorAction
{
    private String sourceId;

    private String targetId;

    private ProxyConnectorConfiguration proxyConfig;

    public String confirmDelete()
    {
        this.proxyConfig = findProxyConnector( sourceId, targetId );

        // Not set? Then there is nothing to delete.
        if ( this.proxyConfig == null )
        {
            addActionError( "Unable to delete proxy configuration, configuration with source [" + sourceId
                + "], and target [" + targetId + "] does not exist." );
            return ERROR;
        }

        return INPUT;
    }

    public String delete()
    {
        this.proxyConfig = findProxyConnector( sourceId, targetId );

        // Not set? Then there is nothing to delete.
        if ( this.proxyConfig == null )
        {
            addActionError( "Unable to delete proxy configuration, configuration with source [" + sourceId
                + "], and target [" + targetId + "] does not exist." );
            return ERROR;
        }

        if ( hasActionErrors() )
        {
            return ERROR;
        }
        
        removeProxyConnector( proxyConfig );
        addActionMessage( "Successfully removed proxy connector [" + sourceId + " , " + targetId + " ]" );

        setSourceId( null );
        setTargetId( null );
        
        return saveConfiguration();
    }

    public String getSourceId()
    {
        return sourceId;
    }

    public void setSourceId( String sourceId )
    {
        this.sourceId = sourceId;
    }

    public String getTargetId()
    {
        return targetId;
    }

    public void setTargetId( String targetId )
    {
        this.targetId = targetId;
    }

    public ProxyConnectorConfiguration getProxyConfig()
    {
        return proxyConfig;
    }
}
