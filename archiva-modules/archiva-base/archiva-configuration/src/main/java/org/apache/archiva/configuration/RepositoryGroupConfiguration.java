package org.apache.archiva.configuration;

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
 * Class RepositoryGroupConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class RepositoryGroupConfiguration
    implements Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * The id of the repository group.
     */
    private String id;

    /**
     * The name of the repository group
     */
    private String name;

    /**
     *
     *             The repository type. Currently only MAVEN type
     * is known.
     *
     */
    private String type = "MAVEN";


    /**
     * The path of the merged index.
     */
    private String mergedIndexPath = ".indexer";

    /**
     * The time to live of the merged index of the repository group.
     */
    private int mergedIndexTtl = 30;

    /**
     * 
     *           When to run the index merging for this group.
     *
     */
    private String cronExpression = "";

    /**
     * Field repositories.
     */
    private List<String> repositories;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addRepository.
     * 
     * @param string
     */
    public void addRepository( String string )
    {
        getRepositories().add( string );
    } //-- void addRepository( String )

    /**
     * Get when to run the index merging for this group.
     *           No default value.
     * 
     * @return String
     */
    public String getCronExpression()
    {
        return this.cronExpression;
    } //-- String getCronExpression()

    /**
     * Get the id of the repository group.
     * 
     * @return String
     */
    public String getId()
    {
        return this.id;
    } //-- String getId()

    /**
     * Get the path of the merged index.
     * 
     * @return String
     */
    public String getMergedIndexPath()
    {
        return this.mergedIndexPath;
    } //-- String getMergedIndexPath()

    /**
     * Get the time to live of the merged index of the repository
     * group.
     * 
     * @return int
     */
    public int getMergedIndexTtl()
    {
        return this.mergedIndexTtl;
    } //-- int getMergedIndexTtl()

    /**
     * Method getRepositories.
     * 
     * @return List
     */
    public List<String> getRepositories()
    {
        if ( this.repositories == null )
        {
            this.repositories = new ArrayList<String>();
        }

        return this.repositories;
    } //-- java.util.List<String> getRepositories()

    /**
     * Method removeRepository.
     * 
     * @param string
     */
    public void removeRepository( String string )
    {
        getRepositories().remove( string );
    } //-- void removeRepository( String )

    /**
     * Set when to run the index merging for this group.
     *           No default value.
     * 
     * @param cronExpression
     */
    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    } //-- void setCronExpression( String )

    /**
     * Set the id of the repository group.
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId( String )

    /**
     * Set the path of the merged index.
     * 
     * @param mergedIndexPath
     */
    public void setMergedIndexPath( String mergedIndexPath )
    {
        this.mergedIndexPath = mergedIndexPath;
    } //-- void setMergedIndexPath( String )

    /**
     * Set the time to live of the merged index of the repository
     * group.
     * 
     * @param mergedIndexTtl
     */
    public void setMergedIndexTtl( int mergedIndexTtl )
    {
        this.mergedIndexTtl = mergedIndexTtl;
    } //-- void setMergedIndexTtl( int )

    /**
     * Set the list of repository ids under the group.
     * 
     * @param repositories
     */
    public void setRepositories( List<String> repositories )
    {
        this.repositories = repositories;
    } //-- void setRepositories( java.util.List )

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
