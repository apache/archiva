package org.apache.maven.repository.reporting;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.model.Model;
import org.apache.maven.repository.digest.DefaultDigester;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public class DuplicateArtifactFileReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private MockArtifactReporter reporter;

    private Artifact artifact;

    private Model model;

    private DuplicateArtifactFileReportProcessor processor;

    private ArtifactFactory artifactFactory;

    private String indexPath = new File( "target/.index" ).getAbsolutePath();

    protected void setUp()
        throws Exception
    {
        super.setUp();
        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.class.getName() );
        reporter = new MockArtifactReporter();
        artifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "1.0-alpha-1", "jar" );
        model = new Model();
        processor = new DuplicateArtifactFileReportProcessor();
        processor.setArtifactFactory( artifactFactory );

        ArtifactRepositoryIndex index = new ArtifactRepositoryIndex( indexPath, repository, new DefaultDigester() );
        index.indexArtifact( artifact );
        index.optimize();
        index.close();
    }

    protected void tearDown()
        throws Exception
    {
        FileUtils.deleteDirectory( indexPath );

        processor = null;
        model = null;
        artifact = null;
        reporter = null;
        super.tearDown();
    }

    public void testNullArtifactFile()
        throws Exception
    {
        artifact.setFile( null );

        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 0, reporter.getSuccesses() );
        assertEquals( "Check warnings", 1, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testSuccessOnAlreadyIndexedArtifact()
        throws Exception
    {
        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testSuccessOnDifferentGroupId()
        throws Exception
    {
        artifact.setGroupId( "different.groupId" );
        processor.processArtifact( model, artifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testSuccessOnNewArtifact()
        throws Exception
    {
        Artifact newArtifact = createArtifact( "groupId", "artifactId", "1.0-alpha-1", "1.0-alpha-1", "pom" );

        processor.processArtifact( model, newArtifact, reporter, repository );

        assertEquals( "Check no successes", 1, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 0, reporter.getFailures() );
    }

    public void testFailure()
        throws Exception
    {
        Artifact duplicate = createArtifact( artifact.getGroupId(), "snapshot-artifact", "1.0-alpha-1-SNAPSHOT",
                                             artifact.getVersion(), artifact.getType() );
        duplicate.setFile( artifact.getFile() );

        processor.processArtifact( model, duplicate, reporter, repository );

        assertEquals( "Check no successes", 0, reporter.getSuccesses() );
        assertEquals( "Check warnings", 0, reporter.getWarnings() );
        assertEquals( "Check no failures", 1, reporter.getFailures() );
    }

    private Artifact createArtifact( String groupId, String artifactId, String baseVersion, String version,
                                     String type )
    {
        Artifact artifact = artifactFactory.createArtifact( groupId, artifactId, version, null, type );
        artifact.setBaseVersion( baseVersion );
        artifact.setRepository( repository );
        artifact.setFile( new File( repository.getBasedir(), repository.pathOf( artifact ) ) );
        return artifact;
    }
}
