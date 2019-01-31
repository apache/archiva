package org.apache.archiva.admin.model.beans;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.proxy.model.ProxyConnectorRuleType;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M3
 */
@XmlRootElement ( name = "proxyConnectorRule" )
public class ProxyConnectorRule
    implements Serializable
{
    private String pattern;

    //FIXME: olamy possible tru rest ? or a String
    private ProxyConnectorRuleType proxyConnectorRuleType;

    private List<ProxyConnector> proxyConnectors;

    public ProxyConnectorRule()
    {
        // no op
    }

    public ProxyConnectorRule( String pattern, ProxyConnectorRuleType proxyConnectorRuleType,
                               List<ProxyConnector> proxyConnectors )
    {
        this.pattern = pattern;
        this.proxyConnectorRuleType = proxyConnectorRuleType;
        this.proxyConnectors = proxyConnectors;
    }

    public String getPattern()
    {
        return pattern;
    }

    public void setPattern( String pattern )
    {
        this.pattern = pattern;
    }

    public ProxyConnectorRuleType getProxyConnectorRuleType()
    {
        return proxyConnectorRuleType;
    }

    public void setProxyConnectorRuleType( ProxyConnectorRuleType proxyConnectorRuleType )
    {
        this.proxyConnectorRuleType = proxyConnectorRuleType;
    }

    public List<ProxyConnector> getProxyConnectors()
    {
        if ( this.proxyConnectors == null )
        {
            this.proxyConnectors = new ArrayList<>();
        }
        return proxyConnectors;
    }

    public void setProxyConnectors( List<ProxyConnector> proxyConnectors )
    {
        this.proxyConnectors = proxyConnectors;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ProxyConnectorRule ) )
        {
            return false;
        }

        ProxyConnectorRule that = (ProxyConnectorRule) o;

        if ( !pattern.equals( that.pattern ) )
        {
            return false;
        }
        if ( proxyConnectorRuleType != that.proxyConnectorRuleType )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = pattern.hashCode();
        result = 31 * result + proxyConnectorRuleType.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ProxyConnectorRule" );
        sb.append( "{pattern='" ).append( pattern ).append( '\'' );
        sb.append( ", proxyConnectorRuleType=" ).append( proxyConnectorRuleType );
        sb.append( ", proxyConnectors=" ).append( proxyConnectors );
        sb.append( '}' );
        return sb.toString();
    }
}
