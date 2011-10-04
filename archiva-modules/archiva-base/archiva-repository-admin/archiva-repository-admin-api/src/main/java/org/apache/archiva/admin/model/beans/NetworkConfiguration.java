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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@XmlRootElement( name = "networkConfiguration" )
public class NetworkConfiguration
    implements Serializable
{
    /**
     * maximum total external http connections.
     */
    private int maxTotal = 30;

    /**
     * maximum total external http connections per host.
     */
    private int maxTotalPerHost = 30;

    private boolean usePooling = true;

    public NetworkConfiguration()
    {
        // no op
    }

    public NetworkConfiguration( int maxTotal, int maxTotalPerHost, boolean usePooling )
    {
        this.maxTotal = maxTotal;
        this.maxTotalPerHost = maxTotalPerHost;
        this.usePooling = usePooling;
    }

    public int getMaxTotal()
    {
        return maxTotal;
    }

    public void setMaxTotal( int maxTotal )
    {
        this.maxTotal = maxTotal;
    }

    public int getMaxTotalPerHost()
    {
        return maxTotalPerHost;
    }

    public void setMaxTotalPerHost( int maxTotalPerHost )
    {
        this.maxTotalPerHost = maxTotalPerHost;
    }

    public boolean isUsePooling()
    {
        return usePooling;
    }

    public void setUsePooling( boolean usePooling )
    {
        this.usePooling = usePooling;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "NetworkConfiguration" );
        sb.append( "{maxTotal=" ).append( maxTotal );
        sb.append( ", maxTotalPerHost=" ).append( maxTotalPerHost );
        sb.append( ", usePooling=" ).append( usePooling );
        sb.append( '}' );
        return sb.toString();
    }
}
