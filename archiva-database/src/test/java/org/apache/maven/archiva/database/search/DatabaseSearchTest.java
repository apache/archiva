package org.apache.maven.archiva.database.search;

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

import org.apache.maven.archiva.database.AbstractArchivaDatabaseTestCase;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.browsing.RepositoryBrowsing;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.Date;
import java.util.List;

/**
 * DatabaseSearchTest
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class DatabaseSearchTest
    extends AbstractArchivaDatabaseTestCase
{
    private ArtifactDAO artifactDao;

    private static final String MD5_HASH1 = "53e3b856aa1a3f3cb7fe0f7ac6163aaf";

    private static final String SHA1_HASH1 = "232f01b24b1617c46a3d4b0ab3415bc9237dcdec";

    private DatabaseSearch dbSearch;

    protected void setUp()
        throws Exception
    {
        super.setUp();
        artifactDao = ( ( ArchivaDAO ) lookup( ArchivaDAO.ROLE, "jdo" ) ).getArtifactDAO();
        dbSearch = (DatabaseSearch) lookup( DatabaseSearch.class.getName() );
    }
    
    public ArchivaArtifact createArtifact( String groupId, String artifactId, String version )
    {
        ArchivaArtifact artifact = artifactDao.createArtifact( groupId, artifactId, version, "", "jar" );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testSearchByChecksum()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "org.apache.maven.test", "test-one", "1.2" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "org.apache.maven.test.foo", "test-two", "1.0" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH1 );
        artifactDao.saveArtifact( artifact );

        List results = dbSearch.searchArtifactsByChecksum( MD5_HASH1 );
        assertEquals( 1, results.size() );

        results = dbSearch.searchArtifactsByChecksum( SHA1_HASH1 );
        assertEquals( 1, results.size() );
    }

}
