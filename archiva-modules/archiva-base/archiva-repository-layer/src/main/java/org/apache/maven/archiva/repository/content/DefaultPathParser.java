package org.apache.maven.archiva.repository.content;

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

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider;
import org.apache.archiva.metadata.repository.storage.maven2.DefaultArtifactMappingProvider;
import org.apache.archiva.metadata.repository.storage.maven2.Maven2RepositoryPathTranslator;
import org.apache.archiva.metadata.repository.storage.maven2.MavenArtifactFacet;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * DefaultPathParser is a parser for maven 2 (default layout) paths to ArtifactReference.
 *
 * TODO: remove in favour of path translator, this is just delegating for the most part, but won't accommodate other
 * extensions like NPanday
 *
 * @version $Id$
 */
@Service( "pathParser#default" )
public class DefaultPathParser
    implements PathParser
{
    private static final String INVALID_ARTIFACT_PATH = "Invalid path to Artifact: ";

    private RepositoryPathTranslator pathTranslator = new Maven2RepositoryPathTranslator(
        Collections.<ArtifactMappingProvider>singletonList( new DefaultArtifactMappingProvider() ) );

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.archiva.repository.content.PathParser#toArtifactReference(java.lang.String)
     */
    public ArtifactReference toArtifactReference( String path )
        throws LayoutException
    {
        if ( StringUtils.isBlank( path ) )
        {
            throw new LayoutException( "Unable to convert blank path." );
        }

        ArtifactMetadata metadata;
        try
        {
            metadata = pathTranslator.getArtifactForPath( null, path );
        }
        catch ( IllegalArgumentException e )
        {
            throw new LayoutException( e.getMessage(), e );
        }

        ArtifactReference artifact = new ArtifactReference();
        artifact.setGroupId( metadata.getNamespace() );
        artifact.setArtifactId( metadata.getProject() );
        artifact.setVersion( metadata.getVersion() );
        MavenArtifactFacet facet = (MavenArtifactFacet) metadata.getFacet( MavenArtifactFacet.FACET_ID );
        if ( facet != null )
        {
            artifact.setClassifier( facet.getClassifier() );
            artifact.setType( facet.getType() );
        }

        return artifact;
    }

}
