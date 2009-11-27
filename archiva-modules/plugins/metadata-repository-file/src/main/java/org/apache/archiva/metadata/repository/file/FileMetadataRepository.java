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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.CiManagement;
import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.IssueManagement;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.MetadataFacetFactory;
import org.apache.archiva.metadata.model.Organization;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionFacet;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
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

    private static final String PROJECT_METADATA_KEY = "project-metadata";

    private static final String PROJECT_VERSION_METADATA_KEY = "version-metadata";

    private static final String NAMESPACE_METADATA_KEY = "namespace-metadata";

    public void updateProject( String repoId, ProjectMetadata project )
    {
        updateProject( repoId, project.getNamespace(), project.getId() );
    }

    private void updateProject( String repoId, String namespace, String id )
    {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        try
        {
            File namespaceDirectory = new File( this.directory, repoId + "/" + namespace );
            Properties properties = new Properties();
            properties.setProperty( "namespace", namespace );
            writeProperties( properties, namespaceDirectory, NAMESPACE_METADATA_KEY );

            properties.setProperty( "id", id );
            writeProperties( properties, new File( namespaceDirectory, id ), PROJECT_METADATA_KEY );

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
        updateProject( repoId, namespace, projectId );

        File directory =
            new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + versionMetadata.getId() );

        Properties properties = readProperties( directory, PROJECT_VERSION_METADATA_KEY );
        // remove properties that are not references or artifacts
        for ( String name : properties.stringPropertyNames() )
        {
            if ( !name.startsWith( "artifact:" ) && !name.startsWith( "ref:" ) )
            {
                properties.remove( name );
            }
        }
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
        i = 0;
        for ( MailingList mailingList : versionMetadata.getMailingLists() )
        {
            setProperty( properties, "mailingList." + i + ".archive", mailingList.getMainArchiveUrl() );
            setProperty( properties, "mailingList." + i + ".name", mailingList.getName() );
            setProperty( properties, "mailingList." + i + ".post", mailingList.getPostAddress() );
            setProperty( properties, "mailingList." + i + ".unsubscribe", mailingList.getUnsubscribeAddress() );
            setProperty( properties, "mailingList." + i + ".subscribe", mailingList.getSubscribeAddress() );
            setProperty( properties, "mailingList." + i + ".otherArchives", join( mailingList.getOtherArchives() ) );
            i++;
        }
        i = 0;
        for ( Dependency dependency : versionMetadata.getDependencies() )
        {
            setProperty( properties, "dependency." + i + ".classifier", dependency.getClassifier() );
            setProperty( properties, "dependency." + i + ".scope", dependency.getScope() );
            setProperty( properties, "dependency." + i + ".systemPath", dependency.getSystemPath() );
            setProperty( properties, "dependency." + i + ".artifactId", dependency.getArtifactId() );
            setProperty( properties, "dependency." + i + ".groupId", dependency.getGroupId() );
            setProperty( properties, "dependency." + i + ".version", dependency.getVersion() );
            setProperty( properties, "dependency." + i + ".type", dependency.getType() );
            i++;
        }
        properties.setProperty( "facetIds", join( versionMetadata.getAllFacetIds() ) );
        for ( ProjectVersionFacet facet : versionMetadata.getAllFacets() )
        {
            properties.putAll( facet.toProperties() );
        }

        try
        {
            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void updateProjectReference( String repoId, String namespace, String projectId, String projectVersion,
                                        ProjectVersionReference reference )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readProperties( directory, PROJECT_VERSION_METADATA_KEY );
        int i = Integer.valueOf( properties.getProperty( "ref:lastReferenceNum", "-1" ) ) + 1;
        setProperty( properties, "ref:lastReferenceNum", Integer.toString( i ) );
        setProperty( properties, "ref:reference." + i + ".namespace", reference.getNamespace() );
        setProperty( properties, "ref:reference." + i + ".projectId", reference.getProjectId() );
        setProperty( properties, "ref:reference." + i + ".projectVersion", reference.getProjectVersion() );
        setProperty( properties, "ref:reference." + i + ".referenceType", reference.getReferenceType().toString() );

        try
        {
            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private String join( Collection<String> ids )
    {
        if ( !ids.isEmpty() )
        {
            StringBuilder s = new StringBuilder();
            for ( String id : ids )
            {
                s.append( id );
                s.append( "," );
            }
            return s.substring( 0, s.length() - 1 );
        }
        return "";
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

        Properties properties = readProperties( directory, PROJECT_VERSION_METADATA_KEY );

        properties.setProperty( "artifact:updated:" + artifact.getId(),
                                Long.toString( artifact.getUpdated().getTime() ) );
        properties.setProperty( "artifact:size:" + artifact.getId(), Long.toString( artifact.getSize() ) );
        properties.setProperty( "artifact:version:" + artifact.getId(), artifact.getVersion() );

        try
        {
            writeProperties( properties, directory, PROJECT_VERSION_METADATA_KEY );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private Properties readProperties( File directory, String propertiesKey )
    {
        Properties properties = new Properties();
        FileInputStream in = null;
        try
        {
            in = new FileInputStream( new File( directory, propertiesKey + ".properties" ) );
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

        Properties properties = readProperties( directory, PROJECT_VERSION_METADATA_KEY );

        ProjectMetadata project = new ProjectMetadata();
        project.setNamespace( properties.getProperty( "namespace" ) );
        project.setId( properties.getProperty( "id" ) );
        return project;
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readProperties( directory, PROJECT_VERSION_METADATA_KEY );
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

            done = false;
            i = 0;
            while ( !done )
            {
                String mailingListName = properties.getProperty( "mailingList." + i + ".name" );
                if ( mailingListName != null )
                {
                    MailingList mailingList = new MailingList();
                    mailingList.setName( mailingListName );
                    mailingList.setMainArchiveUrl( properties.getProperty( "mailingList." + i + ".archive" ) );
                    mailingList.setOtherArchives(
                        Arrays.asList( properties.getProperty( "mailingList." + i + ".otherArchives" ).split( "," ) ) );
                    mailingList.setPostAddress( properties.getProperty( "mailingList." + i + ".post" ) );
                    mailingList.setSubscribeAddress( properties.getProperty( "mailingList." + i + ".subscribe" ) );
                    mailingList.setUnsubscribeAddress( properties.getProperty( "mailingList." + i + ".unsubscribe" ) );
                    versionMetadata.addMailingList( mailingList );
                }
                else
                {
                    done = true;
                }
                i++;
            }

            done = false;
            i = 0;
            while ( !done )
            {
                String dependencyArtifactId = properties.getProperty( "dependency." + i + ".artifactId" );
                if ( dependencyArtifactId != null )
                {
                    Dependency dependency = new Dependency();
                    dependency.setArtifactId( dependencyArtifactId );
                    dependency.setGroupId( properties.getProperty( "dependency." + i + ".groupId" ) );
                    dependency.setClassifier( properties.getProperty( "dependency." + i + ".classifier" ) );
                    dependency.setOptional(
                        Boolean.valueOf( properties.getProperty( "dependency." + i + ".optional" ) ) );
                    dependency.setScope( properties.getProperty( "dependency." + i + ".scope" ) );
                    dependency.setSystemPath( properties.getProperty( "dependency." + i + ".systemPath" ) );
                    dependency.setType( properties.getProperty( "dependency." + i + ".type" ) );
                    dependency.setVersion( properties.getProperty( "dependency." + i + ".version" ) );
                    versionMetadata.addDependency( dependency );
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

        Properties properties = readProperties( directory, PROJECT_VERSION_METADATA_KEY );

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

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + projectVersion );

        Properties properties = readProperties( directory, PROJECT_VERSION_METADATA_KEY );
        int numberOfRefs = Integer.valueOf( properties.getProperty( "ref:lastReferenceNum", "-1" ) ) + 1;

        List<ProjectVersionReference> references = new ArrayList<ProjectVersionReference>();
        for ( int i = 0; i < numberOfRefs; i++ )
        {
            ProjectVersionReference reference = new ProjectVersionReference();
            reference.setProjectId( properties.getProperty( "ref:reference." + i + ".projectId" ) );
            reference.setNamespace( properties.getProperty( "ref:reference." + i + ".namespace" ) );
            reference.setProjectVersion( properties.getProperty( "ref:reference." + i + ".projectVersion" ) );
            reference.setReferenceType( ProjectVersionReference.ReferenceType.valueOf(
                properties.getProperty( "ref:reference." + i + ".referenceType" ) ) );
            references.add( reference );
        }
        return references;
    }

    public Collection<String> getRootNamespaces( String repoId )
    {
        return getNamespaces( repoId, null );
    }

    public Collection<String> getNamespaces( String repoId, String baseNamespace )
    {
        List<String> allNamespaces = new ArrayList<String>();
        File directory = new File( this.directory, repoId );
        File[] files = directory.listFiles();
        if ( files != null )
        {
            for ( File namespace : files )
            {
                if ( new File( namespace, NAMESPACE_METADATA_KEY + ".properties" ).exists() )
                {
                    allNamespaces.add( namespace.getName() );
                }
            }
        }

        Set<String> namespaces = new LinkedHashSet<String>();
        int fromIndex = baseNamespace != null ? baseNamespace.length() + 1 : 0;
        for ( String namespace : allNamespaces )
        {
            if ( baseNamespace == null || namespace.startsWith( baseNamespace + "." ) )
            {
                int i = namespace.indexOf( '.', fromIndex );
                if ( i >= 0 )
                {
                    namespaces.add( namespace.substring( fromIndex, i ) );
                }
                else
                {
                    namespaces.add( namespace.substring( fromIndex ) );
                }
            }
        }
        return new ArrayList<String>( namespaces );
    }

    public Collection<String> getProjects( String repoId, String namespace )
    {
        List<String> projects = new ArrayList<String>();
        File directory = new File( this.directory, repoId + "/" + namespace );
        File[] files = directory.listFiles();
        if ( files != null )
        {
            for ( File project : files )
            {
                if ( new File( project, PROJECT_METADATA_KEY + ".properties" ).exists() )
                {
                    projects.add( project.getName() );
                }
            }
        }
        return projects;
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        List<String> projectVersions = new ArrayList<String>();
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId );
        File[] files = directory.listFiles();
        if ( files != null )
        {
            for ( File projectVersion : files )
            {
                if ( new File( projectVersion, PROJECT_VERSION_METADATA_KEY + ".properties" ).exists() )
                {
                    projectVersions.add( projectVersion.getName() );
                }
            }
        }
        return projectVersions;
    }

    private void writeProperties( Properties properties, File directory, String propertiesKey )
        throws IOException
    {
        directory.mkdirs();
        FileOutputStream os = new FileOutputStream( new File( directory, propertiesKey + ".properties" ) );
        try
        {
            properties.store( os, null );
        }
        finally
        {
            IOUtils.closeQuietly( os );
        }
    }

    public void setDirectory( File directory )
    {
        this.directory = directory;
    }
}
