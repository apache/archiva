package org.apache.maven.archiva.database.constraints;

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
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.Date;
import java.util.List;

/**
 * ArtifactsByChecksumConstraintTest
 *
 * @version
 */
public class ArtifactsByChecksumConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    private static final String SHA1_HASH3 = "f3f653289f3217c65324830ab3415bc92feddefa";

    private static final String SHA1_HASH2 = "a49810ad3eba8651677ab57cd40a0f76fdef9538";

    private static final String SHA1_HASH1 = "232f01b24b1617c46a3d4b0ab3415bc9237dcdec";

    private static final String MD5_HASH3 = "5440efd724c9a5246ddc148662a4f20a";

    private static final String MD5_HASH2 = "4685525525d82dea68c6a6cd5a08f726";

    private static final String MD5_HASH1 = "53e3b856aa1a3f3cb7fe0f7ac6163aaf";

    private ArtifactDAO artifactDao;


    protected void setUp()
        throws Exception
    {
        super.setUp();

        ArchivaDAO dao = (ArchivaDAO) lookup( ArchivaDAO.ROLE, "jdo" );
        artifactDao = dao.getArtifactDAO();
    }

    public ArchivaArtifact createArtifact( String artifactId, String version )
    {
        ArchivaArtifact artifact =
            artifactDao.createArtifact( "org.apache.maven.archiva.test", artifactId, version, "", "jar" );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testConstraintSHA1()
        throws Exception
    {
        ArchivaArtifact artifact;

        // Setup artifacts in fresh DB.
        artifact = createArtifact( "test-sha1-one", "1.0" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-sha1-one", "1.1" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-sha1-one", "1.2" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-sha1-two", "1.0" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-sha1-two", "2.0" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH3 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-sha1-two", "2.1" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH2 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-sha1-two", "3.0" );
        artifact.getModel().setChecksumSHA1( SHA1_HASH2 );
        artifactDao.saveArtifact( artifact );

        assertConstraint( "Artifacts by SHA1 Checksum", 4,
                          new ArtifactsByChecksumConstraint( SHA1_HASH1, ArtifactsByChecksumConstraint.SHA1 ) );
        assertConstraint( "Artifacts by SHA1 Checksum", 2,
                          new ArtifactsByChecksumConstraint( SHA1_HASH2, ArtifactsByChecksumConstraint.SHA1 ) );
        assertConstraint( "Artifacts by SHA1 Checksum", 1,
                          new ArtifactsByChecksumConstraint( SHA1_HASH3, ArtifactsByChecksumConstraint.SHA1 ) );
    }

    public void testConstraintMD5()
        throws Exception
    {
        ArchivaArtifact artifact;

        artifact = createArtifact( "test-md5-one", "1.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-md5-one", "1.1" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-md5-one", "1.2" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-md5-two", "1.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-md5-two", "2.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH3 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-md5-two", "2.1" );
        artifact.getModel().setChecksumMD5( MD5_HASH2 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-md5-two", "3.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH2 );
        artifactDao.saveArtifact( artifact );

        assertConstraint( "Artifacts by MD5 Checksum", 4,
                          new ArtifactsByChecksumConstraint( MD5_HASH1, ArtifactsByChecksumConstraint.MD5 ) );
        assertConstraint( "Artifacts by MD5 Checksum", 2,
                          new ArtifactsByChecksumConstraint( MD5_HASH2, ArtifactsByChecksumConstraint.MD5 ) );
        assertConstraint( "Artifacts by MD5 Checksum", 1,
                          new ArtifactsByChecksumConstraint( MD5_HASH3, ArtifactsByChecksumConstraint.MD5 ) );
    }

    public void testConstraintOR()
        throws Exception
    {
        ArchivaArtifact artifact;

        artifact = createArtifact( "test-one", "1.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-one", "1.1" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-one", "1.2" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "1.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH1 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "2.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH3 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "2.1" );
        artifact.getModel().setChecksumMD5( MD5_HASH2 );
        artifactDao.saveArtifact( artifact );

        artifact = createArtifact( "test-two", "3.0" );
        artifact.getModel().setChecksumMD5( MD5_HASH2 );
        artifactDao.saveArtifact( artifact );

        assertConstraint( "Artifacts by MD5 Checksum", 4, new ArtifactsByChecksumConstraint( MD5_HASH1 ) );
        assertConstraint( "Artifacts by MD5 Checksum", 2, new ArtifactsByChecksumConstraint( MD5_HASH2 ) );
        assertConstraint( "Artifacts by MD5 Checksum", 1, new ArtifactsByChecksumConstraint( MD5_HASH3 ) );
    }


    private void assertConstraint( String msg, int count, ArtifactsByChecksumConstraint constraint )
        throws Exception
    {
        List results = artifactDao.queryArtifacts( constraint );
        assertNotNull( msg + ": Not Null", results );
        assertEquals( msg + ": Results.size", count, results.size() );
    }
}
