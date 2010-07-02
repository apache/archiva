package org.apache.archiva.stagerepository.merge.repodetails;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.model.ArtifactMetadata;

import org.apache.maven.archiva.model.ArchivaArtifact;
import org.easymock.MockControl;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SourceArtifactsTest
    extends PlexusInSpringTestCase
{

    private Logger log = LoggerFactory.getLogger( SourceArtifactsTest.class );

    private MockControl metadataResolverControl;

    private MetadataResolver metadataResolver;

    private static final String TEST_REPO_ID = "internal";

    private SourceAritfacts sourceArtifacts;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        sourceArtifacts = new SourceAritfacts();
        sourceArtifacts.setRepoId( TEST_REPO_ID );
        metadataResolverControl = MockControl.createControl( MetadataResolver.class );
        metadataResolver = (MetadataResolver) metadataResolverControl.getMock();
        sourceArtifacts.setMetadataResolver( metadataResolver );
    }

    @Test
    public void testSourceArtifacts()
    {

        metadataResolverControl.expectAndReturn( metadataResolver.getRootNamespaces( TEST_REPO_ID ), getRootNameSpace() );

        metadataResolverControl.expectAndReturn( metadataResolver.getNamespaces( TEST_REPO_ID, "org" ), getNameSpace() );

        metadataResolverControl.expectAndReturn( metadataResolver.getNamespaces( TEST_REPO_ID, "org" + "." + "apache" ),
                                                 getProject() );

        metadataResolverControl.expectAndReturn( metadataResolver.getProjectVersions( TEST_REPO_ID, "org" + "."
            + "apache", "archiva" ), getProjectVersions() );

        metadataResolverControl.expectAndReturn( metadataResolver.getArtifacts( TEST_REPO_ID, "apache", "archiva",
                                                                                "1.6" ), getArtiFactMetaData() );

        metadataResolverControl.expectAndReturn( metadataResolver.getArtifacts( TEST_REPO_ID, "apache", "archiva",
                                                                                "1.6" ), getArtiFactMetaData() );

        metadataResolverControl.replay();

        Collection<ArchivaArtifact> list = sourceArtifacts.getSourceArtifactList();
        assertEquals( false, list.isEmpty() );

        ArrayList<ArtifactMetadata> metadataList =
            (ArrayList) sourceArtifacts.getSourceArtifactsMetaData( list.iterator().next() );
        assertEquals( 2, metadataList.size() );

        metadataResolverControl.verify();

    }

    private Collection<String> getRootNameSpace()
    {
        List<String> artifactList = new ArrayList<String>();
        artifactList.add( "org" );
        return artifactList;
    }

    private Collection<String> getNameSpace()
    {
        List<String> namespace = new ArrayList<String>();
        namespace.add( "apache" );
        return namespace;
    }

    private Collection<String> getProject()
    {
        List<String> namespace = new ArrayList<String>();
        namespace.add( "archiva" );
        return namespace;
    }

    private Collection<String> getProjectVersions()
    {
        List<String> versionList = new ArrayList<String>();
        versionList.add( "1.6" );
        return versionList;
    }

    private Collection<ArtifactMetadata> getArtiFactMetaData()
    {
        List<ArtifactMetadata> metaDataList = new ArrayList<ArtifactMetadata>();
        ArtifactMetadata metaDataOne = new ArtifactMetadata();
        ArtifactMetadata metaDataTwo = new ArtifactMetadata();
        metaDataList.add( metaDataOne );
        metaDataList.add( metaDataTwo );
        return metaDataList;
    }
}
