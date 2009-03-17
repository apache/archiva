package org.apache.maven.archiva.model;

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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Utility methods for cloning various Archiva Model objects. 
 *
 * @version $Id$
 */
public class ArchivaModelCloner
{
    public static ArchivaProjectModel clone( ArchivaProjectModel model )
    {
        if ( model == null )
        {
            return null;
        }

        ArchivaProjectModel cloned = new ArchivaProjectModel();

        cloned.setGroupId( model.getGroupId() );
        cloned.setArtifactId( model.getArtifactId() );
        cloned.setVersion( model.getVersion() );

        cloned.setParentProject( clone( model.getParentProject() ) );

        cloned.setName( model.getName() );
        cloned.setDescription( model.getDescription() );
        cloned.setUrl( model.getUrl() );
        cloned.setPackaging( model.getPackaging() );
        cloned.setOrigin( model.getOrigin() );

        cloned.setMailingLists( cloneMailingLists( model.getMailingLists() ) );
        cloned.setCiManagement( clone( model.getCiManagement() ) );
        cloned.setIndividuals( cloneIndividuals( model.getIndividuals() ) );
        cloned.setIssueManagement( clone( model.getIssueManagement() ) );
        cloned.setLicenses( cloneLicenses( model.getLicenses() ) );
        cloned.setOrganization( clone( model.getOrganization() ) );
        cloned.setScm( clone( model.getScm() ) );
        cloned.setRepositories( cloneRepositories( model.getRepositories() ) );
        cloned.setDependencies( cloneDependencies( model.getDependencies() ) );
        cloned.setPlugins( clonePlugins( model.getPlugins() ) );
        cloned.setReports( cloneReports( model.getReports() ) );
        cloned.setDependencyManagement( cloneDependencies( model.getDependencyManagement() ) );

        return cloned;
    }

    public static ArtifactReference clone( ArtifactReference artifactReference )
    {
        if ( artifactReference == null )
        {
            return null;
        }

        ArtifactReference cloned = new ArtifactReference();

        cloned.setGroupId( artifactReference.getGroupId() );
        cloned.setArtifactId( artifactReference.getArtifactId() );
        cloned.setVersion( artifactReference.getVersion() );
        cloned.setClassifier( artifactReference.getClassifier() );
        cloned.setType( artifactReference.getType() );

        return cloned;
    }

    public static CiManagement clone( CiManagement ciManagement )
    {
        if ( ciManagement == null )
        {
            return null;
        }

        CiManagement cloned = new CiManagement();

        cloned.setSystem( ciManagement.getSystem() );
        cloned.setUrl( ciManagement.getUrl() );

        return cloned;
    }

    public static Dependency clone( Dependency dependency )
    {
        if ( dependency == null )
        {
            return null;
        }

        Dependency cloned = new Dependency();

        // Identification
        cloned.setGroupId( dependency.getGroupId() );
        cloned.setArtifactId( dependency.getArtifactId() );
        cloned.setVersion( dependency.getVersion() );
        cloned.setClassifier( dependency.getClassifier() );
        cloned.setType( dependency.getType() );

        // The rest.
        cloned.setTransitive( dependency.isTransitive() );
        cloned.setScope( dependency.getScope() );
        cloned.setOptional( dependency.isOptional() );
        cloned.setSystemPath( dependency.getSystemPath() );
        cloned.setUrl( dependency.getUrl() );
        cloned.setExclusions( cloneExclusions( dependency.getExclusions() ) );

        return cloned;
    }

    public static IssueManagement clone( IssueManagement issueManagement )
    {
        if ( issueManagement == null )
        {
            return null;
        }

        IssueManagement cloned = new IssueManagement();

        cloned.setSystem( issueManagement.getSystem() );
        cloned.setUrl( issueManagement.getUrl() );

        return cloned;
    }

    public static MailingList clone( MailingList mailingList )
    {
        if ( mailingList == null )
        {
            return null;
        }

        MailingList cloned = new MailingList();

        cloned.setName( mailingList.getName() );
        cloned.setSubscribeAddress( mailingList.getSubscribeAddress() );
        cloned.setUnsubscribeAddress( mailingList.getUnsubscribeAddress() );
        cloned.setPostAddress( mailingList.getPostAddress() );
        cloned.setMainArchiveUrl( mailingList.getMainArchiveUrl() );
        cloned.setOtherArchives( cloneSimpleStringList( mailingList.getOtherArchives() ) );

        return cloned;
    }

    public static Organization clone( Organization organization )
    {
        if ( organization == null )
        {
            return null;
        }

        Organization cloned = new Organization();

        cloned.setFavicon( organization.getFavicon() );
        cloned.setName( organization.getName() );
        cloned.setUrl( organization.getUrl() );

        return cloned;
    }

    @SuppressWarnings("unchecked")
    public static Properties clone( Properties properties )
    {
        if ( properties == null )
        {
            return null;
        }

        Properties cloned = new Properties();

        Enumeration<String> keys = (Enumeration<String>) properties.propertyNames();
        while ( keys.hasMoreElements() )
        {
            String key = (String) keys.nextElement();
            String value = properties.getProperty( key );
            cloned.setProperty( key, value );
        }

        return cloned;
    }

    public static Scm clone( Scm scm )
    {
        if ( scm == null )
        {
            return null;
        }

        Scm cloned = new Scm();

        cloned.setConnection( scm.getConnection() );
        cloned.setDeveloperConnection( scm.getDeveloperConnection() );
        cloned.setUrl( scm.getUrl() );

        return cloned;
    }

