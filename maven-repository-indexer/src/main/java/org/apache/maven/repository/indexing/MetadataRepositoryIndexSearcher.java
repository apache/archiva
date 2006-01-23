package org.apache.maven.repository.indexing;

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

import org.apache.lucene.document.Document;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class searches the specified index given the search/query criteria.
 */
public class MetadataRepositoryIndexSearcher
    extends AbstractRepositoryIndexSearcher
{
    private ArtifactFactory artifactFactory;

    private static final String FLD_METADATAPATH = "path";

    private static final String FLD_GROUPID = "groupId";

    private static final String FLD_ARTIFACTID = "artifactId";

    private static final String FLD_VERSION = "version";

    private static final String GROUP_TYPE = "GROUP";

    private static final String ARTIFACT_TYPE = "ARTIFACT";

    private static final String SNAPSHOT_TYPE = "SNAPSHOT";

    /**
     * Constructor
     *
     * @param index   the index object to be set
     * @param factory
     */
    public MetadataRepositoryIndexSearcher( MetadataRepositoryIndex index, ArtifactFactory factory )
    {
        super( index );
        artifactFactory = factory;
    }

    /**
     * Create object to be returned by the search based on the document
     *
     * @param doc
     * @return Object
     */
    protected Object createSearchedObjectFromIndexDocument( Document doc )
    {
        List pathParts = new ArrayList();
        StringTokenizer st = new StringTokenizer( doc.get( FLD_METADATAPATH ), "/\\" );
        while ( st.hasMoreTokens() )
        {
            pathParts.add( st.nextToken() );
        }

        Collections.reverse( pathParts );
        Iterator it = pathParts.iterator();
        String metadataFile = (String) it.next();
        String tmpDir = (String) it.next();

        String metadataType = "";
        if ( tmpDir.equals( doc.get( FLD_GROUPID ) ) )
        {
            metadataType = GROUP_TYPE;
        }
        else if ( tmpDir.equals( doc.get( FLD_ARTIFACTID ) ) )
        {
            metadataType = ARTIFACT_TYPE;
        }
        else
        {
            metadataType = SNAPSHOT_TYPE;
        }

        RepositoryMetadata repoMetadata = null;

        try
        {
            repoMetadata = getMetadata( doc.get( FLD_GROUPID ), doc.get( FLD_ARTIFACTID ), doc.get( FLD_VERSION ),
                                        metadataFile, metadataType );
        }
        catch ( Exception e )
        {
            //@todo
        }

        return repoMetadata;
    }

    /**
     * Create RepositoryMetadata object.
     *
     * @param groupId      the groupId to be set
     * @param artifactId   the artifactId to be set
     * @param version      the version to be set
     * @param filename     the name of the metadata file
     * @param metadataType the type of RepositoryMetadata object to be created (GROUP, ARTIFACT or SNAPSHOT)
     * @return RepositoryMetadata
     * @throws Exception
     */
    private RepositoryMetadata getMetadata( String groupId, String artifactId, String version, String filename,
                                            String metadataType )
        throws Exception
    {
        RepositoryMetadata repoMetadata = null;
        URL url;
        InputStream is = null;
        MetadataXpp3Reader metadataReader = new MetadataXpp3Reader();

        //group metadata
        if ( metadataType.equals( GROUP_TYPE ) )
        {
            url = new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new GroupRepositoryMetadata( groupId );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //artifact metadata
        else if ( metadataType.equals( ARTIFACT_TYPE ) )
        {
            url = new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" + artifactId + "/" +
                filename ).toURL();
            is = url.openStream();
            repoMetadata = new ArtifactRepositoryMetadata( getArtifact( groupId, artifactId, version ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }
        //snapshot/version metadata
        else if ( metadataType.equals( SNAPSHOT_TYPE ) )
        {
            url = new File( index.getRepository().getBasedir() + groupId.replace( '.', '/' ) + "/" + artifactId + "/" +
                version + "/" + filename ).toURL();
            is = url.openStream();
            repoMetadata = new SnapshotArtifactRepositoryMetadata( getArtifact( groupId, artifactId, version ) );
            repoMetadata.setMetadata( metadataReader.read( new InputStreamReader( is ) ) );
        }

        return repoMetadata;
    }

    /**
     * Create artifact object.
     *
     * @param groupId    the groupId of the artifact
     * @param artifactId the artifactId of the artifact
     * @param version    the version of the artifact
     * @return Artifact
     * @throws Exception
     */
    private Artifact getArtifact( String groupId, String artifactId, String version )
        throws Exception
    {
        return artifactFactory.createBuildArtifact( groupId, artifactId, version, "jar" );
    }
}
