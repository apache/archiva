package org.apache.maven.repository.discovery;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class gets all the paths that contain the metadata files.
 *
 * @plexus.component role="org.apache.maven.repository.discovery.MetadataDiscoverer" role-hint="org.apache.maven.repository.discovery.DefaultMetadataDiscoverer"
 */
public class DefaultMetadataDiscoverer
    extends AbstractArtifactDiscoverer
    implements MetadataDiscoverer
{
    /**
     * @plexus.requirement
     */
    private ArtifactFactory artifactFactory;

    /**
     * Standard patterns to include in discovery of metadata files.
     */
    private static final String[] STANDARD_DISCOVERY_INCLUDES = {"**/*-metadata.xml", "**/*/*-metadata.xml",
        "**/*/*/*-metadata.xml", "**/*-metadata-*.xml", "**/*/*-metadata-*.xml", "**/*/*/*-metadata-*.xml"};

    /**
     * Search the repository for metadata files.
     *
     * @param repositoryBase
     * @param blacklistedPatterns
     */
    public List discoverMetadata( File repositoryBase, String blacklistedPatterns )
    {
        List metadataFiles = new ArrayList();
        String[] metadataPaths =
            scanForArtifactPaths( repositoryBase, blacklistedPatterns, STANDARD_DISCOVERY_INCLUDES, null );

        for ( int i = 0; i < metadataPaths.length; i++ )
        {
            RepositoryMetadata metadata = buildMetadata( repositoryBase.getPath(), metadataPaths[i] );

            if ( metadata != null )
            {
                metadataFiles.add( metadata );
            }
            else
            {
                addKickedOutPath( metadataPaths[i] );
            }
        }

        return metadataFiles;
    }

    /**
     * Create RepositoryMetadata object.
     *
     * @param repo         The path to the repository.
     * @param metadataPath The path to the metadata file.
     * @return the metadata
     */
    private RepositoryMetadata buildMetadata( String repo, String metadataPath )
    {
        RepositoryMetadata metadata = null;

        try
        {
            URL url = new File( repo + "/" + metadataPath ).toURL();
            InputStream is = url.openStream();
            Reader reader = new InputStreamReader( is );
            MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

            Metadata m = metadataReader.read( reader );
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

            //ArtifactHandler handler = new DefaultArtifactHandler( "jar" );
            //if( metaVersion != null && !metaVersion.equals( "" ) )
            //{
            //   VersionRange version = VersionRange.createFromVersion( metaVersion );
            //}

            Artifact artifact = null;
            if ( metaVersion != null && !metaVersion.equals( "" ) )
            {
                artifact = artifactFactory.createBuildArtifact( metaGroupId, metaArtifactId, metaVersion, "jar" );
            }

            // snapshotMetadata
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
            }

        }
        catch ( FileNotFoundException fe )
        {
            // TODO: log ignored metadata
        }
        catch ( XmlPullParserException xe )
        {
            // TODO: log ignored metadata
        }
        catch ( MalformedURLException e )
        {
            // TODO: log ignored metadata
        }
        catch ( IOException ie )
        {
            // TODO: log ignored metadata
        }

        return metadata;
    }
}
