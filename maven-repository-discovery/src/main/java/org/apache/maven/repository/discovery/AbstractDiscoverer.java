package org.apache.maven.repository.discovery;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Base class for the artifact and metadata discoverers.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractDiscoverer
    extends AbstractLogEnabled
    implements Discoverer
{
    private Map kickedOutPaths = new HashMap();

    /**
     * @plexus.requirement
     */
    protected ArtifactFactory artifactFactory;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private List excludedPaths = new ArrayList();

    /**
     * Add a path to the list of files that were kicked out due to being invalid.
     *
     * @param path the path to add
     * @param reason the reason why the path is being kicked out
     */
    protected void addKickedOutPath( String path, String reason )
    {
        kickedOutPaths.put( path, reason );
    }

    public Iterator getKickedOutPathsIterator()
    {
        return kickedOutPaths.keySet().iterator();
    }

    /**
     * Scan the repository for artifact paths.
     */
    protected String[] scanForArtifactPaths( File repositoryBase, String blacklistedPatterns, String[] includes,
                                             String[] excludes )
    {
        List allExcludes = new ArrayList();
        allExcludes.addAll( FileUtils.getDefaultExcludesAsList() );
        if ( excludes != null )
        {
            allExcludes.addAll( Arrays.asList( excludes ) );
        }

        if ( blacklistedPatterns != null && blacklistedPatterns.length() > 0 )
        {
            allExcludes.addAll( Arrays.asList( blacklistedPatterns.split( "," ) ) );
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryBase );
        if ( includes != null )
        {
            scanner.setIncludes( includes );
        }
        scanner.setExcludes( (String[]) allExcludes.toArray( EMPTY_STRING_ARRAY ) );

        scanner.scan();

        excludedPaths.addAll( Arrays.asList( scanner.getExcludedFiles() ) );

        return scanner.getIncludedFiles();
    }

    public Iterator getExcludedPathsIterator()
    {
        return excludedPaths.iterator();
    }
}
