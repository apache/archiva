package org.apache.maven.archiva.repository.content;

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
import org.apache.maven.archiva.configuration.FileTypes;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.util.SelectorUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * RepositoryRequest is used to determine the type of request that is incoming, and convert it to an appropriate
 * ArtifactReference.  
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.repository.content.RepositoryRequest"
 */
public class RepositoryRequest
    implements RegistryListener, Initializable
{
    /**
     * @plexus.requirement
     */
    private FileTypes filetypes;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private List<String> artifactPatterns;

    /**
     * Test path to see if it is an artifact being requested (or not).
     * 
     * @param requestedPath the path to test.
     * @return true if it is an artifact being requested.
     */
    public boolean isArtifact( String requestedPath )
    {
        // Correct the slash pattern.
        String relativePath = requestedPath.replace( '\\', '/' );

        Iterator<String> it = this.artifactPatterns.iterator();
        while ( it.hasNext() )
        {
            String pattern = it.next();

            if ( SelectorUtils.matchPath( pattern, relativePath, false ) )
            {
                // Found match
                return true;
            }
        }

        // No match.
        return false;
    }

    /**
     * Takes an incoming requested path (in "/" format) and gleans the layout
     * and ArtifactReference appropriate for that content.
     * 
     * @param requestedPath the relative path to the content.
     * @return the ArtifactReference for the requestedPath.
     * @throws LayoutException if the request path is not layout valid. 
     */
    public ArtifactReference toArtifactReference( String requestedPath )
        throws LayoutException
    {
        String pathParts[] = StringUtils.splitPreserveAllTokens( requestedPath, '/' );

        if ( pathParts.length > 3 )
        {
            return DefaultPathParser.toArtifactReference( requestedPath );
        }
        else if ( pathParts.length == 3 )
        {
            return LegacyPathParser.toArtifactReference( requestedPath );
        }
        else
        {
            throw new LayoutException( "Not a valid request path layout, too short." );
        }
    }

    public void initialize()
        throws InitializationException
    {
        this.artifactPatterns = new ArrayList<String>();
        initVariables();
        this.archivaConfiguration.addChangeListener( this );
    }

    private void initVariables()
    {
        synchronized ( this.artifactPatterns )
        {
            this.artifactPatterns.clear();
            this.artifactPatterns.addAll( filetypes.getFileTypePatterns( FileTypes.ARTIFACTS ) );
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( propertyName.contains( "fileType" ) )
        {
            initVariables();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* nothing to do */

    }
}
