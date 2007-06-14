package org.apache.maven.archiva.plugins.dev;

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

import org.apache.maven.archiva.plugins.dev.testgen.DependencyGraphTestCreator;
import org.apache.maven.archiva.plugins.dev.testgen.MemoryRepositoryCreator;
import org.apache.maven.archiva.plugins.dev.utils.VariableNames;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;

import java.io.File;

/**
 * CreateArchivaDependencyResolutionTestCaseMojo 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @goal generate-dependency-tests
 */
public class CreateArchivaDependencyResolutionTestCaseMojo
    extends AbstractMojo
{
    /**
     * The project of the current build.
     * 
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The artifact respository to use.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;
    
    /**
     * The destination directory to generate the test files.
     * 
     * @parameter expression="${archivadev.outputdir}" default-value="${project.build.directory}"
     * @required
     */
    private File destDir;

    /**
     * The artifact factory to use.
     * 
     * @component
     */
    private ArtifactFactory artifactFactory;
    
    /**
     * @component
     */
    private DependencyTreeBuilder dependencyTreeBuilder;
    
    /**
     * @component
     */
    protected ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     */
    private ArtifactCollector collector;

    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        String classPrefix = VariableNames.toClassName( project.getArtifactId() );
        
        getLog().info( "Generating into " + destDir );
        
        createMemoryRepository( classPrefix );
        createDependencyGraphTest( classPrefix );
    }
    
    private void createDependencyGraphTest( String classPrefix ) throws MojoExecutionException
    {
        DependencyGraphTestCreator creator = new DependencyGraphTestCreator();
        creator.setLog( getLog() );
        creator.setOutputDir( destDir );
        creator.setProject( project );
        creator.setArtifactFactory( artifactFactory );
        creator.setLocalRepository( localRepository );
        creator.setDependencyTreeBuilder( dependencyTreeBuilder );
        creator.setArtifactMetadataSource( artifactMetadataSource );
        creator.setCollector( collector );
        
        creator.create( classPrefix );
    }

    private void createMemoryRepository( String classPrefix ) throws MojoExecutionException
    {
        MemoryRepositoryCreator creator = new MemoryRepositoryCreator();
        creator.setLog( getLog() );
        creator.setOutputDir( destDir );
        creator.setProject( project );
        creator.setArtifactFactory( artifactFactory );
        creator.setLocalRepository( localRepository );
        
        creator.create( classPrefix );
    }
}
