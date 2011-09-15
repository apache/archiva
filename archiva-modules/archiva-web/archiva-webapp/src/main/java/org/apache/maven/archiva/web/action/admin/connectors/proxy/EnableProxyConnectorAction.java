package org.apache.maven.archiva.web.action.admin.connectors.proxy;

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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

/**
 * EnableProxyConnectorAction
 */
@Controller( "enableProxyConnectorAction" )
@Scope( "prototype" )
public class EnableProxyConnectorAction
    extends AbstractProxyConnectorAction
{
    private String source;

    private String target;

    private ProxyConnector proxyConfig;

    public String confirmEnable()
        throws RepositoryAdminException
    {
        this.proxyConfig = findProxyConnector( source, target );

        // Not set? Then there is nothing to delete.
        if ( this.proxyConfig == null )
        {
            addActionError(
                "Unable to enable proxy configuration, configuration with source [" + source + "], and target ["
                    + target + "] does not exist." );
            return ERROR;
        }

        return INPUT;
    }

    public String enable() throws RepositoryAdminException
    {
        this.proxyConfig = findProxyConnector( source, target );

        // Not set? Then there is nothing to delete.
        if ( this.proxyConfig == null )
        {
            addActionError(
                "Unable to enabled proxy configuration, configuration with source [" + source + "], and target ["
                    + target + "] does not exist." );
            return ERROR;
        }

        if ( hasActionErrors() )
        {
            return ERROR;
        }

        proxyConfig.setDisabled( false );

        getProxyConnectorAdmin().updateProxyConnector( proxyConfig, getAuditInformation() );

        addActionMessage( "Successfully enabled proxy connector [" + source + " , " + target + " ]" );

        setSource( null );
        setTarget( null );

        return SUCCESS;
    }

    public String getSource()
    {
        return source;
    }

    public void setSource( String source )
    {
        this.source = source;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget( String target )
    {
        this.target = target;
    }
}
