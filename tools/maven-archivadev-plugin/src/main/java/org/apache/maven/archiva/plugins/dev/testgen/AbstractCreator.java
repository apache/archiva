package org.apache.maven.archiva.plugins.dev.testgen;

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

import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.interpolation.ModelInterpolator;
import org.apache.maven.project.interpolation.RegexBasedModelInterpolator;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * AbstractCreator 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractCreator
{
    protected MavenProject project;

    protected ArtifactFactory artifactFactory;

    protected ArtifactRepository localRepository;

    protected File outputDir;

    private MavenXpp3Reader modelReader = new MavenXpp3Reader();
    
    private ModelInterpolator modelInterpolator;

    private Log log;

    public abstract void create( String classPrefix )
        throws MojoExecutionException;

    protected void writeLicense( PrintWriter out )
    {
        out.println( "/*" );
        out.println( " * Licensed to the Apache Software Foundation (ASF) under one" );
        out.println( " * or more contributor license agreements.  See the NOTICE file" );
        out.println( " * distributed with this work for additional information" );
        out.println( " * regarding copyright ownership.  The ASF licenses this file" );
        out.println( " * to you under the Apache License, Version 2.0 (the" );
        out.println( " * \"License\"); you may not use this file except in compliance" );
        out.println( " * with the License.  You may obtain a copy of the License at" );
        out.println( " *" );
        out.println( " *  http://www.apache.org/licenses/LICENSE-2.0" );
        out.println( " *" );
        out.println( " * Unless required by applicable law or agreed to in writing," );
        out.println( " * software distributed under the License is distributed on an" );
        out.println( " * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY" );
        out.println( " * KIND, either express or implied.  See the License for the" );
        out.println( " * specific language governing permissions and limitations" );
        out.println( " * under the License." );
        out.println( " */" );
        out.println( "" );
    }

    protected boolean isNotEmpty( Properties properties )
    {
        return !isEmpty( properties );
    }

    private boolean isEmpty( Properties properties )
    {
        if ( properties == null )
        {
            return true;
        }

        return properties.isEmpty();
    }

    public Log getLog()
    {
        return log;
    }

    public void setLog( Log log )
    {
        this.log = log;
    }

    public MavenProject getProject()
    {
        return project;
    }

    public void setProject( MavenProject project )
    {
        this.project = project;
    }

    protected Model getModel( Dependency dep )
    {
        return getModel( dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), dep.getType() );
    }

    protected Model getModel( Parent parent )
    {
        return getModel( parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), "pom" );
    }

    protected Model getModel( String groupId, String artifactId, String version, String type )
    {
        // getLog().info( ".getModel(" + groupId + ", " + artifactId + ", " + version + ", " + type + ")" );
        Artifact pomArtifact = artifactFactory.createProjectArtifact( groupId, artifactId, version );

        String path = localRepository.getLayout().pathOf( pomArtifact );

        File pomFile = new File( localRepository.getBasedir(), path );

        if ( pomFile.exists() )
        {
            FileReader reader = null;
            try
            {
                reader = new FileReader( pomFile );
                Model model = modelReader.read( reader );
                
                // HACK: to allow ${pom.groupId} expressions to work, WITHOUT resolving/merginc parent.
                //       (The merging of parent pom is done elsewhere)
                if ( StringUtils.isEmpty( model.getGroupId() ) )
                {
                    if( model.getParent() != null )
                    {
                        model.setGroupId( model.getParent().getGroupId() );
                    }
                }
                
                // HACK: to fix bad poms in repo (see jetty:jetty:4.2.10::jar for example)
                model.setVersion( version );
                
                // Interpolate the properties?
                if( modelInterpolator == null )
                {
                    modelInterpolator = new RegexBasedModelInterpolator();
                }
                
                model = modelInterpolator.interpolate( model, Collections.EMPTY_MAP, false );
                
                return model;
            }
            catch ( Exception e )
            {
                getLog().warn(
                               "Unable to read pom file " + pomFile.getAbsolutePath() + " : (" + e.getClass().getName()
                                   + "): " + e.getMessage() );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        // no pom file.

        Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setPackaging( type );

        return model;
    }

    private String getGroupId( Model model )
    {
        String groupId = model.getGroupId();

        if ( StringUtils.isEmpty( groupId ) )
        {
            if ( model.getParent() != null )
            {
                groupId = model.getParent().getGroupId();
            }
        }

        return groupId;
    }

    private String getVersion( Model model )
    {
        String version = model.getVersion();

        if ( StringUtils.isEmpty( version ) )
        {
            if ( model.getParent() != null )
            {
                version = model.getParent().getVersion();
            }
        }

        return VersionUtil.getBaseVersion( version );
    }

    protected boolean isNotBlank( String str )
    {
        return !isBlank( str );
    }

    private boolean isBlank( String str )
    {
        if ( str == null )
        {
            return true;
        }

        return ( str.trim().length() <= 0 );
    }

    protected boolean isNotEmpty( Collection coll )
    {
        return !isEmpty( coll );
    }

    protected boolean isEmpty( Collection coll )
    {
        if ( coll == null )
        {
            return true;
        }

        return coll.isEmpty();
    }

    protected String toKey( Parent ref )
    {
        StringBuffer key = new StringBuffer();

        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() ).append( ":" );
        key.append( ref.getVersion() );

        return key.toString();
    }

    protected String toKey( Exclusion ref )
    {
        StringBuffer key = new StringBuffer();

        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() );

        return key.toString();
    }

    protected String toKey( Model ref )
    {
        StringBuffer key = new StringBuffer();

        key.append( getGroupId( ref ) ).append( ":" );
        key.append( ref.getArtifactId() ).append( ":" );
        key.append( getVersion( ref ) );

        return key.toString();
    }

    protected String toKey( Dependency ref )
    {
        StringBuffer key = new StringBuffer();

        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() ).append( ":" );
        key.append( ref.getVersion() ).append( ":" );
        key.append( StringUtils.defaultString( ref.getClassifier() ) ).append( ":" );
        key.append( ref.getType() );

        return key.toString();
    }

    protected String toKey( Artifact ref )
    {
        StringBuffer key = new StringBuffer();

        key.append( ref.getGroupId() ).append( ":" );
        key.append( ref.getArtifactId() ).append( ":" );
        key.append( ref.getVersion() ).append( ":" );
        key.append( StringUtils.defaultString( ref.getClassifier() ) ).append( ":" );
        key.append( ref.getType() );

        return key.toString();
    }

    public ArtifactFactory getArtifactFactory()
    {
        return artifactFactory;
    }

    public void setArtifactFactory( ArtifactFactory artifactFactory )
    {
        this.artifactFactory = artifactFactory;
    }

    public ArtifactRepository getLocalRepository()
    {
        return localRepository;
    }

    public void setLocalRepository( ArtifactRepository localRepository )
    {
        this.localRepository = localRepository;
    }

    public File getOutputDir()
    {
        return outputDir;
    }

    public void setOutputDir( File outputDir )
    {
        this.outputDir = outputDir;
    }
}
