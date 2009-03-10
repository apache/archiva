package org.apache.maven.archiva.repository.project.readers;

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.Exclusion;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.MailingList;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.ProjectRepository;
import org.apache.maven.archiva.model.Scm;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;
import org.dom4j.Element;

/**
 * ProjectModel400Reader - read in modelVersion 4.0.0 pom files into archiva-model structures.
 *
 * @version $Id$
 */
public class ProjectModel400Reader
    implements ProjectModelReader
{
    public ArchivaProjectModel read( File pomFile )
        throws XMLException
    {
        XMLReader xml = new XMLReader( "project", pomFile );

        ArchivaProjectModel model = new ArchivaProjectModel();

        if ( !"http://maven.apache.org/POM/4.0.0".equals( xml.getDefaultNamespaceURI() ) )
        {
            // No namespace defined
            // TODO: Output to monitor the problem with the Namespace.
        }

        xml.removeNamespaces();

        Element project = xml.getElement( "//project" );

        model.setGroupId( project.elementTextTrim( "groupId" ) );
        model.setArtifactId( project.elementTextTrim( "artifactId" ) );
        model.setVersion( project.elementTextTrim( "version" ) );
        model.setName( project.elementTextTrim( "name" ) );
        model.setDescription( project.elementTextTrim( "description" ) );
        model.setUrl( project.elementTextTrim( "url" ) );

        model.setPackaging( StringUtils.defaultIfEmpty( project.elementTextTrim( "packaging" ), "jar" ) );

        model.setParentProject( getParentProject( xml ) );

        model.setMailingLists( getMailingLists( xml ) );
        model.setCiManagement( getCiManagement( xml ) );
        model.setIndividuals( getIndividuals( xml ) );
        model.setIssueManagement( getIssueManagement( xml ) );
        model.setLicenses( getLicenses( xml ) );
        model.setOrganization( getOrganization( xml ) );
        model.setScm( getSCM( xml ) );
        model.setRepositories( getRepositories( xml ) );

        model.setDependencies( getDependencies( xml ) );
        model.setDependencyManagement( getDependencyManagement( xml ) );
        model.setPlugins( getPlugins( xml ) );
        model.setReports( getReports( xml ) );
        model.setProperties( getProperties( xml.getElement( "//project/properties" ) ) );

        model.setBuildExtensions( getBuildExtensions( xml ) );

        model.setRelocation( getRelocation( xml ) );

        model.setOrigin( "filesystem" );

        return model;
    }

    private ArtifactReference getArtifactReference( Element elemPlugin, String defaultType )
    {
        ArtifactReference reference = new ArtifactReference();

        reference.setGroupId( StringUtils.defaultString( elemPlugin.elementTextTrim( "groupId" ) ) );
        reference.setArtifactId( elemPlugin.elementTextTrim( "artifactId" ) );
        reference.setVersion( StringUtils.defaultString( elemPlugin.elementTextTrim( "version" ) ) );
        reference.setClassifier( StringUtils.defaultString( elemPlugin.elementTextTrim( "classifier" ) ) );
        reference.setType( StringUtils.defaultIfEmpty( elemPlugin.elementTextTrim( "type" ), defaultType ) );

        return reference;
    }

    /**
     * Get List of {@link ArtifactReference} objects from xpath expr.
     */
    private List<ArtifactReference> getArtifactReferenceList( XMLReader xml, String xpathExpr, String defaultType )
        throws XMLException
    {
        List<ArtifactReference> refs = new ArrayList<ArtifactReference>();

        Iterator<Element> it = xml.getElementList( xpathExpr ).iterator();
        while ( it.hasNext() )
        {
            Element elemPlugin = it.next();

            refs.add( getArtifactReference( elemPlugin, defaultType ) );
        }

        return refs;
    }

    private List<ArtifactReference> getBuildExtensions( XMLReader xml )
        throws XMLException
    {
        return getArtifactReferenceList( xml, "//project/build/extensions/extension", "jar" );
    }

    private CiManagement getCiManagement( XMLReader xml )
        throws XMLException
    {
        Element elemCiMgmt = xml.getElement( "//project/ciManagement" );
        if ( elemCiMgmt != null )
        {
            CiManagement ciManagement = new CiManagement();
            ciManagement.setSystem( elemCiMgmt.elementTextTrim( "system" ) );
            ciManagement.setUrl( elemCiMgmt.elementTextTrim( "url" ) );
            return ciManagement;
        }

        return null;
    }

    private List<Dependency> getDependencies( XMLReader xml )
        throws XMLException
    {
        return getDependencyList( xml, new String[] { "dependencies" } );
    }

    private List<Dependency> getDependencyList( XMLReader xml, String parts[] )
        throws XMLException
    {
        List<Dependency> dependencyList = new ArrayList<Dependency>();

        Element project = xml.getElement( "//project" );

        Element depsParent = project;

        for ( String part : parts )
        {
            depsParent = depsParent.element( part );
            if ( depsParent == null )
            {
                return dependencyList;
            }
        }

        Iterator<Element> it = depsParent.elementIterator( "dependency" );
        while ( it.hasNext() )
        {
            Element elemDependency = it.next();
            Dependency dependency = new Dependency();

            dependency.setGroupId( elemDependency.elementTextTrim( "groupId" ) );
            dependency.setArtifactId( elemDependency.elementTextTrim( "artifactId" ) );
            dependency.setVersion( elemDependency.elementTextTrim( "version" ) );

            dependency.setClassifier( StringUtils.defaultString( elemDependency.elementTextTrim( "classifier" ) ) );
            dependency.setType( StringUtils.defaultIfEmpty( elemDependency.elementTextTrim( "type" ), "jar" ) );
            dependency.setScope( StringUtils.defaultIfEmpty( elemDependency.elementTextTrim( "scope" ), "compile" ) );
            // Not for v4.0.0 -> dependency.setUrl( elemDependency.elementTextTrim("url") );
            dependency.setOptional( toBoolean( elemDependency.elementTextTrim( "optional" ), false ) );
            if ( DependencyScope.isSystemScoped( dependency ) )
            {
                dependency.setSystemPath( elemDependency.elementTextTrim( "systemPath" ) );
            }

            dependency.setExclusions( getExclusions( elemDependency ) );

            if ( dependencyList.contains( dependency ) )
            {
                // TODO: throw into monitor as "duplicate dependency" issue.
            }

            dependencyList.add( dependency );
        }

        return dependencyList;
    }

    private List<Dependency> getDependencyManagement( XMLReader xml )
        throws XMLException
    {
        return getDependencyList( xml, new String[] { "dependencyManagement", "dependencies" } );
    }

    private List<Exclusion> getExclusions( Element elemDependency )
    {
        List<Exclusion> exclusions = new ArrayList<Exclusion>();

        Element elemExclusions = elemDependency.element( "exclusions" );

        if ( elemExclusions != null )
        {
            Iterator<Element> it = elemExclusions.elementIterator( "exclusion" );
            while ( it.hasNext() )
            {
                Element elemExclusion = it.next();
                Exclusion exclusion = new Exclusion();

                exclusion.setGroupId( elemExclusion.elementTextTrim( "groupId" ) );
                exclusion.setArtifactId( elemExclusion.elementTextTrim( "artifactId" ) );

                exclusions.add( exclusion );
            }
        }

        return exclusions;
    }

    private List<Individual> getIndividuals( XMLReader xml )
        throws XMLException
    {
        List<Individual> individuals = new ArrayList<Individual>();

        individuals.addAll( getIndividuals( xml, true, "//project/developers/developer" ) );
        individuals.addAll( getIndividuals( xml, false, "//project/contributors/contributor" ) );

        return individuals;
    }

    private List<Individual> getIndividuals( XMLReader xml, boolean isCommitor, String xpathExpr )
        throws XMLException
    {
        List<Individual> ret = new ArrayList<Individual>();

        List<Element> modelPersonList = xml.getElementList( xpathExpr );

        Iterator<Element> iter = modelPersonList.iterator();
        while ( iter.hasNext() )
        {
            Element elemPerson = iter.next();
            Individual individual = new Individual();

            if ( isCommitor )
            {
                individual.setPrincipal( elemPerson.elementTextTrim( "id" ) );
            }

            individual.setCommitor( isCommitor );
            individual.setEmail( elemPerson.elementTextTrim( "email" ) );
            individual.setName( elemPerson.elementTextTrim( "name" ) );
            individual.setOrganization( elemPerson.elementTextTrim( "organization" ) );
            individual.setOrganizationUrl( elemPerson.elementTextTrim( "organizationUrl" ) );
            individual.setUrl( elemPerson.elementTextTrim( "url" ) );
            individual.setTimezone( elemPerson.elementTextTrim( "timezone" ) );

            // Roles
            Element elemRoles = elemPerson.element( "roles" );
            if ( elemRoles != null )
            {
                List<Element> roleNames = elemRoles.elements( "role" );
                Iterator<Element> itRole = roleNames.iterator();
                while ( itRole.hasNext() )
                {
                    Element role = itRole.next();
                    individual.addRole( role.getTextTrim() );
                }
            }

            // Properties
            individual.setProperties( getProperties( elemPerson.element( "properties" ) ) );

            ret.add( individual );
        }

        return ret;
    }

    private IssueManagement getIssueManagement( XMLReader xml )
        throws XMLException
    {
        Element elemIssueMgmt = xml.getElement( "//project/issueManagement" );
        if ( elemIssueMgmt != null )
        {
            IssueManagement issueMgmt = new IssueManagement();

            issueMgmt.setSystem( elemIssueMgmt.elementTextTrim( "system" ) );
            issueMgmt.setUrl( elemIssueMgmt.elementTextTrim( "url" ) );

            return issueMgmt;
        }

        return null;
    }

    private List<License> getLicenses( XMLReader xml )
        throws XMLException
    {
        List<License> licenses = new ArrayList<License>();

        Element elemLicenses = xml.getElement( "//project/licenses" );

        if ( elemLicenses != null )
        {
            List<Element> licenseList = elemLicenses.elements( "license" );
            for ( Element elemLicense : licenseList )
            {
                License license = new License();

                // TODO: Create LicenseIdentity class to managed license ids.
                // license.setId( elemLicense.elementTextTrim("id") );
                license.setName( elemLicense.elementTextTrim( "name" ) );
                license.setUrl( elemLicense.elementTextTrim( "url" ) );
                license.setComments( elemLicense.elementTextTrim( "comments" ) );

                licenses.add( license );
            }
        }

        return licenses;
    }

    private List<MailingList> getMailingLists( XMLReader xml )
        throws XMLException
    {
        List<MailingList> mailingLists = new ArrayList<MailingList>();

        List<Element> mailingListElems = xml.getElementList( "//project/mailingLists/mailingList" );
        for ( Element elemMailingList : mailingListElems )
        {
            MailingList mlist = new MailingList();

            mlist.setName( elemMailingList.elementTextTrim( "name" ) );
            mlist.setSubscribeAddress( elemMailingList.elementTextTrim( "subscribe" ) );
            mlist.setUnsubscribeAddress( elemMailingList.elementTextTrim( "unsubscribe" ) );
            mlist.setPostAddress( elemMailingList.elementTextTrim( "post" ) );
            mlist.setMainArchiveUrl( elemMailingList.elementTextTrim( "archive" ) );

            Element elemOtherArchives = elemMailingList.element( "otherArchives" );
            if ( elemOtherArchives != null )
            {
                List<String> otherArchives = new ArrayList<String>();
                List<Element> others = elemOtherArchives.elements( "otherArchive" );
                for ( Element other : others )
                {
                    String otherArchive = other.getTextTrim();
                    otherArchives.add( otherArchive );
                }

                mlist.setOtherArchives( otherArchives );
            }

            mailingLists.add( mlist );
        }

        return mailingLists;
    }

    private Organization getOrganization( XMLReader xml )
        throws XMLException
    {
        Element elemOrg = xml.getElement( "//project/organization" );
        if ( elemOrg != null )
        {
            Organization org = new Organization();

            org.setName( elemOrg.elementTextTrim( "name" ) );
            org.setUrl( elemOrg.elementTextTrim( "url" ) );

            return org;
        }

        return null;
    }

    private VersionedReference getParentProject( XMLReader xml )
        throws XMLException
    {
        Element elemParent = xml.getElement( "//project/parent" );

        if ( elemParent != null )
        {
            return getVersionedReference( elemParent );
        }

        return null;
    }

    private List<ArtifactReference> getPlugins( XMLReader xml )
        throws XMLException
    {
        return getArtifactReferenceList( xml, "//project/build/plugins/plugin", "maven-plugin" );
    }

    private Properties getProperties( Element elemProperties )
    {
        if ( elemProperties == null )
        {
            return null;
        }

        Properties ret = new Properties();

        Iterator<Element> itProps = elemProperties.elements().iterator();
        while ( itProps.hasNext() )
        {
            Element elemProp = (Element) itProps.next();
            ret.setProperty( elemProp.getName(), elemProp.getText() );
        }

        return ret;
    }

    private VersionedReference getRelocation( XMLReader xml )
        throws XMLException
    {
        Element elemRelocation = xml.getElement( "//project/distributionManagement/relocation" );

        if ( elemRelocation != null )
        {
            return getVersionedReference( elemRelocation );
        }

        return null;
    }

    private List<ArtifactReference> getReports( XMLReader xml )
        throws XMLException
    {
        return getArtifactReferenceList( xml, "//project/reporting/plugins/plugin", "maven-plugin" );
    }

    private List<ProjectRepository> getRepositories( XMLReader xml )
        throws XMLException
    {
        List<ProjectRepository> repos = new ArrayList<ProjectRepository>();

        repos.addAll( getRepositories( xml, false, "//project/repositories/repository" ) );
        repos.addAll( getRepositories( xml, true, "//project/pluginRepositories/pluginRepository" ) );

        return repos;
    }

    private List<ProjectRepository> getRepositories( XMLReader xml, boolean isPluginRepo, String xpathExpr )
        throws XMLException
    {
        List<ProjectRepository> ret = new ArrayList<ProjectRepository>();

        List<Element> repositoriesList = xml.getElementList( xpathExpr );

        for ( Element elemRepo : repositoriesList )
        {
            ProjectRepository repo = new ProjectRepository();

            repo.setId( elemRepo.elementTextTrim( "id" ) );
            repo.setName( elemRepo.elementTextTrim( "name" ) );
            repo.setUrl( elemRepo.elementTextTrim( "url" ) );
            repo.setLayout( StringUtils.defaultIfEmpty( elemRepo.elementTextTrim( "layout" ), "default" ) );
            repo.setPlugins( isPluginRepo );

            repo.setReleases( toBoolean( xml.getElementText( elemRepo, "releases/enabled" ), true ) );
            repo.setSnapshots( toBoolean( xml.getElementText( elemRepo, "snapshots/enabled" ), false ) );

            ret.add( repo );
        }

        return ret;
    }

    private Scm getSCM( XMLReader xml )
        throws XMLException
    {
        Element elemScm = xml.getElement( "//project/scm" );

        if ( elemScm != null )
        {
            Scm scm = new Scm();

            scm.setConnection( elemScm.elementTextTrim( "connection" ) );
            scm.setDeveloperConnection( elemScm.elementTextTrim( "developerConnection" ) );
            scm.setUrl( elemScm.elementTextTrim( "url" ) );

            return scm;
        }

        return null;
    }

    private VersionedReference getVersionedReference( Element elem )
    {
        VersionedReference reference = new VersionedReference();

        reference.setGroupId( elem.elementTextTrim( "groupId" ) );
        reference.setArtifactId( elem.elementTextTrim( "artifactId" ) );
        reference.setVersion( elem.elementTextTrim( "version" ) );

        return reference;
    }

    private boolean toBoolean( String value, boolean defaultValue )
    {
        if ( StringUtils.equalsIgnoreCase( value, "true" ) )
        {
            return true;
        }
        else if ( StringUtils.equalsIgnoreCase( value, "false" ) )
        {
            return false;
        }
        else
        {
            // If unset, or not "true" or "false".
            return defaultValue;
        }
    }
}
