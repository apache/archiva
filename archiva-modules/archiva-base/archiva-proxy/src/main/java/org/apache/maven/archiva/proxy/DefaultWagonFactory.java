package org.apache.maven.archiva.proxy;

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

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.wagon.Wagon;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "wagonFactory" )
public class DefaultWagonFactory
    implements WagonFactory
{

    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    public DefaultWagonFactory( PlexusSisuBridge plexusSisuBridge )
    {
        this.plexusSisuBridge = plexusSisuBridge;
    }

    public Wagon getWagon( String protocol )
        throws WagonFactoryException
    {
        try
        {
            // with sisu inject bridge hint is file or http
            // so remove wagon#
            protocol = StringUtils.remove( protocol, "wagon#" );
            return plexusSisuBridge.lookup( Wagon.class, protocol );
        }
        catch ( PlexusSisuBridgeException e )
        {
            throw new WagonFactoryException( e.getMessage(), e );
        }
    }
}
