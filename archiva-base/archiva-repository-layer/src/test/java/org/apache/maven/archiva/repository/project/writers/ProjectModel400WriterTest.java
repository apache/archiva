package org.apache.maven.archiva.repository.project.writers;

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

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.repository.project.ProjectModelWriter;
import org.apache.maven.archiva.repository.project.readers.ProjectModel400Reader;
import org.codehaus.plexus.PlexusTestCase;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.dom4j.Document;
import org.dom4j.DocumentType;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * ProjectModel400WriterTest 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModel400WriterTest
    extends PlexusTestCase
{
    private static final String DEFAULT_REPOSITORY = "src/test/repositories/default-repository";

    private ProjectModelWriter modelWriter;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        modelWriter = (ProjectModelWriter) lookup( ProjectModelWriter.class, "model400" );
    }

    public void testSimpleWrite()
        throws Exception
    {
        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( "org.apache.archiva.test" );
        model.setArtifactId( "simple-model-write" );
        model.setVersion( "1.0" );

        String actualModel = writeToString( model );
        String expectedModel = getExpectedModelString( "model-write-400-simple.pom" );

        assertModelSimilar( expectedModel, actualModel );
    }

    public void testReadWriteSimple()
        throws Exception
    {
        String pathToModel = DEFAULT_REPOSITORY + "/org/apache/maven/A/1.0/A-1.0.pom";
        ArchivaProjectModel model = createArchivaProjectModel( pathToModel );

        String actualModel = writeToString( model );
        String expectedModel = FileUtils.readFileToString( new File( pathToModel ), null );

        assertModelSimilar( expectedModel, actualModel );
    }

    public void testReadWriteComplex()
        throws Exception
    {
        ArchivaProjectModel model = createArchivaProjectModel( DEFAULT_REPOSITORY
            + "/org/apache/maven/maven-parent/4/maven-parent-4.pom" );

        String actualModel = writeToString( model );
        String expectedModel = getExpectedModelString( "maven-parent-4.pom" );

        assertModelSimilar( expectedModel, actualModel );
    }

    private void assertModelSimilar( String expectedModel, String actualModel )
        throws Exception
    {
        Diff diff = new Diff( expectedModel, actualModel );
        DetailedDiff detailedDiff = new DetailedDiff( diff );
        if ( !detailedDiff.similar() )
        {
            // If it isn't similar, dump the difference.
            System.out.println( detailedDiff.toString() );
            System.out.println( "-- Actual Model --\n" + actualModel + "\n---------------\n\n" );
            System.out.println( "-- Expected Model --\n" + expectedModel + "\n---------------\n\n" );

            assertEquals( expectedModel, actualModel );
        }
    }

    private String getExpectedModelString( String pomfilename )
        throws IOException
    {
        File pomFile = getTestFile( "src/test/expected-poms/" + pomfilename );
        return FileUtils.readFileToString( pomFile, null );
    }

    private ArchivaProjectModel createArchivaProjectModel( String path )
        throws ProjectModelException
    {
        ProjectModelReader reader = new ProjectModel400Reader();

        File pomFile = new File( getBasedir(), path );

        return reader.read( pomFile );
    }

    private String writeToString( ArchivaProjectModel model )
        throws ProjectModelException, IOException
    {
        StringWriter writer = new StringWriter();

        modelWriter.write( model, writer );

        return writer.toString();
    }
}
