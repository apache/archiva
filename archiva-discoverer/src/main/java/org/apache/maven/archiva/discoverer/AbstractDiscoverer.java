package org.apache.maven.archiva.discoverer;

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
     * @plexus.configuration default-value="true"
     */
    private boolean trackOmittedPaths;

    /**
     * Add a path to the list of files that were kicked out due to being invalid.
     *
     * @param path   the path to add
     * @param reason the reason why the path is being kicked out
     */
    protected void addKickedOutPath( String path, String reason )
    {
        if ( trackOmittedPaths )
        {
            kickedOutPaths.add( new DiscovererPath( path, reason ) );
        }
    }

    /**
     * Add a path to the list of files that were excluded.
     *
     * @param path   the path to add
     * @param reason the reason why the path is excluded
     */
    protected void addExcludedPath( String path, String reason )
    {
        excludedPaths.add( new DiscovererPath( path, reason ) );
    }

    /**
     * Returns an iterator for the list if DiscovererPaths that were found to not represent a searched object
     *
     * @return Iterator for the DiscovererPath List
     */
    public Iterator getKickedOutPathsIterator()
    {
        assert trackOmittedPaths;
        return kickedOutPaths.iterator();
    }

    protected List scanForArtifactPaths( File repositoryBase, List blacklistedPatterns, String[] includes,
                                         String[] excludes )
    {
        List allExcludes = new ArrayList();
        allExcludes.addAll( FileUtils.getDefaultExcludesAsList() );
        if ( excludes != null )
        {
            allExcludes.addAll( Arrays.asList( excludes ) );
        }
        if ( blacklistedPatterns != null )
        {
            allExcludes.addAll( blacklistedPatterns );
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryBase );
        if ( includes != null )
        {
            scanner.setIncludes( includes );
        }
        scanner.setExcludes( (String[]) allExcludes.toArray( EMPTY_STRING_ARRAY ) );

        // TODO: Correct for extremely large repositories (artifact counts over 200,000 entries)
        scanner.scan();

        if ( trackOmittedPaths )
        {
            for ( Iterator files = Arrays.asList( scanner.getExcludedFiles() ).iterator(); files.hasNext(); )
            {
                String path = files.next().toString();

                excludedPaths.add( new DiscovererPath( path, "Artifact was in the specified list of exclusions" ) );
            }
        }

        // TODO: this could be a part of the scanner
        List includedPaths = new ArrayList();
        for ( Iterator files = Arrays.asList( scanner.getIncludedFiles() ).iterator(); files.hasNext(); )
        {
            String path = files.next().toString();

            includedPaths.add( path );
        }

        return includedPaths;
    }

    /**
     * Returns an iterator for the list if DiscovererPaths that were not processed because they are explicitly excluded
     *
     * @return Iterator for the DiscovererPath List
     */
    public Iterator getExcludedPathsIterator()
    {
        assert trackOmittedPaths;
        return excludedPaths.iterator();
    }

    public void setTrackOmittedPaths( boolean trackOmittedPaths )
    {
        this.trackOmittedPaths = trackOmittedPaths;
    }
}
