package org.apache.maven.archiva.repository.project.writers;

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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.CiManagement;
import org.apache.maven.archiva.model.Dependency;
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
import org.apache.maven.archiva.repository.project.ProjectModelWriter;
import org.apache.maven.archiva.xml.XMLException;
import org.apache.maven.archiva.xml.XMLWriter;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

/**
 * ProjectModel400Writer for Maven 2 project model v4.0.0 pom files.  
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ProjectModel400Writer
    implements ProjectModelWriter
{
    private static final Namespace DEFAULT_NAMESPACE = Namespace.get( "", "http://maven.apache.org/POM/4.0.0" );

    public void write( ArchivaProjectModel model, File pomFile )
        throws ProjectModelException, IOException
    {
        FileWriter writer = null;
        try
        {
            writer = new FileWriter( pomFile );
            write( model, writer );
            writer.flush();
        }
        finally
        {
            IOUtils.closeQuietly( writer );
        }
    }

    public void write( ArchivaProjectModel model, Writer writer )
        throws ProjectModelException, IOException
    {
        Document doc = DocumentHelper.createDocument();

        Element root = DocumentHelper.createElement( "project" );

        root.add( DEFAULT_NAMESPACE );
        root.addNamespace( "xsi", "http://www.w3.org/2001/XMLSchema-instance" );
        root.addAttribute( "xsi:schemaLocation",
                           "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd" );

        doc.setRootElement( root );

        root.addElement( "modelVersion" ).setText( "4.0.0" );

        addParent( root, model.getParentProject() );

        addChildElement( root, "groupId", model.getGroupId() );
        root.addElement( "artifactId" ).setText( model.getArtifactId() );

        addChildElement( root, "version", model.getVersion() );

        addChildElement( root, "packaging", model.getPackaging() );
        addChildElement( root, "name", model.getName() );
        addChildElement( root, "description", model.getDescription() );
        addChildElement( root, "url", model.getUrl() );
        // TODO: add inceptionYear to ArchivaProjectModel

        addOrganization( root, model.getOrganization() );

        addIssueManagement( root, model.getIssueManagement() );
        addCiManagement( root, model.getCiManagement() );
        addMailingLists( root, model.getMailingLists() );
        addDevelopersAndContributors( root, model.getIndividuals() );
        // TODO: add distribution management to ArchivaProjectModel

        addLicenses( root, model.getLicenses() );
        addRepositories( root, model.getRepositories() );
        addDependencyManagement( root, model.getDependencyManagement() );
        addDependencies( root, model.getDependencies() );

        addReporting( root, model.getReports() );
        addScm( root, model.getScm() );

        // <build> element
        addPlugins( root, model.getPlugins() );
        addBuildExtensions( root, model.getBuildExtensions() );

        // <distributionManagement>
        addRelocation( root, model.getRelocation() );

        fixDefaultNamespace( root );

        try
        {
            XMLWriter.write( doc, writer );
        }
        catch ( XMLException e )
        {
            throw new ProjectModelException( "Unable to write xml contents to writer: " + e.getMessage(), e );
        }
    }

    private void addArtifactReference( Element elem, ArtifactReference ref, String defaultType )
    {
        addChildElement( elem, "groupId", ref.getGroupId() );
        addChildElement( elem, "artifactId", ref.getArtifactId() );
        addChildElement( elem, "version", ref.getVersion() );
        addChildElement( elem, "classifier", ref.getClassifier() );

        if ( !StringUtils.equals( defaultType, ref.getType() ) )
        {
            addChildElement( elem, "type", ref.getType() );
        }
    }

    private void addBuildExtensions( Element root, List<ArtifactReference> buildExtensions )
    {
        if ( CollectionUtils.isEmpty( buildExtensions ) )
        {
            return;
        }

        Element build = root.element( "build" );
        if ( build == null )
        {
            build = root.addElement( "build" );
        }

        Element elemExtensions = build.addElement( "extensions" );

        for ( ArtifactReference extension : buildExtensions )
        {
            Element elem = elemExtensions.addElement( "extension" );

            addArtifactReference( elem, extension, "jar" );
        }
    }

    private void addCiManagement( Element root, CiManagement ciManagement )
    {
        if ( ciManagement == null )
        {
            return;
        }

        Element elem = root.addElement( "ciManagement" );
        addChildElement( elem, "system", ciManagement.getSystem() );
        addChildElement( elem, "url", ciManagement.getUrl() );
        // TODO: Add notifiers into ArchivaProjectModel 
    }

    private void addDependencies( Element root, List<Dependency> dependencies )
    {
        if ( CollectionUtils.isEmpty( dependencies ) )
        {
            return;
        }

        addDependencyList( root, dependencies );
    }

    private void addDependencyList( Element elemParent, List<Dependency> dependencies )
    {
        if ( CollectionUtils.isEmpty( dependencies ) )
        {
            return;
        }

        Element elemDeps = elemParent.addElement( "dependencies" );

        for ( Dependency dep : dependencies )
        {
            Element elem = elemDeps.addElement( "dependency" );

            addChildElement( elem, "groupId", dep.getGroupId() );
            addChildElement( elem, "artifactId", dep.getArtifactId() );
            addChildElement( elem, "version", dep.getVersion() );
            addChildElement( elem, "classifier", dep.getClassifier() );
            addChildElement( elem, "type", dep.getType() );
            addChildElement( elem, "scope", dep.getScope() );
            addChildElement( elem, "systemPath", dep.getSystemPath() );

            addExclusions( elem, dep.getExclusions() );
        }
    }

    private void addDependencyManagement( Element root, List<Dependency> dependencyManagement )
    {
        if ( CollectionUtils.isEmpty( dependencyManagement ) )
        {
            return;
        }

        Element elemDepMgmt = root.addElement( "dependencyManagement" );
        addDependencyList( elemDepMgmt, dependencyManagement );
    }

    private void addDevelopersAndContributors( Element root, List<Individual> individuals )
    {
        if ( CollectionUtils.isEmpty( individuals ) )
        {
            return;
        }

        Element developers = null;
        Element contributors = null;

        for ( Individual individual : individuals )
        {
            if ( individual.isCommitor() )
            {
                if ( developers == null )
                {
                    developers = root.addElement( "developers" );
                }

                Element developer = developers.addElement( "developer" );
                addChildElement( developer, "id", individual.getPrincipal() );
                addIndividual( developer, individual );
            }
            else
            {
                if ( contributors == null )
                {
                    contributors = root.addElement( "contributors" );
                }

                Element contributor = contributors.addElement( "contributor" );
                addIndividual( contributor, individual );
            }
        }
    }

    private void addExclusions( Element elemParent, List<Exclusion> exclusions )
    {
        if ( CollectionUtils.isEmpty( exclusions ) )
        {
            return;
        }

        Element elemExclusions = elemParent.addElement( "exclusions" );

        for ( Exclusion exclusion : exclusions )
        {
            Element elem = elemExclusions.addElement( "exclusion" );

            addChildElement( elem, "groupId", exclusion.getGroupId() );
            addChildElement( elem, "artifactId", exclusion.getArtifactId() );
        }
    }

    private void addIndividual( Element elem, Individual individual )
    {
        addChildElement( elem, "name", individual.getName() );
        addChildElement( elem, "email", individual.getEmail() );
        addChildElement( elem, "organization", individual.getOrganization() );
        addChildElement( elem, "organizationUrl", individual.getOrganizationUrl() );
        addChildElement( elem, "timezone", individual.getTimezone() );

        if ( CollectionUtils.isNotEmpty( individual.getRoles() ) )
        {
            Element roles = elem.addElement( "roles" );
            List<String> roleList = individual.getRoles();
            for ( String roleName : roleList )
            {
                addChildElement( roles, "role", roleName );
            }
        }
    }

    private void addIssueManagement( Element root, IssueManagement issueManagement )
    {
        if ( issueManagement == null )
        {
            return;
        }

        Element elem = root.addElement( "issueManagement" );
        addChildElement( elem, "system", issueManagement.getSystem() );
        addChildElement( elem, "url", issueManagement.getUrl() );
    }

    private void addLicenses( Element root, List<License> licenses )
    {
        if ( CollectionUtils.isEmpty( licenses ) )
        {
            return;
        }

        Element elemLicenses = root.addElement( "licenses" );

        for ( License license : licenses )
        {
            Element elem = elemLicenses.addElement( "license" );
            addChildElement( elem, "name", license.getName() );
            addChildElement( elem, "url", license.getUrl() );
            // TODO: research if we need <distribution> subelement.
        }
    }

    private void addMailingLists( Element root, List<MailingList> mailingLists )
    {
        if ( CollectionUtils.isEmpty( mailingLists ) )
        {
            return;
        }

        Element mlists = root.addElement( "mailingLists" );

        for ( MailingList mailingList : mailingLists )
        {
            Element mlist = mlists.addElement( "mailingList" );
            addChildElement( mlist, "name", mailingList.getName() );
            addChildElement( mlist, "post", mailingList.getPostAddress() );
            addChildElement( mlist, "subscribe", mailingList.getSubscribeAddress() );
            addChildElement( mlist, "unsubscribe", mailingList.getUnsubscribeAddress() );
            addChildElement( mlist, "archive", mailingList.getMainArchiveUrl() );

            addOtherArchives( mlist, mailingList.getOtherArchives() );
        }
    }

    private void addOtherArchives( Element mlist, List<String> otherArchives )
    {
        if ( CollectionUtils.isEmpty( otherArchives ) )
        {
            return;
        }

        Element elemOtherArchives = mlist.addElement( "otherArchives" );

        for ( String archive : otherArchives )
        {
            addChildElement( elemOtherArchives, "otherArchive", archive );
        }
    }

    private void addOrganization( Element root, Organization organization )
    {
        if ( organization == null )
        {
            return;
        }

        Element elem = root.addElement( "organization" );

        addChildElement( elem, "name", organization.getName() );
        addChildElement( elem, "url", organization.getUrl() );
    }

    private void addParent( Element root, VersionedReference parentProject )
    {
        if ( parentProject == null )
        {
            return;
        }

        Element parent = root.addElement( "parent" );
        parent.addElement( "groupId" ).setText( parentProject.getGroupId() );
        parent.addElement( "artifactId" ).setText( parentProject.getArtifactId() );
        parent.addElement( "version" ).setText( parentProject.getVersion() );
    }

    private void addPlugins( Element root, List<ArtifactReference> plugins )
    {
        if ( CollectionUtils.isEmpty( plugins ) )
        {
            return;
        }

        Element build = root.element( "build" );
        if ( build == null )
        {
            build = root.addElement( "build" );
        }

        Element elemPlugins = build.addElement( "plugins" );

        for ( ArtifactReference plugin : plugins )
        {
            Element elem = elemPlugins.addElement( "plugin" );

            addArtifactReference( elem, plugin, "maven-plugin" );
        }
    }

    private void addRelocation( Element root, VersionedReference relocation )
    {
        if ( relocation == null )
        {
            return;
        }

        Element distribManagement = root.element( "distributionManagement" );

        if ( distribManagement == null )
        {
            distribManagement = root.addElement( "distributionManagement" );
        }

        Element elem = distribManagement.addElement( "relocation" );
        addChildElement( elem, "groupId", relocation.getGroupId() );
        addChildElement( elem, "artifactId", relocation.getArtifactId() );
        addChildElement( elem, "version", relocation.getVersion() );
    }

    private void addReporting( Element root, List<ArtifactReference> reports )
    {
        if ( CollectionUtils.isEmpty( reports ) )
        {
            return;
        }

        Element reporting = root.addElement( "reporting" );
        Element plugins = reporting.addElement( "plugins" );

        for ( ArtifactReference reference : reports )
        {
            Element plugin = plugins.addElement( "plugin" );
            addChildElement( plugin, "groupId", reference.getGroupId() );
            addChildElement( plugin, "artifactId", reference.getArtifactId() );
            addChildElement( plugin, "version", reference.getVersion() );
        }
    }

    private void addRepositories( Element root, List<ProjectRepository> repositories )
    {
        if ( CollectionUtils.isEmpty( repositories ) )
        {
            return;
        }

        Element elemRepos = root.addElement( "repositories" );
        for ( ProjectRepository repository : repositories )
        {
            Element elem = elemRepos.addElement( "repository" );
            addChildElement( elem, "id", repository.getId() );
            addChildElement( elem, "name", repository.getName() );
            addChildElement( elem, "url", repository.getUrl() );

            if ( !StringUtils.equals( "default", repository.getLayout() ) )
            {
                addChildElement( elem, "layout", repository.getLayout() );
            }
        }
    }

    private void addScm( Element root, Scm scm )
    {
        if ( scm == null )
        {
            return;
        }

        Element elem = root.addElement( "scm" );

        addChildElement( elem, "connection", scm.getConnection() );
        addChildElement( elem, "developerConnection", scm.getDeveloperConnection() );
        addChildElement( elem, "url", scm.getUrl() );
    }

    /**
     * Fix the default namespace on all elements recursively.
     */
    private void fixDefaultNamespace( Element elem )
    {
        elem.remove( elem.getNamespace() );
        elem.setQName( QName.get( elem.getName(), DEFAULT_NAMESPACE, elem.getQualifiedName() ) );

        Node n;

        Iterator<Node> it = elem.elementIterator();
        while ( it.hasNext() )
        {
            n = it.next();

            switch ( n.getNodeType() )
            {
                case Node.ELEMENT_NODE:
                    fixDefaultNamespace( (Element) n );
                    break;
            }
        }
    }

    private static void addChildElement( Element elem, String elemName, String text )
    {
        if ( StringUtils.isBlank( text ) )
        {
            return;
        }

        elem.addElement( elemName ).setText( text );
    }
}
