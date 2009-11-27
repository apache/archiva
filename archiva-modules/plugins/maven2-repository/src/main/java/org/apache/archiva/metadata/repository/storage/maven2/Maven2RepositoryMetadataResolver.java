package org.apache.archiva.metadata.repository.storage.maven2;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.model.ProjectVersionReference;
import org.apache.archiva.metadata.repository.MetadataResolver;
import org.apache.archiva.metadata.repository.MetadataResolverException;
import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Scm;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.MetadataResolver" role-hint="maven2"
 */
public class Maven2RepositoryMetadataResolver
    implements MetadataResolver
{
    /**
     * @plexus.requirement
     */
    private ModelBuilder builder;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role-hint="maven2"
     */
    private RepositoryPathTranslator pathTranslator;

    private final static Logger log = LoggerFactory.getLogger( Maven2RepositoryMetadataResolver.class );

    public ProjectMetadata getProject( String repoId, String namespace, String projectId )
    {
        throw new UnsupportedOperationException();
    }

    public ProjectVersionMetadata getProjectVersion( String repoId, String namespace, String projectId,
                                                     String projectVersion )
        throws MetadataResolverException
    {
        ManagedRepositoryConfiguration repositoryConfiguration =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repoId );

        String artifactVersion = projectVersion;

        File basedir = new File( repositoryConfiguration.getLocation() );
        if ( VersionUtil.isSnapshot( projectVersion ) )
        {
            File metadataFile =
                pathTranslator.toFile( basedir, namespace, projectId, projectVersion, "maven-metadata.xml" );
            try
            {
                MavenRepositoryMetadata metadata = MavenRepositoryMetadataReader.read( metadataFile );

                // re-adjust to timestamp if present, otherwise retain the original -SNAPSHOT filename
                MavenRepositoryMetadata.Snapshot snapshotVersion = metadata.getSnapshotVersion();
                if ( snapshotVersion != null )
                {
                    artifactVersion =
                        artifactVersion.substring( 0, artifactVersion.length() - 8 ); // remove SNAPSHOT from end
                    artifactVersion =
                        artifactVersion + snapshotVersion.getTimestamp() + "-" + snapshotVersion.getBuildNumber();
                }
            }
            catch ( XMLException e )
            {
                // unable to parse metadata - log it, and continue with the version as the original SNAPSHOT version
                log.warn( "Invalid metadata: " + metadataFile + " - " + e.getMessage() );
            }
        }

        File file = pathTranslator.toFile( basedir, namespace, projectId, projectVersion,
                                           projectId + "-" + artifactVersion + ".pom" );

        if ( !file.exists() )
        {
            // metadata could not be resolved
            return null;
        }

        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins( false );
        req.setPomFile( file );
        req.setModelResolver( new RepositoryModelResolver( basedir, pathTranslator ) );
        req.setValidationLevel( ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL );

        Model model;
        try
        {
            model = builder.build( req ).getEffectiveModel();
        }
        catch ( ModelBuildingException e )
        {
            throw new MetadataResolverException( "Unable to build Maven POM to derive metadata from: " + e.getMessage(),
                                                 e );
        }

        ProjectVersionMetadata metadata = new ProjectVersionMetadata();
        metadata.setCiManagement( convertCiManagement( model.getCiManagement() ) );
        metadata.setDescription( model.getDescription() );
        metadata.setId( projectVersion );
        metadata.setIssueManagement( convertIssueManagement( model.getIssueManagement() ) );
        metadata.setLicenses( convertLicenses( model.getLicenses() ) );
        metadata.setMailingLists( convertMailingLists( model.getMailingLists() ) );
        metadata.setDependencies( convertDependencies( model.getDependencies() ) );
        metadata.setName( model.getName() );
        metadata.setOrganization( convertOrganization( model.getOrganization() ) );
        metadata.setScm( convertScm( model.getScm() ) );
        metadata.setUrl( model.getUrl() );

        MavenProjectFacet facet = new MavenProjectFacet();
        facet.setGroupId( model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId() );
        facet.setArtifactId( model.getArtifactId() );
        facet.setPackaging( model.getPackaging() );
        if ( model.getParent() != null )
        {
            MavenProjectParent parent = new MavenProjectParent();
            parent.setGroupId( model.getParent().getGroupId() );
            parent.setArtifactId( model.getParent().getArtifactId() );
            parent.setVersion( model.getParent().getVersion() );
            facet.setParent( parent );
        }
        metadata.addFacet( facet );

        return metadata;
    }

    private List<org.apache.archiva.metadata.model.Dependency> convertDependencies( List<Dependency> dependencies )
    {
        List<org.apache.archiva.metadata.model.Dependency> l =
            new ArrayList<org.apache.archiva.metadata.model.Dependency>();
        for ( Dependency dependency : dependencies )
        {
            org.apache.archiva.metadata.model.Dependency newDependency =
                new org.apache.archiva.metadata.model.Dependency();
            newDependency.setArtifactId( dependency.getArtifactId() );
            newDependency.setClassifier( dependency.getClassifier() );
            newDependency.setGroupId( dependency.getGroupId() );
            newDependency.setOptional( dependency.isOptional() );
            newDependency.setScope( dependency.getScope() );
            newDependency.setSystemPath( dependency.getSystemPath() );
            newDependency.setType( dependency.getType() );
            newDependency.setVersion( dependency.getVersion() );
            l.add( newDependency );
        }
        return l;
    }

    private org.apache.archiva.metadata.model.Scm convertScm( Scm scm )
    {
        org.apache.archiva.metadata.model.Scm newScm = null;
        if ( scm != null )
        {
            newScm = new org.apache.archiva.metadata.model.Scm();
            newScm.setConnection( scm.getConnection() );
            newScm.setDeveloperConnection( scm.getDeveloperConnection() );
            newScm.setUrl( scm.getUrl() );
        }
        return newScm;
    }

    private org.apache.archiva.metadata.model.Organization convertOrganization( Organization organization )
    {
        org.apache.archiva.metadata.model.Organization org = null;
        if ( organization != null )
        {
            org = new org.apache.archiva.metadata.model.Organization();
            org.setName( organization.getName() );
            org.setUrl( organization.getUrl() );
        }
        return org;
    }

    private List<org.apache.archiva.metadata.model.License> convertLicenses( List<License> licenses )
    {
        List<org.apache.archiva.metadata.model.License> l = new ArrayList<org.apache.archiva.metadata.model.License>();
        for ( License license : licenses )
        {
            org.apache.archiva.metadata.model.License newLicense = new org.apache.archiva.metadata.model.License();
            newLicense.setName( license.getName() );
            newLicense.setUrl( license.getUrl() );
            l.add( newLicense );
        }
        return l;
    }

    private List<org.apache.archiva.metadata.model.MailingList> convertMailingLists( List<MailingList> mailingLists )
    {
        List<org.apache.archiva.metadata.model.MailingList> l =
            new ArrayList<org.apache.archiva.metadata.model.MailingList>();
        for ( MailingList mailingList : mailingLists )
        {
            org.apache.archiva.metadata.model.MailingList newMailingList =
                new org.apache.archiva.metadata.model.MailingList();
            newMailingList.setName( mailingList.getName() );
            newMailingList.setMainArchiveUrl( mailingList.getArchive() );
            newMailingList.setPostAddress( mailingList.getPost() );
            newMailingList.setSubscribeAddress( mailingList.getSubscribe() );
            newMailingList.setUnsubscribeAddress( mailingList.getUnsubscribe() );
            newMailingList.setOtherArchives( mailingList.getOtherArchives() );
            l.add( newMailingList );
        }
        return l;
    }

    private org.apache.archiva.metadata.model.IssueManagement convertIssueManagement( IssueManagement issueManagement )
    {
        org.apache.archiva.metadata.model.IssueManagement im = null;
        if ( issueManagement != null )
        {
            im = new org.apache.archiva.metadata.model.IssueManagement();
            im.setSystem( issueManagement.getSystem() );
            im.setUrl( issueManagement.getUrl() );
        }
        return im;
    }

    private org.apache.archiva.metadata.model.CiManagement convertCiManagement( CiManagement ciManagement )
    {
        org.apache.archiva.metadata.model.CiManagement ci = null;
        if ( ciManagement != null )
        {
            ci = new org.apache.archiva.metadata.model.CiManagement();
            ci.setSystem( ciManagement.getSystem() );
            ci.setUrl( ciManagement.getUrl() );
        }
        return ci;
    }

    // TODO: evidence that storage and resolver != repository API - split the interface up
    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId,
                                                   String projectVersion )
    {
        throw new UnsupportedOperationException();
    }

    public Collection<ProjectVersionReference> getProjectReferences( String repoId, String namespace, String projectId,
                                                                     String projectVersion )
    {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getRootNamespaces( String repoId )
    {
        throw new UnsupportedOperationException();
    }

    public List<String> getNamespaces( String repoId, String namespace )
    {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getProjects( String repoId, String namespace )
    {
        throw new UnsupportedOperationException();
    }

    public Collection<String> getProjectVersions( String repoId, String namespace, String projectId )
    {
        throw new UnsupportedOperationException();
    }
}
