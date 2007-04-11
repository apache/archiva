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
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
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
        cloned.setDependencyTree( clone( model.getDependencyTree() ) );
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

    public static DependencyEdge clone( DependencyEdge edge )
    {
        if ( edge == null )
        {
            return null;
        }

        DependencyEdge cloned = new DependencyEdge();

        cloned.setFromDependency( clone( edge.getFromDependency() ) );
        cloned.setToDependency( clone( edge.getToDependency() ) );
        cloned.setType( edge.getType() );

        return cloned;
    }

    public static DependencyTree clone( DependencyTree dependencyTree )
    {
        if ( dependencyTree == null )
        {
            return null;
        }

        DependencyTree cloned = new DependencyTree();

        cloned.setDependencyNodes( cloneDependencies( dependencyTree.getDependencyNodes() ) );
        cloned.setDependencyEdges( cloneDependencyEdges( dependencyTree.getDependencyEdges() ) );

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

    public static Properties clone( Properties properties )
    {
        if ( properties == null )
        {
            return null;
        }

        Properties cloned = new Properties();

        Enumeration keys = properties.propertyNames();
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

    public static List cloneArtifactReferences( List artifactReferenceList )
    {
        if ( artifactReferenceList == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = artifactReferenceList.iterator();
        while ( it.hasNext() )
        {
            ArtifactReference artifactReference = (ArtifactReference) it.next();
            ret.add( clone( artifactReference ) );
        }

        return ret;
    }

    public static List cloneDependencies( List dependencies )
    {
        if ( dependencies == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dep = (Dependency) it.next();

            if ( dep == null )
            {
                // Skip null dependency.
                continue;
            }

            ret.add( clone( dep ) );
        }

        return ret;
    }

    public static List cloneDependencyEdges( List dependencyEdges )
    {
        if ( dependencyEdges == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = dependencyEdges.iterator();
        while ( it.hasNext() )
        {
            DependencyEdge edge = (DependencyEdge) it.next();

            if ( edge == null )
            {
                // Skip empty edge.
                continue;
            }

            ret.add( clone( edge ) );
        }

        return ret;
    }

    public static List cloneExclusions( List exclusions )
    {
        if ( exclusions == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = exclusions.iterator();
        while ( it.hasNext() )
        {
            Exclusion exclusion = (Exclusion) it.next();
            Exclusion cloned = new Exclusion();

            cloned.setGroupId( exclusion.getGroupId() );
            cloned.setArtifactId( exclusion.getArtifactId() );

            ret.add( cloned );
        }

        return ret;
    }

    public static List cloneIndividuals( List individuals )
    {
        if ( individuals == null )
        {
            return individuals;
        }

        List ret = new ArrayList();

        Iterator it = individuals.iterator();
        while ( it.hasNext() )
        {
            Individual individual = (Individual) it.next();
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

    public static List cloneLicenses( List licenses )
    {
        if ( licenses == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = licenses.iterator();
        while ( it.hasNext() )
        {
            License license = (License) it.next();
            License cloned = new License();

            cloned.setId( license.getId() );
            cloned.setName( license.getName() );
            cloned.setUrl( license.getUrl() );
            cloned.setComments( license.getComments() );

            ret.add( cloned );
        }

        return ret;
    }

    public static List cloneMailingLists( List mailingLists )
    {
        if ( mailingLists == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = mailingLists.iterator();
        while ( it.hasNext() )
        {
            MailingList mailingList = (MailingList) it.next();

            if ( mailingList == null )
            {
                // Skip null mailing list.
                continue;
            }

            ret.add( clone( mailingList ) );
        }

        return ret;
    }

    public static List clonePlugins( List plugins )
    {
        return cloneArtifactReferences( plugins );
    }

    public static List cloneReports( List reports )
    {
        return cloneArtifactReferences( reports );
    }

    public static List cloneRepositories( List repositories )
    {
        if ( repositories == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = repositories.iterator();
        while ( it.hasNext() )
        {
            ProjectRepository repository = (ProjectRepository) it.next();
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

    public static List cloneRoles( List roles )
    {
        return cloneSimpleStringList( roles );
    }

    private static List cloneSimpleStringList( List simple )
    {
        if ( simple == null )
        {
            return null;
        }

        List ret = new ArrayList();

        Iterator it = simple.iterator();

        while ( it.hasNext() )
        {
            String txt = (String) it.next();
            ret.add( txt );
        }

        return ret;
    }
}
