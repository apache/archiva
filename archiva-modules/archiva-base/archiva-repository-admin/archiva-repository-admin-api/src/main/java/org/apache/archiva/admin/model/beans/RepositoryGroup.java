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
import java.util.ArrayList;
import java.util.List;

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@XmlRootElement(name = "repositoryGroup")
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

    /**
     * The path of the merged index.
     */
    private String mergedIndexPath = DEFAULT_INDEX_PATH;

    /**
     * The TTL (time to live) of the repo group's merged index.
     */
    private int mergedIndexTtl = 30;

    /**
     * default model value is empty so none
     * @since 2.0.0
     */
    private String cronExpression;

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
    public List<String> getRepositories()
    {
        if ( this.repositories == null )
        {
            this.repositories = new ArrayList<>( 0 );
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

    public String getMergedIndexPath()
    {
        return mergedIndexPath;
    }

    public void setMergedIndexPath( String mergedIndexPath )
    {
        this.mergedIndexPath = mergedIndexPath;
    }

    public int getMergedIndexTtl() {
        return mergedIndexTtl;
    }

    /**
     * Set the TTL of the repo group's merged index.
     *
     * @param mergedIndexTtl
     */
    public void setMergedIndexTtl(int mergedIndexTtl) {
        this.mergedIndexTtl = mergedIndexTtl;
    }

    public RepositoryGroup mergedIndexPath( String mergedIndexPath ) {
        this.mergedIndexPath = mergedIndexPath;
        return this;
    }

    public RepositoryGroup mergedIndexTtl( int mergedIndexTtl ) {
        this.mergedIndexTtl = mergedIndexTtl;
        return this;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    public RepositoryGroup cronExpression( String mergedIndexCronExpression )
    {
        this.cronExpression = mergedIndexCronExpression;
        return this;
    }

    @Override
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof RepositoryGroup ) )
        {
            return false;
        }

        RepositoryGroup that = (RepositoryGroup) other;
        boolean result = true;
        result = result && ( getId() == null ? that.getId() == null : getId().equals( that.getId() ) );
        return result;
    }

    @Override
    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( id != null ? id.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder( "RepositoryGroup{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", repositories=" ).append( repositories );
        sb.append( ", mergedIndexPath='" ).append( mergedIndexPath ).append( '\'' );
        sb.append( ", mergedIndexTtl=" ).append( mergedIndexTtl );
        sb.append( ", cronExpression='" ).append( cronExpression ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
