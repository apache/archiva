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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.repository.consumer.Consumer;
import org.apache.maven.archiva.repository.consumer.ConsumerException;
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
    extends AbstractConsumer
    implements Consumer
{
    public abstract void processRepositoryMetadata( RepositoryMetadata metadata, BaseFile file );

    private static final List includePatterns;

    static
    {
        includePatterns = new ArrayList();
        includePatterns.add( "**/maven-metadata.xml" );
    }

    public List getIncludePatterns()
    {
        return includePatterns;
    }

    public boolean isEnabled()
    {
        // the RepositoryMetadata objects only exist in 'default' layout repositories.
        ArtifactRepositoryLayout layout = repository.getLayout();
        return ( layout instanceof DefaultRepositoryLayout );
    }

    public void processFile( BaseFile file )
        throws ConsumerException
    {
        if ( file.length() <= 0 )
        {
            throw new ConsumerException( file, "File is empty." );
        }

        if ( !file.canRead() )
        {
            throw new ConsumerException( file, "Not allowed to read file due to permission settings on file." );
        }

        RepositoryMetadata metadata = buildMetadata( file );
        processRepositoryMetadata( metadata, file );
    }

    private RepositoryMetadata buildMetadata( BaseFile metadataFile )
        throws ConsumerException
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
            throw new ConsumerException( metadataFile, "Error parsing metadata file: " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( metadataFile, "Error reading metadata file: " + e.getMessage(), e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        RepositoryMetadata repositoryMetadata = buildMetadata( m, metadataFile );

        if ( repositoryMetadata == null )
        {
            throw new ConsumerException( metadataFile, "Unable to build a repository metadata from path." );
        }

        return repositoryMetadata;
    }

    /**
     * Builds a RepositoryMetadata object from a Metadata object and its path.
     *
     * @param m            Metadata
     * @param metadataFile file information
     * @return RepositoryMetadata if the parameters represent one; null if not
     * @throws ConsumerException 
     */
    private RepositoryMetadata buildMetadata( Metadata m, BaseFile metadataFile )
        throws ConsumerException
    {
        if ( artifactFactory == null )
        {
            throw new IllegalStateException( "Unable to build metadata with a null artifactFactory." );
        }

        String metaGroupId = m.getGroupId();
        String metaArtifactId = m.getArtifactId();
        String metaVersion = m.getVersion();

        // check if the groupId, artifactId and version is in the
        // metadataPath
        // parse the path, in reverse order
        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( metadataFile.getRelativePath(), "/\\" );
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
                 */
                throw new ConsumerException( metadataFile,
                                             "Contents of metadata are not appropriate for its location on disk." );
            }
        }

        return metadata;
    }
}
