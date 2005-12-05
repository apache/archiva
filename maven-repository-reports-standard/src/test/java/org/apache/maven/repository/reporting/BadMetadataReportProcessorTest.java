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
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

import java.util.Iterator;

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

    public void testProcessMetadata()
    {
    }

    public void testCheckPluginMetadata()
    {
    }

    public void testCheckSnapshotMetadata()
    {
    }

    public void testCheckMetadataVersions()
    {
    }

    public void testCheckRepositoryVersions()
    {
    }

}
