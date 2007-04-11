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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.DependencyTree;
import org.apache.maven.archiva.model.Exclusion;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.MailingList;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.ProjectRepository;
import org.apache.maven.archiva.model.Scm;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelReader;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * ProjectModel400Reader - read in modelVersion 4.0.0 pom files into archiva-model structures.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModel400Reader
    implements ProjectModelReader
{

    public ArchivaProjectModel read( File pomFile )
        throws ProjectModelException
    {
        try
        {
            XMLReader xml = new XMLReader( "project", pomFile );

            ArchivaProjectModel model = new ArchivaProjectModel();

            if ( !"http://maven.apache.org/POM/4.0.0".equals( xml.getDefaultNamespaceURI() ) )
            {
                // TODO: Output to monitor the problem with the Namespace.
                System.out.println( "No namespace defined: " + pomFile );
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

            model.setDependencyTree( getDependencyTree( xml ) );
            model.setDependencyManagement( getDependencyManagement( xml ) );
            model.setPlugins( getPlugins( xml ) );
            model.setReports( getReports( xml ) );
            model.setProperties( getProperties( xml.getElement( "//project/properties" ) ) );

            return model;
        }
        catch ( XMLException e )
        {
            throw new ProjectModelException( e.getMessage(), e );
        }
    }

    private ArtifactReference getArtifactReference( Element elemPlugin )
    {
        ArtifactReference reference = new ArtifactReference();

        reference.setGroupId( elemPlugin.elementTextTrim( "groupId" ) );
        reference.setArtifactId( elemPlugin.elementTextTrim( "artifactId" ) );
        reference.setVersion( elemPlugin.elementTextTrim( "version" ) );
        reference.setClassifier( elemPlugin.elementTextTrim( "classifier" ) );
        reference.setType( elemPlugin.elementTextTrim( "type" ) );

        return reference;
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

    private DependencyTree getDependencyTree( XMLReader xml )
        throws XMLException
    {
        DependencyTree tree = new DependencyTree();
        List dependencies = getDependencyList( xml, new String[] { "dependencies" } );

        Iterator it = dependencies.iterator();
        while ( it.hasNext() )
        {
            Dependency dependency = (Dependency) it.next();
            tree.addDependencyNode( dependency );
        }

        return tree;
    }

    private List getDependencyList( XMLReader xml, String parts[] )
        throws XMLException
    {
        List dependencyList = new ArrayList();

        Element project = xml.getElement( "//project" );

        Element depsParent = project;

        for ( int i = 0; i < parts.length; i++ )
        {
            String part = parts[i];
            depsParent = depsParent.element( part );
            if ( depsParent == null )
            {
                return dependencyList;
            }
        }

        Iterator it = depsParent.elementIterator( "dependency" );
        while ( it.hasNext() )
        {
            Element elemDependency = (Element) it.next();
            Dependency dependency = new Dependency();

            dependency.setGroupId( elemDependency.elementTextTrim( "groupId" ) );
            dependency.setArtifactId( elemDependency.elementTextTrim( "artifactId" ) );
            dependency.setVersion( elemDependency.elementTextTrim( "version" ) );

            dependency.setClassifier( elemDependency.elementTextTrim( "classifier" ) );
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
                // TODO: throw into monitor as issue.
                System.err.println( "Duplicate non-unique dependency detected [" + StringUtils.join( parts, ":" )
                    + "]: " + toDependencyKey( dependency ) );
            }

            dependencyList.add( dependency );
            System.out.println( "Added (list.size:" + dependencyList.size() + ") dependency: "
                + toDependencyKey( dependency ) );
        }

        System.out.println( "## Returning dependency list: size=" + dependencyList.size() );
        return dependencyList;
    }

    private List getDependencyManagement( XMLReader xml )
        throws XMLException
    {
        return getDependencyList( xml, new String[] { "dependencyManagement", "dependencies" } );
    }

    private List getExclusions( Element elemDependency )
    {
        List exclusions = new ArrayList();

        Element elemExclusions = elemDependency.element( "exclusions" );

        if ( elemExclusions != null )
        {
            Iterator it = elemExclusions.elementIterator( "exclusion" );
            while ( it.hasNext() )
            {
                Element elemExclusion = (Element) it.next();
                Exclusion exclusion = new Exclusion();

                exclusion.setGroupId( elemExclusion.elementTextTrim( "groupId" ) );
                exclusion.setArtifactId( elemExclusion.elementTextTrim( "artifactId" ) );

                exclusions.add( exclusion );
            }
        }

        return exclusions;
    }

    private List getIndividuals( XMLReader xml )
        throws XMLException
    {
        List individuals = new ArrayList();

        individuals.addAll( getIndividuals( xml, true, "//project/developers/developer" ) );
        individuals.addAll( getIndividuals( xml, false, "//project/contributors/contributor" ) );

        return individuals;
    }

    private List getIndividuals( XMLReader xml, boolean isCommitor, String xpathExpr )
        throws XMLException
    {
        List ret = new ArrayList();

        List modelPersonList = xml.getElementList( xpathExpr );

        Iterator iter = modelPersonList.iterator();
        while ( iter.hasNext() )
        {
            Element elemPerson = (Element) iter.next();
            Individual individual = new Individual();

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
                List roleNames = elemRoles.elements( "role" );
                Iterator itRole = roleNames.iterator();
                while ( itRole.hasNext() )
                {
                    Element role = (Element) itRole.next();
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

    private List getMailingLists( XMLReader xml )
        throws XMLException
    {
        List mailingLists = new ArrayList();

        List mailingListElems = xml.getElementList( "//project/mailingLists/mailingList" );
        Iterator it = mailingListElems.iterator();
        while ( it.hasNext() )
        {
            Element elemMailingList = (Element) it.next();
            MailingList mlist = new MailingList();

            mlist.setName( elemMailingList.elementTextTrim( "name" ) );
            mlist.setSubscribeAddress( elemMailingList.elementTextTrim( "subscribe" ) );
            mlist.setUnsubscribeAddress( elemMailingList.elementTextTrim( "unsubscribe" ) );
            mlist.setPostAddress( elemMailingList.elementTextTrim( "post" ) );
            mlist.setMainArchiveUrl( elemMailingList.elementTextTrim( "archive" ) );

            Element elemOtherArchives = elemMailingList.element( "otherArchives" );
            if ( elemOtherArchives != null )
            {
                List otherArchives = new ArrayList();
                Iterator itother = elemOtherArchives.elementIterator( "otherArchive" );
                while ( itother.hasNext() )
                {
                    String otherArchive = ( (Element) itother.next() ).getTextTrim();
                    otherArchives.add( otherArchive );
                }

                mlist.setOtherArchives( otherArchives );
            }

            mailingLists.add( mlist );
        }

        return mailingLists;
    }

    private List getLicenses( XMLReader xml )
        throws XMLException
    {
        List licenses = new ArrayList();

        Element elemLicenses = xml.getElement( "//project/licenses" );

        if ( elemLicenses != null )
        {
            Iterator itLicense = elemLicenses.elements( "license" ).iterator();
            while ( itLicense.hasNext() )
            {
                Element elemLicense = (Element) itLicense.next();
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

    private List getPlugins( XMLReader xml )
        throws XMLException
    {
        return getPlugins( xml, "//project/build/plugins/plugin" );
    }

    /**
     * Get List of {@link RepositoryContent} objects from plugin definitions.
     */
    private List getPlugins( XMLReader xml, String xpathExpr )
        throws XMLException
    {
        List plugins = new ArrayList();

        Iterator it = xml.getElementList( xpathExpr ).iterator();
        while ( it.hasNext() )
        {
            Element elemPlugin = (Element) it.next();

            plugins.add( getArtifactReference( elemPlugin ) );
        }

        return plugins;
    }

    private Properties getProperties( Element elemProperties )
    {
        if ( elemProperties == null )
        {
            return null;
        }

        Properties ret = new Properties();

        Iterator itProps = elemProperties.elements().iterator();
        while ( itProps.hasNext() )
        {
            Element elemProp = (Element) itProps.next();
            ret.setProperty( elemProp.getName(), elemProp.getText() );
        }

        return ret;
    }

    private List getReports( XMLReader xml )
        throws XMLException
    {
        return getPlugins( xml, "//project/reporting/plugins/plugin" );
    }

    private List getRepositories( XMLReader xml )
        throws XMLException
    {
        List repos = new ArrayList();

        repos.addAll( getRepositories( xml, false, "//project/repositories/repository" ) );
        repos.addAll( getRepositories( xml, true, "//project/pluginRepositories/pluginRepository" ) );

        return repos;
    }

    private List getRepositories( XMLReader xml, boolean isPluginRepo, String xpathExpr )
        throws XMLException
    {
        List ret = new ArrayList();

        List repositoriesList = xml.getElementList( xpathExpr );

        Iterator itRepos = repositoriesList.iterator();
        while ( itRepos.hasNext() )
        {
            Element elemRepo = (Element) itRepos.next();
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

    private String toDependencyKey( Dependency dep )
    {
        return "[" + dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion() + ":" + dep.getClassifier()
            + ":" + dep.getType() + "]";
    }

}
