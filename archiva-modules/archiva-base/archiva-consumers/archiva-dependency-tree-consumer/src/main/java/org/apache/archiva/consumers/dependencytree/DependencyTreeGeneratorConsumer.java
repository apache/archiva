package org.apache.archiva.consumers.dependencytree;

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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.DefaultMavenProjectBuilder;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.StateDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.BuildingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 *                   role-hint="dependency-tree-generator" instantiation-strategy="per-lookup"
 */
public class DependencyTreeGeneratorConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    /** @plexus.configuration */
    private File generatedRepositoryLocation;

    /** @plexus.configuration */
    private File localRepository;

    /** @plexus.requirement */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /** @plexus.requirement */
    private ArtifactFactory artifactFactory;

    /** @plexus.requirement role-hint="maven" */
    private ArtifactMetadataSource artifactMetadataSource;

    /** @plexus.requirement */
    private ArtifactCollector artifactCollector;

    /** @plexus.requirement */
    private MavenProjectBuilder projectBuilder;
    
    /** @plexus.requirement */
    private ArtifactRepositoryFactory artifactRepositoryFactory;

    private String repositoryLocation;

    private final DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

    private ArtifactRepository localArtifactRepository;

    private Field rawProjectCacheField;

    private Field processedProjectCacheField;

    private List<String> includes = Collections.singletonList( "**/*.pom" );

    public String getDescription()
    {
        return "Generate dependency tree metadata for tracking changes across algorithms";
    }

    public String getId()
    {
        return "dependency-tree-generator";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void setGeneratedRepositoryLocation( File generatedRepositoryLocation )
    {
        this.generatedRepositoryLocation = generatedRepositoryLocation;
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered )
        throws ConsumerException
    {
        repositoryLocation = repository.getLocation();

        if ( generatedRepositoryLocation == null )
        {
            generatedRepositoryLocation = new File( repositoryLocation );
        }

        if ( localRepository == null )
        {
            // This is a bit crappy, it would be better to operate entirely within
            // the base repository, but would need to adjust maven-artifact
            localRepository = new File( System.getProperty( "user.home" ), ".m2/repository" );
            try
            {
                localArtifactRepository =
                    artifactRepositoryFactory.createArtifactRepository( "local",
                                                                        localRepository.toURL().toExternalForm(),
                                                                        layout, null, null );
            }
            catch ( MalformedURLException e )
            {
                throw new ConsumerException( e.getMessage(), e );
            }
        }
    }

    public void beginScan( ManagedRepositoryConfiguration repository, Date whenGathered, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        beginScan( repository, whenGathered );    
    }

    public void completeScan()
    {
    }

    public void completeScan( boolean executeOnEntireRepo )
    {
        completeScan();
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        MavenProject project;
        try
        {
            project = projectBuilder.build( new File( repositoryLocation, path ), localArtifactRepository, null, false );

            // manually flush out the cache for memory concerns and more accurate building
            flushProjectCache( projectBuilder );
        }
        catch ( ProjectBuildingException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }

        DependencyNode rootNode;
        try
        {
            // TODO: do this for different values of new ScopeArtifactFilter( scope )
            ArtifactFilter artifactFilter = null;

            rootNode =
                dependencyTreeBuilder.buildDependencyTree( project, localArtifactRepository, artifactFactory,
                                                           artifactMetadataSource, artifactFilter, artifactCollector );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }

        Document document = DocumentHelper.createDocument();
        DependencyNodeVisitor visitor = new XmlSerializingDependencyNodeVisitor( document );

        // TODO: remove the need for this when the serializer can calculate last nodes from visitor calls only
        visitor = new BuildingDependencyNodeVisitor( visitor );

        CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
        DependencyNodeVisitor firstPassVisitor =
            new FilteringDependencyNodeVisitor( collectingVisitor, StateDependencyNodeFilter.INCLUDED );
        rootNode.accept( firstPassVisitor );

        DependencyNodeFilter secondPassFilter = new AncestorOrSelfDependencyNodeFilter( collectingVisitor.getNodes() );
        visitor = new FilteringDependencyNodeVisitor( visitor, secondPassFilter );

        rootNode.accept( visitor );

        FileWriter writer = null;
        try
        {
            Artifact artifact =
                artifactFactory.createProjectArtifact( project.getGroupId(), project.getArtifactId(),
                                                       project.getVersion() );

            String p = layout.pathOf( artifact );
            if ( !p.equals( path ) )
            {
                throw new ConsumerException( "Bad path: " + p + "; should be: " + path );
            }

            File generatedFile = new File( generatedRepositoryLocation, p + ".xml" );
            generatedFile.getParentFile().mkdirs();
            writer = new FileWriter( generatedFile );
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter w = new XMLWriter( writer, format );
            w.write( document );
        }
        catch ( IOException e )
        {
            throw new ConsumerException( e.getMessage(), e );
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }
    }

    public void processFile( String path, boolean executeOnEntireRepo )
        throws ConsumerException
    {
        processFile( path );    
    }

    private void flushProjectCache( MavenProjectBuilder projectBuilder )
    {
        try
        {
            if ( rawProjectCacheField == null )
            {
                rawProjectCacheField = DefaultMavenProjectBuilder.class.getDeclaredField( "rawProjectCache" );
                rawProjectCacheField.setAccessible( true );
            }

            if ( processedProjectCacheField == null )
            {
                processedProjectCacheField =
                    DefaultMavenProjectBuilder.class.getDeclaredField( "processedProjectCache" );
                processedProjectCacheField.setAccessible( true );
            }

            rawProjectCacheField.set( projectBuilder, new HashMap<Object, Object>() );

            processedProjectCacheField.set( projectBuilder, new HashMap<Object, Object>() );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    private static class XmlSerializingDependencyNodeVisitor
        implements DependencyNodeVisitor
    {
        private Element xmlNode;

        public XmlSerializingDependencyNodeVisitor( Document document )
        {
            xmlNode = document.addElement( "tree" );
        }

        // DependencyNodeVisitor methods ------------------------------------------

        /*
         * @see org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor#visit(org.apache.maven.shared.dependency.tree.DependencyNode)
         */
        public boolean visit( DependencyNode node )
        {
            Element dependency = xmlNode.addElement( "dependency" );

            Artifact artifact = node.getArtifact();
            dependency.addElement( "groupId" ).setText( artifact.getGroupId() );
            dependency.addElement( "artifactId" ).setText( artifact.getArtifactId() );
            dependency.addElement( "type" ).setText( artifact.getType() );
            dependency.addElement( "version" ).setText( artifact.getVersion() );
            if ( artifact.getScope() != null )
            {
                dependency.addElement( "scope" ).setText( artifact.getScope() );
            }
            if ( artifact.getClassifier() != null )
            {
                dependency.addElement( "classifier" ).setText( artifact.getClassifier() );
            }

            xmlNode = dependency.addElement( "dependencies" );

            return true;
        }

        /*
         * @see org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor#endVisit(org.apache.maven.shared.dependency.tree.DependencyNode)
         */
        public boolean endVisit( DependencyNode node )
        {
            Element e = xmlNode.getParent();

            if ( !xmlNode.hasContent() )
            {
                e.remove( xmlNode );
            }

            xmlNode = e.getParent();

            return true;
        }
    }

    public void setIncludes( List<String> includes )
    {
        this.includes = includes;
    }
}
