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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RepositoryLayoutUtils - utility methods common for most BidirectionalRepositoryLayout implementation. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class RepositoryLayoutUtils
{
    /**
     * Complex 2+ part extensions.
     * Do not include initial "." character in extension names here.
     */
    private static final String ComplexExtensions[] = new String[] { "tar.gz", "tar.bz2" };

    /**
     * These are the version patterns found in the filenames of the various artifact's versions IDs.
     * These patterns are all tackling lowercase version IDs.
     */
    private static final String VersionPatterns[] =
        new String[] { "(snapshot)", "([0-9][_.0-9a-z]*)", "(g?[_.0-9ab]*(pre|rc|g|m)[_.0-9]*)", "(dev[_.0-9]*)",
            "(alpha[_.0-9]*)", "(beta[_.0-9]*)", "(rc[_.0-9]*)", "(test[_.0-9]*)", "(debug[_.0-9]*)",
            "(unofficial[_.0-9]*)", "(current)", "(latest)", "(fcs)", "(release[_.0-9]*)", "(nightly)", "(final)",
            "(incubating)", "(incubator)", "([ab][_.0-9]*)" };

    private static final String VersionMegaPattern = StringUtils.join( VersionPatterns, '|' );

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
     * Split the provided filename into 4 String parts.
     * 
     * <pre>
     * String part[] = splitFilename( filename );
     * artifactId = part[0];
     * version    = part[1];
     * classifier = part[2];
     * extension  = part[3];
     * </pre>
     * 
     * @param filename the filename to split.
     * @param possibleArtifactId the optional artifactId to aide in splitting the filename. 
     *                  (null to allow algorithm to calculate one)
     * @return the parts of the filename.
     * @throws LayoutException 
     */
    public static FilenameParts splitFilename( String filename, String possibleArtifactId ) throws LayoutException
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

        if ( ( possibleArtifactId != null ) && filename.startsWith( possibleArtifactId ) )
        {
            parts.artifactId = possibleArtifactId;
            filestring = filestring.substring( possibleArtifactId.length() + 1 );
        }

        String fileParts[] = StringUtils.split( filestring, '-' );

        int versionStart = -1;
        int versionEnd = -1;

        Pattern pat = Pattern.compile( VersionMegaPattern, Pattern.CASE_INSENSITIVE );
        Matcher mat;

        for ( int i = 0; i < fileParts.length; i++ )
        {
            String part = fileParts[i];
            mat = pat.matcher( part );

            if ( mat.matches() )
            {
                // It is a potential verion part.
                if ( versionStart < 0 )
                {
                    versionStart = i;
                }

                versionEnd = i;
            }
        }

        if ( versionStart < 0 )
        {
            throw new LayoutException( "Unable to determine version from filename " + filename );
        }

        // Gather up the ArtifactID - Version - Classifier pieces found. 

        int mode = ARTIFACTID;
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

}
