package org.apache.archiva.event.context;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.event.EventContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This context provides repository data.
 *
 * @author Martin Schreier <martin_s@apache.org>
 */
public class RepositoryContext implements EventContext, Serializable
{
    private static final long serialVersionUID = -4172663291198878307L;

    private static final String ID = "repository";

    private final String repositoryId;
    private final String type;
    private final String flavour;

    public RepositoryContext( String repositoryId, String type, String flavour )
    {
        this.repositoryId = repositoryId;
        this.type = type;
        this.flavour = flavour;
    }

    /**
     * Returns the repository id
     * @return the repository id
     */
    public String getRepositoryId( )
    {
        return repositoryId;
    }

    /**
     * Returns the repository type (e.g. MAVEN)
     * @return the string representation of the repository type
     */
    public String getType( )
    {
        return type;
    }

    /**
     * Returns the repository flavour (e.g. Remote, Managed, Group)
     * @return
     */
    public String getFlavour( )
    {
        return flavour;
    }

    @Override
    public Map<String, String> getData( )
    {
        Map<String, String> values = new HashMap<>( );
        values.put( ID +".repositoryId", repositoryId );
        values.put( ID +".type", type );
        values.put( ID +".flavour", flavour );
        return values;
    }

    @Override
    public String getId( )
    {
        return ID;
    }
}
