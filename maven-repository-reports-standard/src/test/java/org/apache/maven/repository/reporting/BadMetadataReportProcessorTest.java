package org.apache.maven.repository.reporting;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;

import java.util.Iterator;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;

/**
 * @todo???  should use MetadataXpp3Reader instead ?
 */
public class BadMetadataReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    protected ArtifactFactory artifactFactory;

    private MetadataReportProcessor badMetadataReportProcessor;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        badMetadataReportProcessor = (MetadataReportProcessor) lookup( MetadataReportProcessor.ROLE );
    }
    
    public void testMetadataMissingLastUpdated()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        assertEquals( "check metadata", metadata, result.getMetadata() );
        assertEquals( "check reason", 
                      "Missing lastUpdated element inside the metadata.",
                      result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }
    
    public void testMetadataValidVersions()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertFalse( "check there are no failures", failures.hasNext() );
    }

    public void testMetadataMissingADirectory()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        assertEquals( "check metadata", metadata, result.getMetadata() );
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-2 found in the repository but missing in the metadata.",
                      result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }
    
    public void testMetadataInvalidArtifactVersion()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );
        versioning.addVersion( "1.0-alpha-3" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        assertEquals( "check metadata", metadata, result.getMetadata() );
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-3 is present in metadata but missing in the repository.",
                      result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }
    
    public void testMoreThanOneMetadataVersionErrors()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-3" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        assertEquals( "check metadata", metadata, result.getMetadata() );
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-3 is present in metadata but missing in the repository.",
                      result.getReason() );
        assertTrue( "check there is a 2nd failure", failures.hasNext() );
        result = (RepositoryMetadataResult) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-2 found in the repository but missing in the metadata.",
                      result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testValidPluginMetadata()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", "default" ) );
        
        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );
        
        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertFalse( "check there are no failures", failures.hasNext() );
    }
    
    public void testMissingMetadataPlugin()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "missing-plugin", "default" ) );
        
        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Metadata plugin missing-plugin is not present in the repository", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }
    
    public void testInvalidPluginArtifactId()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( null, "default" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "", "default2" ) );
        
        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty artifactId in group metadata.", result.getReason() );
        assertTrue( "check there is a 2nd failure", failures.hasNext() );
        result = (RepositoryMetadataResult) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty artifactId in group metadata.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }
    
    public void testInvalidPluginPrefix()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", null ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "snapshot-artifact", "" ) );
        
        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty plugin prefix for artifactId artifactId.", result.getReason() );
        assertTrue( "check there is a 2nd failure", failures.hasNext() );
        result = (RepositoryMetadataResult) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty plugin prefix for artifactId snapshot-artifact.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }
    
    public void testDuplicatePluginPrefixes()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();

        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", "default" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "snapshot-artifact", "default" ) );
        
        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );

        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Duplicate plugin prefix found: default.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testValidSnapshotMetadata()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();
        
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "snapshot-artifact", 
                                                                 "1.0-alpha-1-SNAPSHOT", "type" );

        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber( 1 );
        snapshot.setTimestamp( "20050611.202024" );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact, snapshot );
        
        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );
        
        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertFalse( "check there are no failures", failures.hasNext() );
    }

    public void testInvalidSnapshotMetadata()
        throws ReportProcessorException
    {
        ArtifactReporter reporter = new MockArtifactReporter();
        
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "snapshot-artifact", 
                                                                 "1.0-alpha-1-SNAPSHOT", "type" );

        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber( 2 );
        snapshot.setTimestamp( "20050611.202024" );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact, snapshot );
        
        badMetadataReportProcessor.processMetadata( metadata, repository, reporter );
        
        Iterator failures = reporter.getRepositoryMetadataFailureIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        RepositoryMetadataResult result = (RepositoryMetadataResult) failures.next();
        assertEquals( "check metadata", metadata, result.getMetadata() );
        // TODO: should be more robust
        assertEquals( "check reason", "Snapshot artifact 20050611.202024-2 does not exist.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }
    
    private Plugin createMetadataPlugin( String artifactId, String prefix )
    {
        Plugin plugin = new Plugin();
        plugin.setArtifactId( artifactId );
        plugin.setName( artifactId );
        plugin.setPrefix( prefix );
        return plugin;
    }
}
