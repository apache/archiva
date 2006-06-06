package org.apache.maven.repository.discovery;

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
 * TODO [!]: Description.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class AbstractDiscoverer
    extends AbstractLogEnabled
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
     * @todo add a reason
     */
    protected void addKickedOutPath( String path )
    {
        kickedOutPaths.add( path );
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
