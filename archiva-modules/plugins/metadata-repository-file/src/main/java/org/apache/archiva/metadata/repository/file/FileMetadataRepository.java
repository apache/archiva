package org.apache.archiva.metadata.repository.file;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionFacet;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.Scm;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.MetadataRepository"
 */
public class FileMetadataRepository
    implements MetadataRepository
{
    /**
     * TODO: this isn't suitable for production use
     *
     * @plexus.configuration
     */
    private File directory = new File( System.getProperty( "user.home" ), ".archiva-metadata" );

    /**
     * @plexus.requirement role="org.apache.archiva.metadata.model.MetadataFacetFactory"
     */
    private Map<String, MetadataFacetFactory> metadataFacetFactories;

    private static final Logger log = LoggerFactory.getLogger( FileMetadataRepository.class );

    public void updateProject( String repoId, ProjectMetadata project )
    {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        try
        {
            File projectDirectory =
                new File( this.directory, repoId + "/" + project.getNamespace() + "/" + project.getId() );
            Properties properties = new Properties();
            properties.setProperty( "namespace", project.getNamespace() );
            properties.setProperty( "id", project.getId() );
            writeProperties( properties, projectDirectory );
        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();
        }
    }

    public void updateProjectVersion( String repoId, String namespace, String projectId,
                                      ProjectVersionMetadata versionMetadata )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId );

        Properties properties = new Properties();
        properties.setProperty( "id", versionMetadata.getId() );
        setProperty( properties, "name", versionMetadata.getName() );
        setProperty( properties, "description", versionMetadata.getDescription() );
        setProperty( properties, "url", versionMetadata.getUrl() );
        if ( versionMetadata.getScm() != null )
        {
            setProperty( properties, "scm.connection", versionMetadata.getScm().getConnection() );
            setProperty( properties, "scm.developerConnection", versionMetadata.getScm().getDeveloperConnection() );
            setProperty( properties, "scm.url", versionMetadata.getScm().getUrl() );
        }
        if ( versionMetadata.getCiManagement() != null )
        {
            setProperty( properties, "ci.system", versionMetadata.getCiManagement().getSystem() );
            setProperty( properties, "ci.url", versionMetadata.getCiManagement().getUrl() );
        }
        if ( versionMetadata.getIssueManagement() != null )
        {
            setProperty( properties, "issue.system", versionMetadata.getIssueManagement().getSystem() );
            setProperty( properties, "issue.url", versionMetadata.getIssueManagement().getUrl() );
        }
        if ( versionMetadata.getOrganization() != null )
        {
            setProperty( properties, "org.name", versionMetadata.getOrganization().getName() );
            setProperty( properties, "org.url", versionMetadata.getOrganization().getUrl() );
        }
        int i = 0;
        for ( License license : versionMetadata.getLicenses() )
        {
            setProperty( properties, "license." + i + ".name", license.getName() );
            setProperty( properties, "license." + i + ".url", license.getUrl() );
            i++;
        }
        properties.setProperty( "facetIds", join( versionMetadata.getAllFacetIds() ) );
        for ( ProjectVersionFacet facet : versionMetadata.getAllFacets() )
        {
            properties.putAll( facet.toProperties() );
        }

        try
        {
            writeProperties( properties, new File( directory, versionMetadata.getId() ) );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String join( Collection<String> ids )
    {
        StringBuilder s = new StringBuilder();
        for ( String id : ids )
        {
            s.append( id );
            s.append( "," );
        }
        return s.substring( 0, s.length() - 1 );
    }

    private void setProperty( Properties properties, String name, String value )
    {
        if ( value != null )
        {
            properties.setProperty( name, value );
        }
    }

    public void updateArtifact( String repoId, String namespace, String projectId, String projectVersion,
                                ArtifactMetadata artifact )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readProperties( directory );

        properties.setProperty( "updated:" + artifact.getId(), Long.toString( artifact.getUpdated().getTime() ) );
        properties.setProperty( "size:" + artifact.getId(), Long.toString( artifact.getSize() ) );
        properties.setProperty( "version:" + artifact.getId(), artifact.getVersion() );

        try
        {
            writeProperties( properties, directory );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private Properties readProperties( File directory )
    {
        Properties properties = new Properties();
        FileInputStream in = null;
        try
        {
            in = new FileInputStream( new File( directory, "metadata.properties" ) );
            properties.load( in );
        }
        catch ( FileNotFoundException e )
        {
            // skip - use blank properties
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }
        return properties;
    }

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId );

        Properties properties = readProperties( directory );

        ProjectMetadata project = new ProjectMetadata();
        project.setNamespace( properties.getProperty( "namespace" ) );
        project.setId( properties.getProperty( "id" ) );
        return project;
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readProperties( directory );
        String id = properties.getProperty( "id" );
        ProjectVersionMetadata versionMetadata = null;
        if ( id != null )
        {
            versionMetadata = new ProjectVersionMetadata();
            versionMetadata.setId( id );
            versionMetadata.setName( properties.getProperty( "name" ) );
            versionMetadata.setDescription( properties.getProperty( "description" ) );
            versionMetadata.setUrl( properties.getProperty( "url" ) );

            String scmConnection = properties.getProperty( "scm.connection" );
            String scmDeveloperConnection = properties.getProperty( "scm.developerConnection" );
            String scmUrl = properties.getProperty( "scm.url" );
            if ( scmConnection != null || scmDeveloperConnection != null || scmUrl != null )
            {
                Scm scm = new Scm();
                scm.setConnection( scmConnection );
                scm.setDeveloperConnection( scmDeveloperConnection );
                scm.setUrl( scmUrl );
                versionMetadata.setScm( scm );
            }

            String ciSystem = properties.getProperty( "ci.system" );
            String ciUrl = properties.getProperty( "ci.url" );
            if ( ciSystem != null || ciUrl != null )
            {
                CiManagement ci = new CiManagement();
                ci.setSystem( ciSystem );
                ci.setUrl( ciUrl );
                versionMetadata.setCiManagement( ci );
            }

            String issueSystem = properties.getProperty( "issue.system" );
            String issueUrl = properties.getProperty( "issue.url" );
            if ( issueSystem != null || issueUrl != null )
            {
                IssueManagement issueManagement = new IssueManagement();
                issueManagement.setSystem( ciSystem );
                issueManagement.setUrl( ciUrl );
                versionMetadata.setIssueManagement( issueManagement );
            }

            String orgName = properties.getProperty( "org.name" );
            String orgUrl = properties.getProperty( "org.url" );
            if ( orgName != null || orgUrl != null )
            {
                Organization org = new Organization();
                org.setName( orgName );
                org.setUrl( orgUrl );
                versionMetadata.setOrganization( org );
            }

            boolean done = false;
            int i = 0;
            while ( !done )
            {
                String licenseName = properties.getProperty( "license." + i + ".name" );
                String licenseUrl = properties.getProperty( "license." + i + ".url" );
                if ( licenseName != null || licenseUrl != null )
                {
                    License license = new License();
                    license.setName( licenseName );
                    license.setUrl( licenseUrl );
                    versionMetadata.addLicense( license );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            for ( String facetId : properties.getProperty( "facetIds" ).split( "," ) )
            {
                MetadataFacetFactory factory = metadataFacetFactories.get( facetId );
                if ( factory == null )
                {
                    log.error( "Attempted to load unknown metadata facet: " + facetId );
                }
                else
                {
                    ProjectVersionFacet facet = factory.createProjectVersionFacet();
                    Map<String, String> map = new HashMap<String, String>();
                    for ( String key : properties.stringPropertyNames() )
                    {
                        if ( key.startsWith( facet.getFacetId() ) )
                        {
                            map.put( key, properties.getProperty( key ) );
                        }
                    }
                    facet.fromProperties( map );
                    versionMetadata.addFacet( facet );
                }
            }

            for ( ProjectVersionFacet facet : versionMetadata.getAllFacets() )
            {
                properties.putAll( facet.toProperties() );
            }
        }
        return versionMetadata;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readProperties( directory );

        List<String> versions = new ArrayList<String>();
        for ( Map.Entry entry : properties.entrySet() )
        {
            String name = (String) entry.getKey();
            if ( name.startsWith( "version:" ) )
            {
                versions.add( (String) entry.getValue() );
            }
        }
        return versions;
    }

    private void writeProperties( Properties properties, File directory )
        throws IOException
    {
        directory.mkdirs();
        FileOutputStream os = new FileOutputStream( new File( directory, "metadata.properties" ) );
        try
        {
            properties.store( os, null );
        }
        finally
        {
            IOUtils.closeQuietly( os );
        }
    }

}
