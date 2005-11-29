package org.apache.maven.repository.discovery;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for artifact discoverers.
 *
 * @author John Casey
 * @author Brett Porter
 */
public abstract class AbstractArtifactDiscoverer
    extends AbstractLogEnabled
    implements ArtifactDiscoverer
{
    /**
     * Standard patterns to exclude from discovery as they are not artifacts.
     */
    private static final String[] STANDARD_DISCOVERY_EXCLUDES = {"bin/**", "reports/**", ".maven/**", "**/*.md5",
        "**/*.MD5", "**/*.sha1", "**/*.SHA1", "**/*snapshot-version", "*/website/**", "*/licenses/**", "*/licences/**",
        "**/.htaccess", "**/*.html", "**/*.asc", "**/*.txt", "**/*.xml", "**/README*", "**/CHANGELOG*", "**/KEYS*"};

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private List excludedPaths = new ArrayList();

    private List kickedOutPaths = new ArrayList();

    /**
     * Scan the repository for artifact paths
     *
     * @todo operate on better parameters, more collections, less arrays
     */
    protected String[] scanForArtifactPaths( File repositoryBase, String blacklistedPatterns )
    {
        String[] blacklisted;
        if ( blacklistedPatterns != null && blacklistedPatterns.length() > 0 )
        {
            blacklisted = blacklistedPatterns.split( "," );
        }
        else
        {
            blacklisted = EMPTY_STRING_ARRAY;
        }

        String[] allExcludes = new String[STANDARD_DISCOVERY_EXCLUDES.length + blacklisted.length];

        System.arraycopy( STANDARD_DISCOVERY_EXCLUDES, 0, allExcludes, 0, STANDARD_DISCOVERY_EXCLUDES.length );
        System.arraycopy( blacklisted, 0, allExcludes, STANDARD_DISCOVERY_EXCLUDES.length, blacklisted.length );

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryBase );
        scanner.setExcludes( allExcludes );

        scanner.scan();

        excludedPaths.addAll( Arrays.asList( scanner.getExcludedFiles() ) );

        return scanner.getIncludedFiles();
    }

    /**
     * Add a path to the list of files that were kicked out due to being invalid.
     *
     * @param path the path to add
     */
    protected void addKickedOutPath( String path )
    {
        kickedOutPaths.add( path );
    }

    public Iterator getExcludedPathsIterator()
    {
        return excludedPaths.iterator();
    }

    public Iterator getKickedOutPathsIterator()
    {
        return kickedOutPaths.iterator();
    }
}
