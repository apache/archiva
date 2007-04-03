package org.apache.maven.archiva.repository.project;

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
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.DependencyScope;
import org.apache.maven.archiva.model.Exclusion;
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.License;
import org.apache.maven.archiva.model.Organization;
import org.apache.maven.archiva.model.ProjectRepository;
import org.apache.maven.archiva.model.RepositoryContent;
import org.apache.maven.archiva.model.Scm;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLReader;
import org.dom4j.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ProjectModel400Reader 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModel400Reader implements ProjectModelReader
{

    public ArchivaProjectModel read( File pomFile ) throws ProjectModelException
    {
        try
        {
            XMLReader xml = new XMLReader( "project", pomFile );

            ArchivaProjectModel model = new ArchivaProjectModel();

            RepositoryContent contentKey = new RepositoryContent();
            contentKey.setGroupId( xml.getElementText( "//project/groupId" ) );
            contentKey.setArtifactId( xml.getElementText( "//project/artifactId" ) );
            contentKey.setVersion( xml.getElementText( "//project/version" ) );
            model.setContentKey( contentKey );

            model.setName( xml.getElementText( "//project/name" ) );
            model.setDescription( xml.getElementText( "//project/description" ) );
            model.setUrl( xml.getElementText( "//project/url" ) );
            model.setPackaging( StringUtils.defaultIfEmpty( xml.getElementText( "//project/packaging" ), "jar" ) );

            model.setParentContentKey( getParentContentKey( xml ) );

            model.setCiManagement( getCiManagement( xml ) );
            model.setIndividuals( getIndividuals( xml ) );
            model.setIssueManagement( getIssueManagement( xml ) );
            model.setLicenses( getLicenses( xml ) );
            model.setOrganization( getOrganization( xml ) );
            model.setScm( getSCM( xml ) );
            model.setRepositories( getRepositories( xml ) );

            model.setDependencies( getDependencies( xml ) );
            model.setPlugins( getPlugins( xml ) );
            model.setReports( getReports( xml ) );

            return model;
        }
        catch ( XMLException e )
        {
            throw new ProjectModelException( e.getMessage(), e );
        }
    }

    private CiManagement getCiManagement( XMLReader xml ) throws XMLException
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

    private List getDependencies( XMLReader xml ) throws XMLException
    {
        List dependencies = new ArrayList();

        Iterator it = xml.getElementList( "//project/dependencies/dependency" ).iterator();
        while ( it.hasNext() )
        {
            Element elemDependency = (Element) it.next();
            Dependency dependency = new Dependency();

            dependency.setContentKey( getContentKey( elemDependency ) );

            dependency.setClassifier( elemDependency.elementTextTrim( "classifier" ) );
            dependency.setType( StringUtils.defaultIfEmpty( elemDependency.elementTextTrim( "type" ), "jar" ) );
            dependency.setScope( StringUtils.defaultIfEmpty( elemDependency.elementTextTrim( "scope" ), "compile" ) );
            // Not for v4.0.0 -> dependency.setUrl( elemDependency.elementTextTrim( "url" ) );
            dependency.setOptional( toBoolean( elemDependency.elementTextTrim( "optional" ), false ) );
            if ( DependencyScope.isSystemScoped( dependency ) )
            {
                dependency.setSystemPath( elemDependency.elementTextTrim( "systemPath" ) );
            }

            dependency.setExclusions( getExclusions( elemDependency ) );

            dependencies.add( dependency );
        }

        return dependencies;
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

    private List getIndividuals( XMLReader xml ) throws XMLException
    {
        List individuals = new ArrayList();

        individuals.addAll( getIndividuals( xml, true, "//project/developers/developer" ) );
        individuals.addAll( getIndividuals( xml, false, "//project/contributors/contributor" ) );

        return individuals;
    }

    private List getIndividuals( XMLReader xml, boolean isCommitor, String xpathExpr ) throws XMLException
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
            Element elemProperties = elemPerson.element( "properties" );
            if ( elemProperties != null )
            {
                Iterator itProps = elemProperties.elements().iterator();
                while ( itProps.hasNext() )
                {
                    Element elemProp = (Element) itProps.next();
                    individual.addProperty( elemProp.getName(), elemProp.getText() );
                }
            }

            ret.add( individual );
        }

        return ret;
    }

    private IssueManagement getIssueManagement( XMLReader xml ) throws XMLException
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

    private List getLicenses( XMLReader xml ) throws XMLException
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
                // license.setId( elemLicense.elementTextTrim( "id" ) );
                license.setName( elemLicense.elementTextTrim( "name" ) );
                license.setUrl( elemLicense.elementTextTrim( "url" ) );
                license.setComments( elemLicense.elementTextTrim( "comments" ) );

                licenses.add( license );
            }
        }

        return licenses;
    }

    private Organization getOrganization( XMLReader xml ) throws XMLException
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

    private RepositoryContent getParentContentKey( XMLReader xml ) throws XMLException
    {
        Element elemParent = xml.getElement( "//project/parent" );

        if ( elemParent != null )
        {
            return getContentKey( elemParent );
        }

        return null;
    }

    private RepositoryContent getContentKey( Element elem )
    {
        RepositoryContent contentKey = new RepositoryContent();

        contentKey.setGroupId( elem.elementTextTrim( "groupId" ) );
        contentKey.setArtifactId( elem.elementTextTrim( "artifactId" ) );
        contentKey.setVersion( elem.elementTextTrim( "version" ) );

        return contentKey;
    }

    private List getPlugins( XMLReader xml ) throws XMLException
    {
        return getPlugins( xml, "//project/build/plugins/plugin" );
    }

    private List getReports( XMLReader xml ) throws XMLException
    {
        return getPlugins( xml, "//project/reporting/plugins/plugin" );
    }

    /**
     * Get List of {@link RepositoryContent} objects from plugin definitions.
     */
    private List getPlugins( XMLReader xml, String xpathExpr ) throws XMLException
    {
        List plugins = new ArrayList();

        Iterator it = xml.getElementList( xpathExpr ).iterator();
        while ( it.hasNext() )
        {
            Element elemPlugin = (Element) it.next();

            plugins.add( getContentKey( elemPlugin ) );
        }

        return plugins;
    }

    private List getRepositories( XMLReader xml ) throws XMLException
    {
        List repos = new ArrayList();

        repos.addAll( getRepositories( xml, false, "//project/repositories/repository" ) );
        repos.addAll( getRepositories( xml, true, "//project/pluginRepositories/pluginRepository" ) );

        return repos;
    }

    private List getRepositories( XMLReader xml, boolean isPluginRepo, String xpathExpr ) throws XMLException
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
            repo.setReleases( toBoolean( xml.getElementText( elemRepo, "snapshots/enabled" ), false ) );

            ret.add( repo );
        }

        return ret;
    }

    private Scm getSCM( XMLReader xml ) throws XMLException
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
