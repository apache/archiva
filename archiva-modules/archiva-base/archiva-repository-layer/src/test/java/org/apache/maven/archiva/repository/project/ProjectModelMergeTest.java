package org.apache.maven.archiva.repository.project;

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
 
import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.archiva.repository.project.ProjectModelMerge;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.VersionedReference;
import org.codehaus.plexus.spring.PlexusInSpringTestCase;

/**
 * ProjectModelMergeTest
 * 
 * @author jzurbano
 */
public class ProjectModelMergeTest
    extends PlexusInSpringTestCase
{
    private ProjectModelMerge modelMerge;
    
    private Enumeration<String> keys;
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        modelMerge = new ProjectModelMerge();
    }
    
    @SuppressWarnings("unchecked")
    public void testPropertiesMerge()
        throws Exception
    {
        ArchivaProjectModel mainProject = createMainProject();
        ArchivaProjectModel parentProject = createParentProject();
    
        assertNotNull( mainProject.getProperties() );
    
        Properties prop = parentProject.getProperties();
        assertNotNull( prop );
    
        keys = (Enumeration<String>) prop.propertyNames();
        assertTrue( keys.hasMoreElements() );
    
        modelMerge.merge( mainProject, parentProject );
    }
    
    private ArchivaProjectModel createMainProject()
    {
        ArchivaProjectModel mainProject = new ArchivaProjectModel();
    
        VersionedReference parent = new VersionedReference();
        parent.setGroupId( "org.apache" );
        parent.setArtifactId( "apache" );
        parent.setVersion( "4" );
    
        mainProject.setParentProject( parent );
        mainProject.setGroupId( "org.apache.servicemix" );
        mainProject.setArtifactId( "servicemix-pom" );
        mainProject.setVersion( "2" );
        mainProject.setPackaging( "pom" );
        mainProject.setName( "ServiceMix POM" );
        mainProject.setUrl( "http://servicemix.apache.org/" );
        mainProject.setDescription( "This pom provides project information that is common to all ServiceMix branches." );
        mainProject.setProperties( new Properties() );
    
        return mainProject;
    }
    
    private ArchivaProjectModel createParentProject()
    {
        ArchivaProjectModel parentProject = new ArchivaProjectModel();
    
        parentProject.setGroupId( "org.apache" );
        parentProject.setArtifactId( "apache" );
        parentProject.setVersion( "4" );
        parentProject.setPackaging( "pom" );
    
        Properties prop = new Properties();
        prop.setProperty( "test.key", "" );
        parentProject.setProperties( prop );
    
        return parentProject;
    }
}
