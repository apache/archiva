package org.apache.maven.archiva.web.util;

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

import org.apache.archiva.metadata.model.Dependency;
import org.apache.archiva.metadata.model.License;
import org.apache.archiva.metadata.model.MailingList;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * ProjectMetadataDisplayUtil
 *
 */

public class ProjectMetadataDisplayUtil
{
    private StringBuilder metadataEntries;
    
    public String formatProjectMetadata( ProjectVersionMetadata projectMetadata )
    {
        metadataEntries = new StringBuilder();
        
        startList();
        
        addListItem( "project.metadata.id=", projectMetadata.getId() );
        addListItem( "project.url=", projectMetadata.getUrl() );
        addListItem( "project.name=", projectMetadata.getName() );
        addListItem( "project.description=", projectMetadata.getDescription() );
        
        startListItem( "organization" );
        if ( projectMetadata.getOrganization() != null )
        {
            startList();
            addListItem( "organization.name=", projectMetadata.getOrganization().getName() );
            addListItem( "organization.url=", projectMetadata.getOrganization().getUrl() );
            endList();
        }
        endListItem();
         
        startListItem( "issueManagement" );
        if ( projectMetadata.getIssueManagement() != null )
        {
            startList();
            addListItem( "issueManagement.system=", projectMetadata.getIssueManagement().getSystem() );
            addListItem( "issueManagement.url=", projectMetadata.getIssueManagement().getUrl() );
            endList();
        }
        endListItem();
        
        startListItem( "scm" );
        if ( projectMetadata.getScm() != null )
        {
            startList();
            addListItem( "scm.url=", projectMetadata.getScm().getUrl() );
            addListItem( "scm.connection=", projectMetadata.getScm().getConnection() );
            addListItem( "scm.developer.connection=", projectMetadata.getScm().getDeveloperConnection() );
            endList();
        }
        endListItem();
        
        startListItem( "ciManagement" );
        if ( projectMetadata.getCiManagement() != null )
        {
            startList();
            addListItem( "ciManagement.system=", projectMetadata.getCiManagement().getSystem() );
            addListItem( "ciManagement.url=", projectMetadata.getCiManagement().getUrl() );
            endList();
        }
        endListItem();
        
        startListItem( "licenses" );
        if ( projectMetadata.getLicenses() != null )
        {
            List<License> licenses = projectMetadata.getLicenses();
            int ctr = 0;
            startList();
            for ( License license : licenses )
            {
                addListItem( "licenses." + ctr + ".name=", license.getName() );
                addListItem( "licenses." + ctr + ".url=", license.getUrl() );
                ctr++;
            }
            endList();
        }
        endListItem();
         
        startListItem( "mailingLists" );
        if ( projectMetadata.getMailingLists() != null )
        {
            List<MailingList> lists = projectMetadata.getMailingLists();
            List<String> otherArchives;
            int ctr = 0;
            int archiveCtr = 0;
            
            startList();
            for ( MailingList list : lists )
            {
                addListItem( "mailingLists." + ctr + ".name=", list.getName() );
                addListItem( "mailingLists." + ctr + ".archive.url=", list.getMainArchiveUrl() );
                addListItem( "mailingLists." + ctr + ".post=", list.getPostAddress() );
                addListItem( "mailingLists." + ctr + ".subscribe=", list.getSubscribeAddress() );
                addListItem( "mailingLists." + ctr + ".unsubscribe=", list.getUnsubscribeAddress() );
                startListItem( "mailingLists." + ctr + ".otherArchives" );
                
                if ( list.getOtherArchives() != null && list.getOtherArchives().size() > 0 )
                {
                    archiveCtr = 0;
                    otherArchives = list.getOtherArchives();
                    
                    startList();
                    for ( String archive : otherArchives )
                    {
                        addListItem( "mailingLists." + ctr + ".otherArchives." + archiveCtr + "=", archive );
                        metadataEntries.append( archive );
                        archiveCtr++;
                    }
                    endList();
                }
                endListItem();
                ctr++;
            }
            endList();
        }
        endListItem();
        
        startListItem( "dependencies" );
        if ( projectMetadata.getDependencies() != null )
        {
            List<Dependency> dependencies = projectMetadata.getDependencies();
            int ctr = 0;
            
            startList();
            for ( Dependency dependency : dependencies )
            {
                addListItem( "dependency." + ctr + ".group.id=", dependency.getGroupId() );
                addListItem( "dependency." + ctr + ".artifact.id=", dependency.getArtifactId() );
                addListItem( "dependency." + ctr + ".version=", dependency.getVersion() );
                addListItem( "dependency." + ctr + ".classifier=", dependency.getClassifier() );
                addListItem( "dependency." + ctr + ".type=", dependency.getType() );
                addListItem( "dependency." + ctr + ".scope=", dependency.getScope() );
                addListItem( "dependency." + ctr + ".system.path=", dependency.getSystemPath() );
                ctr++;
            }
            endList();
        }
        endListItem();
        
        endList();
        
        return metadataEntries.toString();
    }
    
    private void startList()
    {
        metadataEntries.append( "\n<ul>" );
    }
     
    private void endList()
    {
        metadataEntries.append( "\n</ul>" );
    }
    
    private void addListItem( String label, String value )
    {
        String newValue = StringUtils.isEmpty( value ) ? "" : value;
        metadataEntries.append( "\n<li>" ).append( label ).append( newValue ).append( "</li>" );
    }
    
    private void startListItem( String value )
    {
        metadataEntries.append( "\n<li>" ).append( value );
    }
    
    private void endListItem()
    {
        metadataEntries.append( "\n</li>" );
    }
}

