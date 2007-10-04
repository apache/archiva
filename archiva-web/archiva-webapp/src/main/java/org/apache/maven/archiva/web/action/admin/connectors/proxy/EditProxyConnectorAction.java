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
 * EditProxyConnectorAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="editProxyConnectorAction"
 */
public class EditProxyConnectorAction
    extends AbstractProxyConnectorFormAction
{
    /**
     * The proxy connector source id to edit. (used with {@link #target})
     */
    private String source;

    /**
     * The proxy connector target id to edit. (used with {@link #source})
     */
    private String target;

    @Override
    public void prepare()
    {
        super.prepare();

        connector = findProxyConnector( source, target );
    }

    public String input()
    {
        if ( connector == null )
        {
            addActionError( "Unable to edit non existant proxy connector with source [" + source + "] and target ["
                + target + "]" );
            return ERROR;
        }

        return INPUT;
    }

    public String commit()
    {
        validateConnector();

        if ( hasActionErrors() )
        {
            return INPUT;
        }

        String sourceId = connector.getSourceRepoId();
        String targetId = connector.getTargetRepoId();

        ProxyConnectorConfiguration otherConnector = findProxyConnector( sourceId, targetId );
        if ( otherConnector != null )
        {
            // Remove the previous connector.
            removeProxyConnector( otherConnector );
        }

        if ( hasActionErrors() )
        {
            return INPUT;
        }

        addProxyConnector( connector );
        return saveConfiguration();
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
