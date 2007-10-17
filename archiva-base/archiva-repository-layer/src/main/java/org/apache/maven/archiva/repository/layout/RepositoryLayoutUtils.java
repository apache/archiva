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
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.repository.ManagedRepositoryContent;
import org.apache.maven.archiva.repository.RemoteRepositoryContent;
import org.apache.maven.archiva.repository.RepositoryContentFactory;

/**
 * RepositoryLayoutUtils - utility methods common for most BidirectionalRepositoryLayout implementation. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @deprecated use {@link RepositoryContentFactory} and {@link ManagedRepositoryContent} 
 *             or {@link RemoteRepositoryContent} instead.
 */
public class RepositoryLayoutUtils
{
    /**
     * Complex 2+ part extensions.
     * Do not include initial "." character in extension names here.
     */
    private static final String ComplexExtensions[] = new String[] { "tar.gz", "tar.bz2" };

    /**
     * Filename Parsing Mode - Artifact Id.
     */
    private static final int ARTIFACTID = 1;

    /**
     * Filename Parsing Mode - Version.
     */
    private static final int VERSION = 2;

    /**
     * Filename Parsing Mode - Classifier.
     */
    private static final int CLASSIFIER = 3;

    /**
     * Split the provided filename into 4 String parts. Simply delegate to 
     * splitFilename( filename, possibleArtifactId, possibleVersion ) with no possibleVersion
     * proposal.
     *
     * @param filename the filename to split.
     * @param possibleArtifactId the optional artifactId to aide in splitting the filename.
     *                  (null to allow algorithm to calculate one)
     * @return the parts of the filename.
     * @throws LayoutException
     * @deprecated to not use directly. Use {@link ManagedRepositoryContent} or {@link RemoteRepositoryContent} instead.
     */
    public static FilenameParts splitFilename( String filename, String possibleArtifactId ) throws LayoutException
    {
        return splitFilename( filename, possibleArtifactId, null );
    }

    /**
     * Split the provided filename into 4 String parts.
     *
     * <pre>
     * String part[] = splitFilename( filename );
     * artifactId = part[0];
     * version = part[1];
     * classifier = part[2];
     * extension = part[3];
     * </pre>
     *
     * @param filename the filename to split.
     * @param possibleArtifactId the optional artifactId to aide in splitting the filename.
     *                  (null to allow algorithm to calculate one)
     * @param possibleVersion the optional version to aide in splitting the filename.
     *                  (null to allow algorithm to calculate one)
     * @return the parts of the filename.
     * @throws LayoutException
     * @deprecated to not use directly. Use {@link ManagedRepositoryContent} or {@link RemoteRepositoryContent} instead.
     */
    public static FilenameParts splitFilename( String filename, String possibleArtifactId,
                                               String possibleVersion ) throws LayoutException
    {
        if ( StringUtils.isBlank( filename ) )
        {
            throw new IllegalArgumentException( "Unable to split blank filename." );
        }

        String filestring = filename.trim();

        FilenameParts parts = new FilenameParts();
        // I like working backwards.

        // Find the extension.

        // Work on multipart extensions first.
        boolean found = false;

        String lowercaseFilename = filestring.toLowerCase();
        for ( int i = 0; i < ComplexExtensions.length && !found; i++ )
        {
            if ( lowercaseFilename.endsWith( "." + ComplexExtensions[i] ) )
            {
                parts.extension = ComplexExtensions[i];
                filestring = filestring.substring( 0, filestring.length() - ComplexExtensions[i].length() - 1 );
                found = true;
            }
        }

        if ( !found )
        {
            // Default to 1 part extension.

            int index = filestring.lastIndexOf( '.' );
            if ( index <= 0 )
            {
                // Bad Filename - No Extension
                throw new LayoutException( "Unable to determine extension from filename " + filename );
            }
            parts.extension = filestring.substring( index + 1 );
            filestring = filestring.substring( 0, index );
        }

        // Work on version string.
        int mode = ARTIFACTID;

        if ( startsWith( filename, possibleArtifactId ) )
        {
            parts.artifactId = possibleArtifactId;
            filestring = filestring.substring( possibleArtifactId.length() + 1 );
            mode = VERSION;
        }

        if ( startsWith( filestring, possibleVersion ) )
        {
            if ( filestring.length() > possibleVersion.length() )
            {
                filestring = filestring.substring( possibleVersion.length() );
            }
            else
            {
                filestring = "";
            }
            parts.version = possibleVersion;
            mode = CLASSIFIER;
        }

        String fileParts[] = StringUtils.split( filestring, '-' );

        int versionStart = -1;
        int versionEnd = -1;
        for ( int i = 0; i < fileParts.length; i++ )
        {
            String part = fileParts[i];

            if ( VersionUtil.isSimpleVersionKeyword( part ) )
            {
                // It is a potential version part.
                if ( versionStart < 0 )
                {
                    versionStart = i;
                }

                versionEnd = i;
            }
        }

        if ( versionStart < 0 && parts.version == null )
        {
            // Assume rest of string is the version Id.

            if ( fileParts.length > 0 )
            {
                versionStart = 0;
                versionEnd = fileParts.length;
            }
            else
            {
                throw new LayoutException( "Unable to determine version from filename " + filename );
            }
        }

        // Gather up the ArtifactID - Version - Classifier pieces found.

        for ( int i = 0; i < fileParts.length; i++ )
        {
            String part = fileParts[i];

            if ( ( mode == ARTIFACTID ) && ( i >= versionStart ) )
            {
                if ( StringUtils.isBlank( parts.artifactId ) )
                {
                    throw new LayoutException( "No Artifact Id detected." );
                }
                mode = VERSION;
            }

            switch ( mode )
            {
                case ARTIFACTID:
                    parts.appendArtifactId( part );
                    break;
                case VERSION:
                    parts.appendVersion( part );
                    break;
                case CLASSIFIER:
                    parts.appendClassifier( part );
                    break;
            }

            if ( i >= versionEnd )
            {
                mode = CLASSIFIER;
            }
        }

        return parts;
    }

    /**
     * Check if the string starts with the proposed token, with no more char
     * expeect the '-' separator.
     * @param string string to test
     * @param possible proposed startOf
     * @return true if the possible matches
     */
    private static boolean startsWith( String string, String possible )
    {
        if (possible == null)
        {
            return false;
        }
        int length = possible.length();
        return string.startsWith( possible ) && ( string.length() == length || string.charAt( length ) == '-' );
    }
}
