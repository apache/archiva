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
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for the artifact and metadata discoverers.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public abstract class AbstractDiscoverer
    extends AbstractLogEnabled
    implements Discoverer
{
    private List kickedOutPaths = new ArrayList();

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
        kickedOutPaths.add( new DiscovererPath( path, reason ) );
    }

    public Iterator getKickedOutPathsIterator()
    {
        return kickedOutPaths.iterator();
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

        if ( !StringUtils.isEmpty( blacklistedPatterns ) )
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

        for ( Iterator files = Arrays.asList( scanner.getExcludedFiles() ).iterator(); files.hasNext(); )
        {
            String path = files.next().toString();

            excludedPaths.add( new DiscovererPath( path, "Excluded by DirectoryScanner" ) );
        }

        return scanner.getIncludedFiles();
    }

    public Iterator getExcludedPathsIterator()
    {
        return excludedPaths.iterator();
    }
}
