package org.apache.maven.archiva.model;

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
 * ArchivaRepository 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaRepository
{
    private ArchivaRepositoryModel model;

    private RepositoryURL url;

    protected boolean blacklisted;

    /**
     * Construct a Repository.
     * 
     * @param id the unique identifier for this repository.
     * @param name the name for this repository.
     * @param url the base URL for this repository (this should point to the top level URL for the entire repository)
     * @param layout the layout technique for this repository.
     */
    public ArchivaRepository( String id, String name, String url )
    {
        model = new ArchivaRepositoryModel();

        model.setId( id );
        model.setName( name );
        setUrl( new RepositoryURL( url ) );
    }

    /**
     * Construct a Repository.
     * 
     * @param model the model to use
     */
    public ArchivaRepository( ArchivaRepositoryModel model )
    {
        this.model = model;

        this.url = new RepositoryURL( model.getUrl() );
    }

    public String getId()
    {
        return model.getId();
    }

    public void setUrl( RepositoryURL url )
    {
        this.url = url;
        model.setUrl( url.getUrl() );
    }

    public RepositoryURL getUrl()
    {
        return this.url;
    }

    public ArchivaRepositoryModel getModel()
    {
        return this.model;
    }

    public boolean isBlacklisted()
    {
        return blacklisted;
    }

    public void setBlacklisted( boolean blacklisted )
    {
        this.blacklisted = blacklisted;
    }

    public boolean isRemote()
    {
        return this.url.getProtocol().equals( "file" );
    }

    public boolean isManaged()
    {
        return this.url.getProtocol().equals( "file" );
    }

    public String getLayoutType()
    {
        return this.model.getLayoutName();
    }

    public String getName()
    {
        return this.model.getName();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "ArchivaRepository[" );
        sb.append( this.model.getId() ).append( "," );
        sb.append( this.model.getUrl() );
        sb.append( "]" );

        return sb.toString();
    }
}
