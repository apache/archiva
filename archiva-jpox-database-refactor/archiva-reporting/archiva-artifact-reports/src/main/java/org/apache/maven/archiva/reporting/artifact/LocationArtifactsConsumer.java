package org.apache.maven.archiva.reporting.artifact;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ArchivaArtifactConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.util.SelectorUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Validate the location of the artifact based on the values indicated
 * in its pom (both the pom packaged with the artifact & the pom in the
 * file system).
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.consumers.ArchivaArtifactConsumer"
 *                   role-hint="validate-artifacts-location"
 */
public class LocationArtifactsConsumer
    extends AbstractMonitoredConsumer
    implements ArchivaArtifactConsumer, RegistryListener, Initializable
{
    /**
     * @plexus.configuration default-value="duplicate-artifacts"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Check for Duplicate Artifacts via SHA1 Checksums"
     */
    private String description;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    private Map repositoryMap = new HashMap();

    private List includes = new ArrayList();

    public String getId()
    {
        return id;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan()
    {
        /* do nothing */
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List getIncludedTypes()
    {
        return null;
    }

    /**
     * Check whether the artifact is in its proper location. The location of the artifact
     * is validated first against the groupId, artifactId and versionId in the specified model
     * object (pom in the file system). Then unpack the artifact (jar file) and get the model (pom)
     * included in the package. If a model exists inside the package, then check if the artifact's
     * location is valid based on the location specified in the pom. Check if the both the location
     * specified in the file system pom and in the pom included in the package is the same.
     */
    public void processArchivaArtifact( ArchivaArtifact artifact )
        throws ConsumerException
    {
        ArchivaRepository repository = findRepository( artifact );
        if ( !repository.isManaged() )
        {
            getLogger().warn( "Artifact Location Validation Cannot operate against a non-managed Repository." );
            return;
        }

        File artifactFile = new File( repository.getUrl().toString(), toPath( artifact ) );
        ArchivaProjectModel fsModel = readFilesystemModel( artifactFile );
        ArchivaProjectModel embeddedModel = readEmbeddedModel( artifact, artifactFile );

        validateAppropriateModel( "Filesystem", artifact, fsModel );
        validateAppropriateModel( "Embedded", artifact, embeddedModel );
    }

    private void validateAppropriateModel( String location, ArchivaArtifact artifact, ArchivaProjectModel model )
        throws ConsumerException
    {
        if ( model != null )
        {
            if ( !StringUtils.equals( model.getGroupId(), artifact.getGroupId() ) )
            {
                addProblem( artifact, "The groupId of the " + location
                    + " project model doesn't match with the artifact, expected <" + artifact.getGroupId()
                    + ">, but was actually <" + model.getGroupId() + ">" );
            }
            
            if ( !StringUtils.equals( model.getArtifactId(), artifact.getArtifactId() ) )
            {
                addProblem( artifact, "The artifactId of the " + location
                    + " project model doesn't match with the artifact, expected <" + artifact.getArtifactId()
                    + ">, but was actually <" + model.getArtifactId() + ">" );
            }
            
            if ( !StringUtils.equals( model.getVersion(), artifact.getVersion() ) )
            {
                addProblem( artifact, "The version of the " + location
                    + " project model doesn't match with the artifact, expected <" + artifact.getVersion()
                    + ">, but was actually <" + model.getVersion() + ">" );
            }
        }
    }

    private ArchivaProjectModel readEmbeddedModel( ArchivaArtifact artifact, File artifactFile )
        throws ConsumerException
    {
        try
        {
            JarFile jar = new JarFile( artifactFile );

            // Get the entry and its input stream.
            JarEntry expectedEntry = jar.getJarEntry( "META-INF/maven/" + artifact.getGroupId() + "/"
                + artifact.getArtifactId() + "/pom.xml" );

            if ( expectedEntry != null )
            {
                // TODO: read and resolve model here.
                return null;
            }
            
            /* Expected Entry not found, look for alternate that might
             * indicate that the artifact is, indeed located in the wrong place.
             */

            List actualPomXmls = findJarEntryPattern( jar, "META-INF/maven/**/pom.xml" );
            if ( actualPomXmls.isEmpty() )
            {
                // No check needed.

            }
            
            // TODO: test for invalid actual pom.xml
            // TODO: test
        }
        catch ( IOException e )
        {
            // Not able to read from the file.
            String emsg = "Unable to read file contents: " + e.getMessage();
            addProblem( artifact, emsg );
        }

        return null;
    }

    private List findJarEntryPattern( JarFile jar, String pattern )
    {
        List hits = new ArrayList();

        Enumeration entries = jar.entries();
        while ( entries.hasMoreElements() )
        {
            JarEntry entry = (JarEntry) entries.nextElement();
            if ( SelectorUtils.match( pattern, entry.getName() ) )
            {
                hits.add( entry );
            }
        }

        return hits;
    }

    private void addProblem( ArchivaArtifact artifact, String msg )
        throws ConsumerException
    {
        RepositoryProblem problem = new RepositoryProblem();
        problem.setRepositoryId( artifact.getModel().getRepositoryId() );
        problem.setPath( toPath( artifact ) );
        problem.setGroupId( artifact.getGroupId() );
        problem.setArtifactId( artifact.getArtifactId() );
        problem.setVersion( artifact.getVersion() );
        problem.setType( LocationArtifactsReport.PROBLEM_TYPE_BAD_ARTIFACT_LOCATION );
        problem.setOrigin( getId() );
        problem.setMessage( msg );

        try
        {
            dao.getRepositoryProblemDAO().saveRepositoryProblem( problem );
        }
        catch ( ArchivaDatabaseException e )
        {
            String emsg = "Unable to save problem with artifact location to DB: " + e.getMessage();
            getLogger().warn( emsg, e );
            throw new ConsumerException( emsg, e );
        }
    }

    private ArchivaProjectModel readFilesystemModel( File artifactFile )
    {
        File pomFile = createPomFileReference( artifactFile );

        // TODO: read and resolve model here.
        
        return null;
    }

    private File createPomFileReference( File artifactFile )
    {
        String pomFilename = artifactFile.getAbsolutePath();

        int pos = pomFilename.lastIndexOf( '.' );
        if ( pos <= 0 )
        {
            // Invalid filename.
            return null;
        }

        pomFilename = pomFilename.substring( 0, pos ) + ".pom";
        return new File( pomFilename );
    }

    private ArchivaRepository findRepository( ArchivaArtifact artifact )
    {
        return (ArchivaRepository) this.repositoryMap.get( artifact.getModel().getRepositoryId() );
    }

    private String toPath( ArchivaArtifact artifact )
    {
        try
        {
            BidirectionalRepositoryLayout layout = layoutFactory.getLayout( artifact );
            return layout.toPath( artifact );
        }
        catch ( LayoutException e )
        {
            getLogger().warn( "Unable to calculate path for artifact: " + artifact );
            return null;
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
        {
            initRepositoryMap();
        }

        if ( ConfigurationNames.isRepositoryScanning( propertyName ) )
        {
            initIncludes();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initIncludes()
    {
        includes.clear();

        FileType artifactTypes = configuration.getConfiguration().getRepositoryScanning().getFileTypeById( "artifacts" );
        if ( artifactTypes != null )
        {
            includes.addAll( artifactTypes.getPatterns() );
        }
    }

    private void initRepositoryMap()
    {
        synchronized ( this.repositoryMap )
        {
            this.repositoryMap.clear();

            Iterator it = configuration.getConfiguration().createRepositoryMap().entrySet().iterator();
            while ( it.hasNext() )
            {
                Map.Entry entry = (Entry) it.next();
                String key = (String) entry.getKey();
                RepositoryConfiguration repoConfig = (RepositoryConfiguration) entry.getValue();
                ArchivaRepository repository = ArchivaConfigurationAdaptor.toArchivaRepository( repoConfig );
                this.repositoryMap.put( key, repository );
            }
        }
    }

    public void initialize()
        throws InitializationException
    {
        initRepositoryMap();
        initIncludes();
        configuration.addChangeListener( this );
    }
}
