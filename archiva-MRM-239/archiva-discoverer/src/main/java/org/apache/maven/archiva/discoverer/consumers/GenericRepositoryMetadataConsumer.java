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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.discoverer.DiscovererConsumer;
import org.apache.maven.archiva.discoverer.DiscovererException;
import org.apache.maven.archiva.discoverer.PathUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * GenericRepositoryMetadataConsumer - Consume any maven-metadata.xml files as {@link RepositoryMetadata} objects. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class GenericRepositoryMetadataConsumer
    extends AbstractDiscovererConsumer
    implements DiscovererConsumer
{
    public abstract void processRepositoryMetadata( RepositoryMetadata metadata, File file );

    private static final List includePatterns;

    static
    {
        includePatterns = new ArrayList();
        includePatterns.add( "**/maven-metadata.xml" );
    }

    public GenericRepositoryMetadataConsumer()
    {

    }

    public List getIncludePatterns()
    {
        return includePatterns;
    }

    public String getName()
    {
        return "RepositoryMetadata Consumer";
    }

    public boolean isEnabled()
    {
        // the RepositoryMetadata objects only exist in 'default' layout repositories.
        ArtifactRepositoryLayout layout = repository.getLayout();
        return ( layout instanceof DefaultRepositoryLayout );
    }

    public void processFile( File file )
        throws DiscovererException
    {
        String relpath = PathUtil.getRelative( repository.getBasedir(), file );
        RepositoryMetadata metadata = buildMetadata( file, relpath );
        processRepositoryMetadata( metadata, file );
    }

    private RepositoryMetadata buildMetadata( File metadataFile, String metadataPath )
        throws DiscovererException
    {
        Metadata m;
        Reader reader = null;
        try
        {
            reader = new FileReader( metadataFile );
            MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

            m = metadataReader.read( reader );
        }
        catch ( XmlPullParserException e )
        {
            throw new DiscovererException( "Error parsing metadata file '" + metadataFile + "': " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new DiscovererException( "Error reading metadata file '" + metadataFile + "': " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        RepositoryMetadata repositoryMetadata = buildMetadata( m, metadataPath );

        if ( repositoryMetadata == null )
        {
            throw new DiscovererException( "Unable to build a repository metadata from path" );
        }

        return repositoryMetadata;
    }

    /**
     * Builds a RepositoryMetadata object from a Metadata object and its path.
     *
     * @param m            Metadata
     * @param metadataPath path
     * @return RepositoryMetadata if the parameters represent one; null if not
     * @todo should we just be using the path information, and loading it later when it is needed? (for reporting, etc)
     */
    private RepositoryMetadata buildMetadata( Metadata m, String metadataPath )
    {
        String metaGroupId = m.getGroupId();
        String metaArtifactId = m.getArtifactId();
        String metaVersion = m.getVersion();

        // check if the groupId, artifactId and version is in the
        // metadataPath
        // parse the path, in reverse order
        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( metadataPath, "/\\" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );
        // remove the metadata file
        pathParts.remove( 0 );
        Iterator it = pathParts.iterator();
        String tmpDir = (String) it.next();

        Artifact artifact = null;
        if ( StringUtils.isNotEmpty( metaVersion ) )
        {
            artifact = artifactFactory.createProjectArtifact( metaGroupId, metaArtifactId, metaVersion );
        }
        else
        {
            getLogger().info(
                              "Skipping Create Project Artifact due to no Version defined in [" + m.getGroupId() + ":"
                                  + m.getArtifactId() + ":" + m.getVersion() + "]." );
        }

        // snapshotMetadata
        RepositoryMetadata metadata = null;
        if ( tmpDir != null && tmpDir.equals( metaVersion ) )
        {
            if ( artifact != null )
            {
                metadata = new SnapshotArtifactRepositoryMetadata( artifact );
            }
        }
        else if ( tmpDir != null && tmpDir.equals( metaArtifactId ) )
        {
            // artifactMetadata
            if ( artifact != null )
            {
                metadata = new ArtifactRepositoryMetadata( artifact );
            }
            else
            {
                artifact = artifactFactory.createProjectArtifact( metaGroupId, metaArtifactId, "1.0" );
                metadata = new ArtifactRepositoryMetadata( artifact );
            }
        }
        else
        {
            String groupDir = "";
            int ctr = 0;
            for ( it = pathParts.iterator(); it.hasNext(); )
            {
                String path = (String) it.next();
                if ( ctr == 0 )
                {
                    groupDir = path;
                }
                else
                {
                    groupDir = path + "." + groupDir;
                }
                ctr++;
            }

            // groupMetadata
            if ( metaGroupId != null && metaGroupId.equals( groupDir ) )
            {
                metadata = new GroupRepositoryMetadata( metaGroupId );
            }
            else
            {
                /* If we reached this point, we have some bad metadata.
                 * We have a metadata file, with values for groupId / artifactId / version.
                 * But the information it is providing does not exist relative to the file location.
                 * 
                 * See ${basedir}/src/test/repository/javax/maven-metadata.xml for example
                 * 
                 * TODO: document the bad metadata ??
                 */
            }
        }

        return metadata;
    }
}
