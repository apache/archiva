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
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.Xpp3DomWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

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

    protected static final String DATE_FMT = "yyyyMMddHHmmss";

    /**
     * Add a path to the list of files that were kicked out due to being invalid.
     *
     * @param path   the path to add
     * @param reason the reason why the path is being kicked out
     */
    protected void addKickedOutPath( String path, String reason )
    {
        kickedOutPaths.add( new DiscovererPath( path, reason ) );
    }

    /**
     * Returns an iterator for the list if DiscovererPaths that were found to not represent a searched object
     *
     * @return Iterator for the DiscovererPath List
     */
    public Iterator getKickedOutPathsIterator()
    {
        return kickedOutPaths.iterator();
    }

    protected List scanForArtifactPaths( File repositoryBase, String blacklistedPatterns, String[] includes,
                                         String[] excludes, long comparisonTimestamp )
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

            excludedPaths.add( new DiscovererPath( path, "Artifact was in the specified list of exclusions" ) );
        }

        // TODO: this could be a part of the scanner
        List includedPaths = new ArrayList();
        for ( Iterator files = Arrays.asList( scanner.getIncludedFiles() ).iterator(); files.hasNext(); )
        {
            String path = files.next().toString();

            if ( comparisonTimestamp == 0 || new File( repositoryBase, path ).lastModified() > comparisonTimestamp )
            {
                includedPaths.add( path );
            }
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
        return excludedPaths.iterator();
    }

    protected long readComparisonTimestamp( ArtifactRepository repository, String operation, Xpp3Dom dom )
    {
        Xpp3Dom entry = dom.getChild( operation );
        long comparisonTimestamp = 0;
        if ( entry != null )
        {
            try
            {
                comparisonTimestamp = new SimpleDateFormat( DATE_FMT, Locale.US ).parse( entry.getValue() ).getTime();
            }
            catch ( ParseException e )
            {
                getLogger().error( "Timestamp was invalid: " + entry.getValue() + "; ignoring" );
            }
        }
        return comparisonTimestamp;
    }

    protected Xpp3Dom readDom( File file )
    {
        Xpp3Dom dom;
        FileReader fileReader = null;
        try
        {
            fileReader = new FileReader( file );
            dom = Xpp3DomBuilder.build( fileReader );
        }
        catch ( FileNotFoundException e )
        {
            // Safe to ignore
            dom = new Xpp3Dom( "metadata" );
        }
        catch ( XmlPullParserException e )
        {
            getLogger().error( "Error reading metadata (ignoring and recreating): " + e.getMessage() );
            dom = new Xpp3Dom( "metadata" );
        }
        catch ( IOException e )
        {
            getLogger().error( "Error reading metadata (ignoring and recreating): " + e.getMessage() );
            dom = new Xpp3Dom( "metadata" );
        }
        finally
        {
            IOUtil.close( fileReader );
        }
        return dom;
    }

    protected Xpp3Dom getLastArtifactDiscoveryDom( Xpp3Dom dom )
    {
        Xpp3Dom lastDiscoveryDom = dom.getChild( "lastArtifactDiscovery" );
        if ( lastDiscoveryDom == null )
        {
            dom.addChild( new Xpp3Dom( "lastArtifactDiscovery" ) );
            lastDiscoveryDom = dom.getChild( "lastArtifactDiscovery" );
        }
        return lastDiscoveryDom;
    }

    protected Xpp3Dom getLastMetadataDiscoveryDom( Xpp3Dom dom )
    {
        Xpp3Dom lastDiscoveryDom = dom.getChild( "lastMetadataDiscovery" );
        if ( lastDiscoveryDom == null )
        {
            dom.addChild( new Xpp3Dom( "lastMetadataDiscovery" ) );
            lastDiscoveryDom = dom.getChild( "lastMetadataDiscovery" );
        }
        return lastDiscoveryDom;
    }

    public void resetLastCheckedTime( ArtifactRepository repository, String operation )
        throws IOException
    {
        // TODO: get these changes into maven-metadata.xml and migrate towards that. The model is further diverging to a different layout at each level so submodels might be a good idea.
        // TODO: maven-artifact probably needs an improved pathOfMetadata to cope with top level metadata
        // TODO: might we need to write this as maven-metadata-local in some circumstances? merge others? Probably best to keep it simple and just use this format at the root. No need to merge anything that I can see
        // TODO: since this metadata isn't meant to be shared, perhaps another file is called for after all.
        // Format is: <repository><lastDiscovery><KEY>yyyyMMddHHmmss</KEY></lastDiscovery></repository> (ie, flat properties)

        File file = new File( repository.getBasedir(), "maven-metadata.xml" );

        Xpp3Dom dom = readDom( file );

        boolean changed = false;

        if ( removeEntry( getLastArtifactDiscoveryDom( dom ), operation ) )
        {
            changed = true;
        }

        if ( removeEntry( getLastMetadataDiscoveryDom( dom ), operation ) )
        {
            changed = true;
        }

        if ( changed )
        {
            saveDom( file, dom );
        }
    }

    private boolean removeEntry( Xpp3Dom lastDiscoveryDom, String operation )
    {
        boolean changed = false;

        // do this in reverse so that removing doesn't affect counter
        Xpp3Dom[] children = lastDiscoveryDom.getChildren();
        for ( int i = lastDiscoveryDom.getChildCount() - 1; i >= 0; i-- )
        {
            if ( children[i].getName().equals( operation ) )
            {
                changed = true;
                lastDiscoveryDom.removeChild( i );
            }
        }
        return changed;
    }

    protected void saveDom( File file, Xpp3Dom dom )
        throws IOException
    {
        FileWriter writer = new FileWriter( file );

        // save metadata
        try
        {
            Xpp3DomWriter.write( writer, dom );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    protected void setEntry( Xpp3Dom lastDiscoveryDom, String operation, String dateString )
    {
        Xpp3Dom entry = lastDiscoveryDom.getChild( operation );
        if ( entry == null )
        {
            entry = new Xpp3Dom( operation );
            lastDiscoveryDom.addChild( entry );
        }
        entry.setValue( dateString );
    }

    protected Xpp3Dom readRepositoryMetadataDom( ArtifactRepository repository )
    {
        return readDom( new File( repository.getBasedir(), "maven-metadata.xml" ) );
    }
}
