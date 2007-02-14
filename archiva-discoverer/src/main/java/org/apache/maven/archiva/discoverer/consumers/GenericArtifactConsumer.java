package org.apache.maven.archiva.discoverer.consumers;

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

import org.apache.maven.archiva.discoverer.DiscovererConsumer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.PathUtil;
import org.apache.maven.archiva.discoverer.builders.BuilderException;
import org.apache.maven.archiva.discoverer.builders.DefaultLayoutArtifactBuilder;
import org.apache.maven.archiva.discoverer.builders.LayoutArtifactBuilder;
import org.apache.maven.archiva.discoverer.builders.LegacyLayoutArtifactBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.layout.LegacyRepositoryLayout;

import java.io.File;
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
    extends AbstractDiscovererConsumer
    implements DiscovererConsumer
{
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

    public GenericArtifactConsumer()
    {
    }

    public boolean init( ArtifactRepository repository )
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

    public abstract void processArtifact( Artifact artifact, File file );

    public abstract void processArtifactBuildFailure( File path, String message );

    public List getIncludePatterns()
    {
        return includePatterns;
    }

    public String getName()
    {
        return "Artifact Consumer";
    }

    public boolean isEnabled()
    {
        ArtifactRepositoryLayout layout = repository.getLayout();
        return ( layout instanceof DefaultRepositoryLayout ) || ( layout instanceof LegacyRepositoryLayout );
    }

    public void processFile( File file )
        throws DiscovererException
    {
        try
        {
            Artifact artifact = buildArtifact( repository.getBasedir(), file.getAbsolutePath() );

            processArtifact( artifact, file );
        }
        catch ( BuilderException e )
        {
            processArtifactBuildFailure( file, e.getMessage() );
        }
    }

    /**
     * @see org.apache.maven.archiva.discoverer.ArtifactDiscoverer#buildArtifact(String)
     */
    private Artifact buildArtifact( String repoBaseDir, String path )
        throws BuilderException, DiscovererException
    {
        LayoutArtifactBuilder builder = (LayoutArtifactBuilder) artifactBuilders.get( layoutId );

        String relativePath = PathUtil.getRelative( repoBaseDir, path );

        Artifact artifact = builder.build( relativePath );
        artifact.setRepository( repository );
        artifact.setFile( new File( repository.getBasedir(), path ) );

        return artifact;
    }
}
