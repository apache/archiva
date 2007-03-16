package org.apache.maven.archiva.consumers;

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

import org.apache.maven.archiva.common.artifact.builder.BuilderException;
import org.apache.maven.archiva.common.artifact.builder.DefaultLayoutArtifactBuilder;
import org.apache.maven.archiva.common.artifact.builder.LayoutArtifactBuilder;
import org.apache.maven.archiva.common.artifact.builder.LegacyLayoutArtifactBuilder;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.repository.ArchivaRepository;
import org.apache.maven.archiva.repository.consumer.Consumer;
import org.apache.maven.archiva.repository.consumer.ConsumerException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DefaultArtifactConsumer 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class GenericArtifactConsumer
    extends AbstractConsumer
    implements Consumer
{
    public abstract void processArtifact( Artifact artifact, BaseFile file );

    private Map artifactBuilders = new HashMap();

    private static final List includePatterns;

    static
    {
        includePatterns = new ArrayList();
        includePatterns.add( "**/*.pom" );
        includePatterns.add( "**/*.jar" );
        includePatterns.add( "**/*.war" );
        includePatterns.add( "**/*.ear" );
        includePatterns.add( "**/*.sar" );
        includePatterns.add( "**/*.zip" );
        includePatterns.add( "**/*.gz" );
        includePatterns.add( "**/*.bz2" );
    }

    private String layoutId = "default";

    public boolean init( ArchivaRepository repository )
    {
        this.artifactBuilders.clear();
        this.artifactBuilders.put( "default", new DefaultLayoutArtifactBuilder( artifactFactory ) );
        this.artifactBuilders.put( "legacy", new LegacyLayoutArtifactBuilder( artifactFactory ) );

        if ( repository.getLayout() instanceof LegacyRepositoryLayout )
        {
            this.layoutId = "legacy";
        }

        return super.init( repository );
    }

    public List getIncludePatterns()
    {
        return includePatterns;
    }

    public boolean isEnabled()
    {
        ArtifactRepositoryLayout layout = repository.getLayout();
        return ( layout instanceof DefaultRepositoryLayout ) || ( layout instanceof LegacyRepositoryLayout );
    }

    public void processFile( BaseFile file )
        throws ConsumerException
    {
        if ( file.length() <= 0 )
        {
            processFileProblem( file, "File is empty." );
        }

        if ( !file.canRead() )
        {
            processFileProblem( file, "Not allowed to read file due to permission settings on file." );
        }

        try
        {
            Artifact artifact = buildArtifact( file );

            processArtifact( artifact, file );
        }
        catch ( BuilderException e )
        {
            throw new ConsumerException( file, e.getMessage(), e );
        }
    }

    private Artifact buildArtifact( BaseFile file )
        throws BuilderException
    {
        LayoutArtifactBuilder builder = (LayoutArtifactBuilder) artifactBuilders.get( layoutId );

        Artifact artifact = builder.build( file.getRelativePath() );
//        artifact.setRepository( repository );
        artifact.setFile( file );

        return artifact;
    }
}
