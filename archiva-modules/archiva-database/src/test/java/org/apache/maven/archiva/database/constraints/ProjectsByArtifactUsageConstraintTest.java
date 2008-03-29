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
import org.apache.maven.archiva.database.DeclarativeConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.VersionedReference;

import java.util.Date;
import java.util.List;

/**
 * ProjectsByArtifactUsageConstraintTest 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectsByArtifactUsageConstraintTest
    extends AbstractArchivaDatabaseTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();
    }

    private void saveModel( String modelId, String deps[] )
        throws Exception
    {
        ArchivaProjectModel model = new ArchivaProjectModel();
        // Piece together a simple model.
        VersionedReference ref = toVersionedReference( modelId );
        model.setGroupId( ref.getGroupId() );
        model.setArtifactId( ref.getArtifactId() );
        model.setVersion( ref.getVersion() );
        model.setPackaging( "jar" );
        model.setOrigin( "testcase" );

        if ( deps != null )
        {
            for ( int i = 0; i < deps.length; i++ )
            {
                ArtifactReference artiref = toArtifactReference( deps[i] );
                Dependency dep = new Dependency();
                dep.setGroupId( artiref.getGroupId() );
                dep.setArtifactId( artiref.getArtifactId() );
                dep.setVersion( artiref.getVersion() );
                dep.setClassifier( artiref.getClassifier() );
                dep.setClassifier( artiref.getType() );

                model.addDependency( dep );
            }
        }

        dao.getProjectModelDAO().saveProjectModel( model );
    }

    public ArchivaArtifact toArtifact( String id )
    {
        ArtifactReference ref = toArtifactReference( id );

        ArchivaArtifact artifact = new ArchivaArtifact( ref.getGroupId(), ref.getArtifactId(), ref.getVersion(), ref
            .getClassifier(), ref.getType() );
        artifact.getModel().setLastModified( new Date() );
        artifact.getModel().setRepositoryId( "testable_repo" );
        return artifact;
    }

    public void testContraint()
        throws Exception
    {
        saveModel( "org.apache.maven.archiva:archiva-configuration:1.0",
                   new String[] { "org.codehaus.plexus:plexus-digest:1.0::jar" } );

        saveModel( "org.apache.maven.archiva:archiva-common:1.0", new String[] {
            "org.codehaus.plexus:plexus-digest:1.0::jar",
            "junit:junit:3.8.1::jar" } );

        ArchivaArtifact artifact;

        artifact = toArtifact( "org.foo:bar:4.0::jar" );
        assertConstraint( 0, new ProjectsByArtifactUsageConstraint( artifact ) );
        artifact = toArtifact( "org.codehaus.plexus:plexus-digest:1.0::jar" );
        assertConstraint( 2, new ProjectsByArtifactUsageConstraint( artifact ) );
    }

    private void assertConstraint( int expectedHits, DeclarativeConstraint constraint )
        throws Exception
    {
        List results = dao.getProjectModelDAO().queryProjectModels( constraint );
        assertNotNull( "Projects By Artifact Usage: Not Null", results );
        assertEquals( "Projects By Artifact Usage: Results.size", expectedHits, results.size() );
    }
}
