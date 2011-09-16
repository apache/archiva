package org.apache.archiva.repository.content;

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
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.layout.LayoutException;
import org.apache.archiva.repository.metadata.MetadataTools;

/**
 * RepositoryRequest is used to determine the type of request that is incoming, and convert it to an appropriate
 * ArtifactReference.
 *
 * @version $Id$
 * <p/>
 */
public class RepositoryRequest
{
    private PathParser defaultPathParser = new DefaultPathParser();

    private PathParser legacyPathParser;

    public RepositoryRequest (LegacyPathParser legacyPathParser)
    {
        this.legacyPathParser = legacyPathParser;
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
        if ( StringUtils.isBlank( requestedPath ) )
        {
            throw new LayoutException( "Blank request path is not a valid." );
        }

        String path = requestedPath;
        while ( path.startsWith( "/" ) )
        {
            path = path.substring( 1 );

            // Only slash? that's bad, mmm-kay?
            if ( "/".equals( path ) )
            {
                throw new LayoutException( "Invalid request path: Slash only." );
            }
        }

        if ( isDefault( path ) )
        {
            return defaultPathParser.toArtifactReference( path );
        }
        else if ( isLegacy( path ) )
        {
            return legacyPathParser.toArtifactReference( path );
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

    public boolean isMetadataSupportFile( String requestedPath )
    {
        if ( isSupportFile( requestedPath ) )
        {
            String basefilePath = StringUtils.substring( requestedPath, 0, requestedPath.lastIndexOf( '.' ) );
            if ( isMetadata( basefilePath ) )
            {
                return true;
            }
        }

        return false;
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
        if ( pathParts.length > 3 )
        {
            return true;
        }
        else if ( pathParts.length == 3 )
        {
            // check if artifact-level metadata (ex. eclipse/jdtcore/maven-metadata.xml)
            if ( isMetadata( requestedPath ) )
            {
                return true;
            }
            else
            {
                // check if checksum of artifact-level metadata (ex. eclipse/jdtcore/maven-metadata.xml.sha1)
                int idx = requestedPath.lastIndexOf( '.' );
                if ( idx > 0 )
                {
                    String base = requestedPath.substring( 0, idx );
                    if ( isMetadata( base ) && isSupportFile( requestedPath ) )
                    {
                        return true;
                    }
                }

                return false;
            }
        }
        else
        {
            return false;
        }
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
     * @param repository    the repository to adjust to.
     * @return the adjusted (to native) path.
     * @throws LayoutException if the path cannot be parsed.
     */
    public String toNativePath( String requestedPath, ManagedRepositoryContent repository )
        throws LayoutException
    {
        if ( StringUtils.isBlank( requestedPath ) )
        {
            throw new LayoutException( "Request Path is blank." );
        }

        String referencedResource = requestedPath;
        // No checksum by default.
        String supportfile = "";

        // Figure out support file, and actual referencedResource.
        if ( isSupportFile( requestedPath ) )
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
}
