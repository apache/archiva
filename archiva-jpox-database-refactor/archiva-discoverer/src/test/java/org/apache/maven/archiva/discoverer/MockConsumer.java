/**
 * 
 */
package org.apache.maven.archiva.discoverer;

import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.consumers.Consumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.ArrayList;
import java.util.List;

public class MockConsumer
    implements Consumer
{
    private List excludePatterns = new ArrayList();

    private List includePatterns = new ArrayList();

    private List filesProcessed = new ArrayList();

    private int countFileProblems = 0;

    public String getName()
    {
        return "MockConsumer (Testing Only)";
    }

    public boolean init( ArtifactRepository repository )
    {
        return true;
    }

    public void processFile( BaseFile file )
        throws ConsumerException
    {
        filesProcessed.add( file );
    }

    public void processFileProblem( BaseFile file, String message )
    {
        countFileProblems++;
    }

    public List getExcludePatterns()
    {
        return excludePatterns;
    }

    public void setExcludePatterns( List excludePatterns )
    {
        this.excludePatterns = excludePatterns;
    }

    public List getIncludePatterns()
    {
        return includePatterns;
    }

    public void setIncludePatterns( List includePatterns )
    {
        this.includePatterns = includePatterns;
    }

    public int getCountFileProblems()
    {
        return countFileProblems;
    }

    public List getFilesProcessed()
    {
        return filesProcessed;
    }
}