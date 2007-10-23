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
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
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
        if ( isDefault( requestedPath ) )
        {
            return DefaultPathParser.toArtifactReference( requestedPath );
        }
        else if ( isLegacy( requestedPath ) )
        {
            return LegacyPathParser.toArtifactReference( requestedPath );
        }
        else
        {
            throw new LayoutException( "Not a valid request path layout, too short." );
        }
    }
    
    /**
     * <p>
     * Tests the path to see if it conforms to the expectations of a metadata request.
     * </p>
     * <p>
     * NOTE: This does a cursory check on the path's last element.  A result of true
     * from this method is not a guarantee that the metadata is in a valid format, or
     * that it even contains data.
     * </p> 
     * 
     * @param requestedPath the path to test.
     * @return true if the requestedPath is likely a metadata request.
     */
    public boolean isMetadata( String requestedPath )
    {
        return requestedPath.endsWith( "/" + MetadataTools.MAVEN_METADATA );
    }
    
    /**
     * <p>
     * Tests the path to see if it conforms to the expectations of a support file request.
     * </p>
     * <p>
     * Tests for <code>.sha1</code>, <code>.md5</code>, <code>.asc</code>, and <code>.php</code>.
     * </p>
     * <p>
     * NOTE: This does a cursory check on the path's extension only.  A result of true
     * from this method is not a guarantee that the support resource is in a valid format, or
     * that it even contains data.
     * </p> 
     * 
     * @param requestedPath the path to test.
     * @return true if the requestedPath is likely that of a support file request.
     */
    public boolean isSupportFile( String requestedPath )
    {
        int idx = requestedPath.lastIndexOf( '.' );
        if ( idx <= 0 )
        {
            return false;
        }

        String ext = requestedPath.substring( idx );
        return ( ".sha1".equals( ext ) || ".md5".equals( ext ) || ".asc".equals( ext ) || ".pgp".equals( ext ) );
    }
    
    /**
     * <p>
     * Tests the path to see if it conforms to the expectations of a default layout request.
     * </p>
     * <p>
     * NOTE: This does a cursory check on the count of path elements only.  A result of
     * true from this method is not a guarantee that the path sections are valid and
     * can be resolved to an artifact reference.  use {@link #toArtifactReference(String)} 
     * if you want a more complete analysis of the validity of the path.
     * </p>
     * 
     * @param requestedPath the path to test.
     * @return true if the requestedPath is likely that of a default layout request.
     */
    public boolean isDefault( String requestedPath )
    {
        if ( StringUtils.isBlank( requestedPath ) )
        {
            return false;
        }
        
        String pathParts[] = StringUtils.splitPreserveAllTokens( requestedPath, '/' );
        return pathParts.length > 3;
    }
    
    /**
     * <p>
     * Tests the path to see if it conforms to the expectations of a legacy layout request.
     * </p>
     * <p>
     * NOTE: This does a cursory check on the count of path elements only.  A result of
     * true from this method is not a guarantee that the path sections are valid and
     * can be resolved to an artifact reference.  use {@link #toArtifactReference(String)} 
     * if you want a more complete analysis of the validity of the path.
     * </p>
     * 
     * @param requestedPath the path to test.
     * @return true if the requestedPath is likely that of a legacy layout request.
     */
    public boolean isLegacy( String requestedPath )
    {
        if ( StringUtils.isBlank( requestedPath ) )
        {
            return false;
        }
        
        String pathParts[] = StringUtils.splitPreserveAllTokens( requestedPath, '/' );
        return pathParts.length == 3;
    }
    
    /**
     * Adjust the requestedPath to conform to the native layout of the provided {@link ManagedRepositoryContent}.
     * 
     * @param requestedPath the incoming requested path.
     * @param repository the repository to adjust to.
     * @return the adjusted (to native) path.
     * @throws LayoutException if the path cannot be parsed. 
     */
    public String toNativePath( String requestedPath, ManagedRepositoryContent repository ) throws LayoutException
    {
        if ( StringUtils.isBlank( requestedPath ) )
        {
            throw new LayoutException( "Request Path is blank." );
        }
        
        String referencedResource = requestedPath;
        // No checksum by default.
        String supportfile = "";
        
        // Figure out support file, and actual referencedResource.
        if( isSupportFile( requestedPath ) )
        {
            int idx = requestedPath.lastIndexOf( '.' );
            referencedResource = requestedPath.substring( 0, idx );
            supportfile = requestedPath.substring( idx );
        }

        if ( isMetadata( referencedResource ) )
        {
            if ( repository instanceof ManagedLegacyRepositoryContent )
            {
                throw new LayoutException( "Cannot translate metadata request to legacy layout." );
            }
            
            /* Nothing to translate.
             * Default layout is the only layout that can contain maven-metadata.xml files, and
             * if the managedRepository is layout legacy, this request would never occur.
             */
            return requestedPath;
        }

        // Treat as an artifact reference.
        ArtifactReference ref = toArtifactReference( referencedResource );
        String adjustedPath = repository.toPath( ref );
        return adjustedPath + supportfile;
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
