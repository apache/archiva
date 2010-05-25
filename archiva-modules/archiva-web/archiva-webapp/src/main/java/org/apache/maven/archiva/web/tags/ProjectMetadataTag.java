package org.apache.maven.archiva.web.tags;

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

import java.io.IOException;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProjectMetadataTag 
 * 
 * Outputs the project metadata attributes, used in the Metadata tab in artifact browse.
 */
@SuppressWarnings( "serial" )
public class ProjectMetadataTag
    extends TagSupport
{
    private Logger log = LoggerFactory.getLogger( ProjectMetadataTag.class );

    private Object object;

    @Override
    public void release()
    {
        object = null;
        super.release();
    }

    @Override
    public int doStartTag()
        throws JspException
    {
        StringBuffer buf = new StringBuffer();

        if ( object == null )
        {
            buf.append( "Error generating project metadata." );
            log.error( "Unable to generate project metadata for null object." );
        }
        else if ( object instanceof ProjectVersionMetadata )
        {
            ProjectVersionMetadata metadata = (ProjectVersionMetadata) object;

            buildProjectMetadata( buf, metadata );
        }
        else
        {
            buf.append( "Unable to generate project metadata for object " ).append( object.getClass().getName() );
        }

        out( buf.toString() );

        return EVAL_BODY_INCLUDE;
    }

    private void out( String msg )
        throws JspException
    {
        try
        {
            pageContext.getOut().print( msg );
        }
        catch ( IOException e )
        {
            throw new JspException( "Unable to output to jsp page context." );
        }
    }

    private void buildProjectMetadata( StringBuffer metadataEntries, ProjectVersionMetadata projectMetadata )
    {
        startList( metadataEntries );

        addListItem( "project.metadata.id=", projectMetadata.getId(), metadataEntries );
        addListItem( "project.url=", projectMetadata.getUrl(), metadataEntries );
        addListItem( "project.name=", projectMetadata.getName(), metadataEntries );
        addListItem( "project.description=", projectMetadata.getDescription(), metadataEntries );
        
        if ( projectMetadata.getOrganization() != null )
        {
            startListItem( "organization", metadataEntries );
            startList( metadataEntries );
            addListItem( "organization.name=", projectMetadata.getOrganization().getName(), metadataEntries );
            addListItem( "organization.url=", projectMetadata.getOrganization().getUrl(), metadataEntries );
            endList( metadataEntries );
            endListItem( metadataEntries );
        }
        
        if ( projectMetadata.getIssueManagement() != null )
        {
            startListItem( "issueManagement", metadataEntries );
            startList( metadataEntries );
            addListItem( "issueManagement.system=", projectMetadata.getIssueManagement().getSystem(), metadataEntries );
            addListItem( "issueManagement.url=", projectMetadata.getIssueManagement().getUrl(), metadataEntries );
            endList( metadataEntries );
            endListItem( metadataEntries );
        }
        
        if ( projectMetadata.getScm() != null )
        {
            startListItem( "scm", metadataEntries );
            startList( metadataEntries );
            addListItem( "scm.url=", projectMetadata.getScm().getUrl(), metadataEntries );
            addListItem( "scm.connection=", projectMetadata.getScm().getConnection(), metadataEntries );
            addListItem( "scm.developer.connection=", projectMetadata.getScm().getDeveloperConnection(),
                         metadataEntries );
            endList( metadataEntries );
            endListItem( metadataEntries );
        }
        
        if ( projectMetadata.getCiManagement() != null )
        {
            startListItem( "ciManagement", metadataEntries );
            startList( metadataEntries );
            addListItem( "ciManagement.system=", projectMetadata.getCiManagement().getSystem(), metadataEntries );
            addListItem( "ciManagement.url=", projectMetadata.getCiManagement().getUrl(), metadataEntries );
            endList( metadataEntries );
            endListItem( metadataEntries );
        }
        
        if ( projectMetadata.getLicenses() != null && !projectMetadata.getLicenses().isEmpty() )
        {
            startListItem( "licenses", metadataEntries );
            List<License> licenses = projectMetadata.getLicenses();
            int ctr = 0;
            startList( metadataEntries );
            for ( License license : licenses )
            {
                addListItem( "licenses." + ctr + ".name=", license.getName(), metadataEntries );
                addListItem( "licenses." + ctr + ".url=", license.getUrl(), metadataEntries );
                ctr++;
            }
            endList( metadataEntries );
            endListItem( metadataEntries );
        }
        
        if ( projectMetadata.getMailingLists() != null && !projectMetadata.getMailingLists().isEmpty() )
        {
            startListItem( "mailingLists", metadataEntries );
            List<MailingList> lists = projectMetadata.getMailingLists();
            List<String> otherArchives;
            int ctr = 0;
            int archiveCtr = 0;

            startList( metadataEntries );
            for ( MailingList list : lists )
            {
                addListItem( "mailingLists." + ctr + ".name=", list.getName(), metadataEntries );
                addListItem( "mailingLists." + ctr + ".archive.url=", list.getMainArchiveUrl(), metadataEntries );
                addListItem( "mailingLists." + ctr + ".post=", list.getPostAddress(), metadataEntries );
                addListItem( "mailingLists." + ctr + ".subscribe=", list.getSubscribeAddress(), metadataEntries );
                addListItem( "mailingLists." + ctr + ".unsubscribe=", list.getUnsubscribeAddress(), metadataEntries );
                startListItem( "mailingLists." + ctr + ".otherArchives", metadataEntries );

                if ( list.getOtherArchives() != null && list.getOtherArchives().size() > 0 )
                {
                    archiveCtr = 0;
                    otherArchives = list.getOtherArchives();

                    startList( metadataEntries );
                    for ( String archive : otherArchives )
                    {
                        addListItem( "mailingLists." + ctr + ".otherArchives." + archiveCtr + "=", archive,
                                     metadataEntries );
                        metadataEntries.append( archive );
                        archiveCtr++;
                    }
                    endList( metadataEntries );
                }
                endListItem( metadataEntries );
                ctr++;
            }
            endList( metadataEntries );
            endListItem( metadataEntries );
        }
        
        if ( projectMetadata.getDependencies() != null && !projectMetadata.getDependencies().isEmpty() )
        {
            startListItem( "dependencies", metadataEntries );
            List<Dependency> dependencies = projectMetadata.getDependencies();
            int ctr = 0;

            startList( metadataEntries );
            for ( Dependency dependency : dependencies )
            {
                addListItem( "dependency." + ctr + ".group.id=", dependency.getGroupId(), metadataEntries );
                addListItem( "dependency." + ctr + ".artifact.id=", dependency.getArtifactId(), metadataEntries );
                addListItem( "dependency." + ctr + ".version=", dependency.getVersion(), metadataEntries );
                addListItem( "dependency." + ctr + ".classifier=", dependency.getClassifier(), metadataEntries );
                addListItem( "dependency." + ctr + ".type=", dependency.getType(), metadataEntries );
                addListItem( "dependency." + ctr + ".scope=", dependency.getScope(), metadataEntries );
                addListItem( "dependency." + ctr + ".system.path=", dependency.getSystemPath(), metadataEntries );
                ctr++;
            }
            endList( metadataEntries );
            endListItem( metadataEntries );
        }

        endList( metadataEntries );
    }

    private void startList( StringBuffer metadataEntries )
    {
        metadataEntries.append( "\n<ul>" );
    }

    private void endList( StringBuffer metadataEntries )
    {
        metadataEntries.append( "\n</ul>" );
    }

    private void addListItem( String label, String value, StringBuffer metadataEntries )
    {
        String newValue = StringUtils.isEmpty( value ) ? "" : value;
        metadataEntries.append( "\n<li>" ).append( label ).append( newValue ).append( "</li>" );
    }

    private void startListItem( String value, StringBuffer metadataEntries )
    {
        metadataEntries.append( "\n<li>" ).append( value );
    }

    private void endListItem( StringBuffer metadataEntries )
    {
        metadataEntries.append( "\n</li>" );
    }

    public void setObject( Object object )
    {
        this.object = object;
    }
}
