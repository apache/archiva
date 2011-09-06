package org.apache.archiva.admin.repository.group;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public class RepositoryGroup
    implements Serializable
{
    /**
     * repository group Id
     */
    private String id;

    /**
     * repositories ids
     */
    private List<String> repositories;

    public RepositoryGroup()
    {
        // no op
    }

    public RepositoryGroup( String id, List<String> repositories )
    {
        this.id = id;
        this.repositories = repositories;
    }

    /**
     * Method addRepository.
     *
     * @param string
     */
    public void addRepository( String string )
    {
        getRepositories().add( string );
    }

    /**
     * Get the id of the repository group.
     *
     * @return String
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Method getRepositories.
     *
     * @return List
     */
    public java.util.List<String> getRepositories()
    {
        if ( this.repositories == null )
        {
            this.repositories = new ArrayList<String>();
        }

        return this.repositories;
    }

    /**
     * Method removeRepository.
     *
     * @param string
     */
    public void removeRepository( String string )
    {
        getRepositories().remove( string );
    }

    /**
     * Set the id of the repository group.
     *
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    }

    /**
     * Set the list of repository ids under the group.
     *
     * @param repositories
     */
    public void setRepositories( List<String> repositories )
    {
        this.repositories = repositories;
    }
}
