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
import org.apache.maven.archiva.model.Individual;
import org.apache.maven.archiva.model.IssueManagement;
import org.apache.maven.archiva.model.MailingList;
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
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component 
 *      role="org.apache.maven.archiva.repository.project.ProjectModelWriter"
 *      role-hint="model400"
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

        addOptionalElementText( root, "groupId", model.getGroupId() );
        root.addElement( "artifactId" ).setText( model.getArtifactId() );

        addOptionalElementText( root, "version", model.getVersion() );

        addOptionalElementText( root, "packaging", model.getPackaging() );
        addOptionalElementText( root, "name", model.getName() );
        addOptionalElementText( root, "description", model.getDescription() );
        addOptionalElementText( root, "url", model.getUrl() );
        // TODO: add inceptionYear to ArchivaProjectModel

        addIssueManagement( root, model.getIssueManagement() );
        addCiManagement( root, model.getCiManagement() );
        addMailingLists( root, model.getMailingLists() );
        addDevelopersAndContributors( root, model.getIndividuals() );
        // TODO: add distribution management to ArchivaProjectModel
        addReporting( root, model.getReports() );
        addScm( root, model.getScm() );

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

    private void addScm( Element root, Scm scm )
    {
        if( scm == null )
        {
            return;
        }
        
        Element elem = root.addElement( "scm" );
        
        addOptionalElementText( elem, "connection", scm.getConnection() );
        addOptionalElementText( elem, "developerConnection", scm.getDeveloperConnection() );
        addOptionalElementText( elem, "url", scm.getUrl() );
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
            addOptionalElementText( plugin, "groupId", reference.getGroupId() );
            addOptionalElementText( plugin, "artifactId", reference.getArtifactId() );
            addOptionalElementText( plugin, "version", reference.getVersion() );
        }
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
                addOptionalElementText( developer, "id", individual.getPrincipal() );
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

    private void addIndividual( Element elem, Individual individual )
    {
        addOptionalElementText( elem, "name", individual.getName() );
        addOptionalElementText( elem, "email", individual.getEmail() );
        addOptionalElementText( elem, "organization", individual.getOrganization() );
        addOptionalElementText( elem, "timezone", individual.getTimezone() );

        if ( CollectionUtils.isNotEmpty( individual.getRoles() ) )
        {
            Element roles = elem.addElement( "roles" );
            List<String> roleList = individual.getRoles();
            for ( String roleName : roleList )
            {
                addOptionalElementText( roles, "role", roleName );
            }
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
            addOptionalElementText( mlist, "name", mailingList.getName() );
            addOptionalElementText( mlist, "post", mailingList.getPostAddress() );
            addOptionalElementText( mlist, "subscribe", mailingList.getSubscribeAddress() );
            addOptionalElementText( mlist, "unsubscribe", mailingList.getUnsubscribeAddress() );
            addOptionalElementText( mlist, "archive", mailingList.getMainArchiveUrl() );
        }
    }

    private void addCiManagement( Element root, CiManagement ciManagement )
    {
        if ( ciManagement == null )
        {
            return;
        }

        Element elem = root.addElement( "ciManagement" );
        addOptionalElementText( elem, "system", ciManagement.getSystem() );
        addOptionalElementText( elem, "url", ciManagement.getUrl() );
        // TODO: Add notifiers into ArchivaProjectModel 
    }

    private void addIssueManagement( Element root, IssueManagement issueManagement )
    {
        if ( issueManagement == null )
        {
            return;
        }

        Element elem = root.addElement( "issueManagement" );
        addOptionalElementText( elem, "system", issueManagement.getSystem() );
        addOptionalElementText( elem, "url", issueManagement.getUrl() );
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

    /**
     * Fix the default namespace on all elements recursively.
     */
    public void fixDefaultNamespace( Element elem )
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

    private static void addOptionalElementText( Element elem, String elemName, String text )
    {
        if ( StringUtils.isBlank( text ) )
        {
            return;
        }

        elem.addElement( elemName ).setText( text );
    }
}