    public static SnapshotVersion clone( SnapshotVersion snapshotVersion )
    {
        if ( snapshotVersion == null )
        {
            return null;
        }

        SnapshotVersion cloned = new SnapshotVersion();

        cloned.setTimestamp( snapshotVersion.getTimestamp() );
        cloned.setBuildNumber( snapshotVersion.getBuildNumber() );

        return cloned;
    }

    public static VersionedReference clone( VersionedReference versionedReference )
    {
        if ( versionedReference == null )
        {
            return null;
        }

        VersionedReference cloned = new VersionedReference();

        cloned.setGroupId( versionedReference.getGroupId() );
        cloned.setArtifactId( versionedReference.getArtifactId() );
        cloned.setVersion( versionedReference.getVersion() );

        return cloned;
    }

    public static List<ArtifactReference> cloneArtifactReferences( List<ArtifactReference> artifactReferenceList )
    {
        if ( artifactReferenceList == null )
        {
            return null;
        }

        List<ArtifactReference> ret = new ArrayList<ArtifactReference>();

        for ( ArtifactReference ref : artifactReferenceList )
        {
            ret.add( clone( ref ) );
        }

        return ret;
    }

    public static List<Dependency> cloneDependencies( List<Dependency> dependencies )
    {
        if ( dependencies == null )
        {
            return null;
        }

        List<Dependency> ret = new ArrayList<Dependency>();

        for ( Dependency dep : dependencies )
        {
            if ( dep == null )
            {
                // Skip null dependency.
                continue;
            }

            ret.add( clone( dep ) );
        }

        return ret;
    }

    public static List<Exclusion> cloneExclusions( List<Exclusion> exclusions )
    {
        if ( exclusions == null )
        {
            return null;
        }

        List<Exclusion> ret = new ArrayList<Exclusion>();

        for ( Exclusion exclusion : exclusions )
        {
            Exclusion cloned = new Exclusion();

            cloned.setGroupId( exclusion.getGroupId() );
            cloned.setArtifactId( exclusion.getArtifactId() );

            ret.add( cloned );
        }

        return ret;
    }

    public static List<Individual> cloneIndividuals( List<Individual> individuals )
    {
        if ( individuals == null )
        {
            return individuals;
        }

        List<Individual> ret = new ArrayList<Individual>();

        Iterator<Individual> it = individuals.iterator();
        while ( it.hasNext() )
        {
            Individual individual = it.next();
            Individual cloned = new Individual();

            cloned.setPrincipal( individual.getPrincipal() );

            cloned.setEmail( individual.getEmail() );
            cloned.setName( individual.getName() );
            cloned.setOrganization( individual.getOrganization() );
            cloned.setOrganizationUrl( individual.getOrganizationUrl() );
            cloned.setUrl( individual.getUrl() );
            cloned.setTimezone( individual.getTimezone() );

            cloned.setRoles( cloneRoles( individual.getRoles() ) );
            cloned.setProperties( clone( individual.getProperties() ) );

            ret.add( cloned );
        }

        return ret;
    }

    public static List<License> cloneLicenses( List<License> licenses )
    {
        if ( licenses == null )
        {
            return null;
        }

        List<License> ret = new ArrayList<License>();

        for ( License license : licenses )
        {
            License cloned = new License();

            cloned.setId( license.getId() );
            cloned.setName( license.getName() );
            cloned.setUrl( license.getUrl() );
            cloned.setComments( license.getComments() );

            ret.add( cloned );
        }

        return ret;
    }

    public static List<MailingList> cloneMailingLists( List<MailingList> mailingLists )
    {
        if ( mailingLists == null )
        {
            return null;
        }

        List<MailingList> ret = new ArrayList<MailingList>();

        for ( MailingList mailingList : mailingLists )
        {
            if ( mailingList == null )
            {
                // Skip null mailing list.
                continue;
            }

            ret.add( clone( mailingList ) );
        }

        return ret;
    }

    public static List<ArtifactReference> clonePlugins( List<ArtifactReference> plugins )
    {
        return cloneArtifactReferences( plugins );
    }

    public static List<ArtifactReference> cloneReports( List<ArtifactReference> reports )
    {
        return cloneArtifactReferences( reports );
    }

    public static List<ProjectRepository> cloneRepositories( List<ProjectRepository> repositories )
    {
        if ( repositories == null )
        {
            return null;
        }

        List<ProjectRepository> ret = new ArrayList<ProjectRepository>();

        for ( ProjectRepository repository : repositories )
        {
            ProjectRepository cloned = new ProjectRepository();

            cloned.setId( repository.getId() );
            cloned.setName( repository.getName() );
            cloned.setUrl( repository.getUrl() );
            cloned.setLayout( repository.getLayout() );
            cloned.setPlugins( repository.isPlugins() );
            cloned.setReleases( repository.isReleases() );
            cloned.setSnapshots( repository.isSnapshots() );

            ret.add( cloned );
        }

        return ret;
    }

    public static List<String> cloneRoles( List<String> roles )
    {
        return cloneSimpleStringList( roles );
    }

    private static List<String> cloneSimpleStringList( List<String> simple )
    {
        if ( simple == null )
        {
            return null;
        }

        List<String> ret = new ArrayList<String>();

        for ( String txt : simple )
        {
            ret.add( txt );
        }

        return ret;
    }

    public static List<String> cloneAvailableVersions( List<String> availableVersions )
    {
        return cloneSimpleStringList( availableVersions );
    }
}
