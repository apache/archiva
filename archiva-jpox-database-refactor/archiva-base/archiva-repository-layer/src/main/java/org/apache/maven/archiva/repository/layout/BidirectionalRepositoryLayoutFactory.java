package org.apache.maven.archiva.repository.layout;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.HashMap;
import java.util.Map;

/**
 * BidirectionalRepositoryLayoutFactory 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory"
 */
public class BidirectionalRepositoryLayoutFactory
    extends AbstractLogEnabled
    implements RegistryListener, Initializable
{
    /**
     * @plexus.requirement role="org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout"
     */
    private Map layouts;
    
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;
    
    private Map repositoryMap = new HashMap();

    public BidirectionalRepositoryLayout getLayout( String type )
        throws LayoutException
    {
        if ( !layouts.containsKey( type ) )
        {
            throw new LayoutException( "Layout type [" + type + "] does not exist.  " + "Available types ["
                + layouts.keySet() + "]" );
        }

        return (BidirectionalRepositoryLayout) layouts.get( type );
    }

    public BidirectionalRepositoryLayout getLayout( ArchivaArtifact artifact )
        throws LayoutException
    {
        if ( artifact == null )
        {
            throw new LayoutException( "Cannot determine layout using a null artifact." );
        }
        
        String repoId = artifact.getModel().getRepositoryId();
        if ( StringUtils.isBlank( repoId ) )
        {
            throw new LayoutException( "Cannot determine layout using artifact with no repository id: " + artifact );
        }
        
        RepositoryConfiguration repo = (RepositoryConfiguration) this.repositoryMap.get( repoId );
        return getLayout( repo.getLayout() );
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
        {
            initRepositoryMap();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }
    
    private void initRepositoryMap()
    {
        synchronized ( this.repositoryMap )
        {
            this.repositoryMap.clear();
            this.repositoryMap.putAll( configuration.getConfiguration().createRepositoryMap() );
        }
    }

    public void initialize()
        throws InitializationException
    {
        initRepositoryMap();
        configuration.addChangeListener( this );
    }
}
