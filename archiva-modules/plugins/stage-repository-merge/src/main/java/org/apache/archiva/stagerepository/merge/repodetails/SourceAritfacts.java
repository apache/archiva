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

import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.storage.maven2.MavenArtifactFacet;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.archiva.metadata.model.ArtifactMetadata;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @plexus.component role="org.apache.archiva.stagerepository.merge.repodetails.SourceAritfacts"
 */
public class SourceAritfacts
{

    /**
     * @plexus.requirement
     */    
    private MetadataResolver metadataResolver;

    private ArrayList<String> rootNameSpacesList;

    private ArrayList<String> gruopIdList;

    private ArrayList<String> artifactsList;

    private ArrayList<String> artifactsVersionsList;

    private List<ArchivaArtifact> artifactsListWithDetails;

    private String repoId;

    private static final Logger log = LoggerFactory.getLogger( SourceAritfacts.class );

    public String getRepoId()
    {
        return repoId;
    }

    public void setRepoId( String repoId )
    {
        this.repoId = repoId;
    }

    // this methos returns a ArtifactMetaData List.(i guess no harm due to hardcoding the Artifact type and version)
    public Collection<ArtifactMetadata> getSourceArtifactsMetaData( ArchivaArtifact artifact )
    {

        return metadataResolver.getArtifacts( artifact.getRepositoryId(), artifact.getGroupId(),
                                              artifact.getArtifactId(), artifact.getVersion() );
    }

    public Collection<ArchivaArtifact> getSourceArtifactList()
    {
        artifactsListWithDetails = new ArrayList<ArchivaArtifact>();

        process();

        return artifactsListWithDetails;
    }

    public void setMetadataResolver( MetadataResolver metadataResolver )
    {
        this.metadataResolver = metadataResolver;
    }

    private void process()
    {
        // this will get the root name spaces eg : org, com
        rootNameSpacesList = (ArrayList<String>) metadataResolver.getRootNamespaces( repoId );

        gruopIdList = new ArrayList<String>();

        artifactsList = new ArrayList<String>();

        // following iterates through the root name spaces list and get the gruo id of relavet root name spaces.
        for ( String namespace : rootNameSpacesList )
        {
            // this will get the gruop id list of relavant name space . eg : org > archiva(gruop id)
            gruopIdList = (ArrayList<String>) metadataResolver.getNamespaces( repoId, namespace );

            // following will iterates through the particular gruop id 's
            for ( String gruopId : gruopIdList )
            {
                // parse the parameters "repoId" and "namespace + gruop id "to artifacts list. eg : params = ("internal"
                // , "org.archiva")
                artifactsList = (ArrayList<String>) metadataResolver.getNamespaces( repoId, namespace + "." + gruopId );

                for ( String artifact : artifactsList )
                {
                    // iterates through the artifacts and get the available versions of a particular artifact
                    artifactsVersionsList =
                        (ArrayList<String>) metadataResolver.getProjectVersions( repoId, namespace + "." + gruopId,
                                                                                 artifact );

                    for ( String version : artifactsVersionsList )
                    {
                        // assign gathered attributes Artifact object and add it in to the list
                        artifactsListWithDetails.addAll( getArtifactMetadata( repoId, gruopId, artifact, version ) );

                    }
                }
            }
        }

    }

    private Collection<ArchivaArtifact> getArtifactMetadata( String repoId, String gruopId, String artifactId,
                                                             String version )
    {
        MavenArtifactFacet facet = null;

        List<ArchivaArtifact> artifactList = new ArrayList<ArchivaArtifact>();

        ArrayList<ArtifactMetadata> artifactMetaDataList =
            (ArrayList<ArtifactMetadata>) metadataResolver.getArtifacts( repoId, gruopId, artifactId, version );

        for ( ArtifactMetadata artifactMetadata : artifactMetaDataList )
        {

            facet = (MavenArtifactFacet) artifactMetadata.getFacet( MavenArtifactFacet.FACET_ID );

            if ( facet != null )
            {
                artifactList.add( new ArchivaArtifact( gruopId, artifactId, version, facet.getClassifier(),
                                                       facet.getType(), repoId ) );

            }
            else
            {
                artifactList.add( new ArchivaArtifact( gruopId, artifactId, version, "", "jar", repoId ) );
            }

        }
        return artifactList;
    }
}
