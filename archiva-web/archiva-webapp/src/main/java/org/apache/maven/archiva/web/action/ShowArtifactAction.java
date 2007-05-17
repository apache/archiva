package org.apache.maven.archiva.web.action;

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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.browsing.RepositoryBrowsing;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.web.util.VersionMerger;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Browse the repository.
 *
 * TODO change name to ShowVersionedAction to conform to terminology.
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="showArtifactAction"
 */
public class ShowArtifactAction
    extends PlexusActionSupport
{
    /* .\ Not Exposed \._____________________________________________ */
    
    /**
     * @plexus.requirement role-hint="default"
     */
    private RepositoryBrowsing repoBrowsing;

    /* .\ Input Parameters \.________________________________________ */

    private String groupId;

    private String artifactId;

    private String version;

    /* .\ Exposed Output Objects \.__________________________________ */

    /**
     * The model of this versioned project.
     */
    private ArchivaProjectModel model;

    /**
     * The list of artifacts that depend on this versioned project.
     */
    private List dependees;

    /**
     * The reports associated with this versioned project.
     */
    private List reports;

    /**
     * Show the versioned project information tab.
     * 
     * TODO: Change name to 'project'
     */
    public String artifact()
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        this.model = readProject();

        return SUCCESS;
    }

    /**
     * Show the artifact information tab.
     */
    public String dependencies()
    throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        this.model = readProject();

        // TODO: should this be the whole set of artifacts, and be more like the maven dependencies report?
        // this.dependencies = VersionMerger.wrap( project.getModel().getDependencies() );

        return SUCCESS;
    }

    /**
     * Show the mailing lists information tab.
     */
    public String mailingLists()
    throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        this.model = readProject();

        return SUCCESS;
    }
    
    /**
     * Show the reports tab.
     */
    public String reports()
    throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        System.out.println("#### In reports.");
        // TODO: hook up reports on project - this.reports = artifactsDatabase.findArtifactResults( groupId, artifactId, version );
        System.out.println("#### Found " + reports.size() + " reports.");

        return SUCCESS;
    }

    /**
     * Show the dependees (other artifacts that depend on this project) tab.
     */
    public String dependees()
    throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        this.model = readProject();

        // TODO: create depends on collector.
        this.dependees = Collections.EMPTY_LIST;

        return SUCCESS;
    }

    /**
     * Show the dependencies of this versioned project tab.
     */
    public String dependencyTree()
    throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( !checkParameters() )
        {
            return ERROR;
        }

        this.model = readProject();

        return SUCCESS;
    }

    private ArchivaProjectModel readProject()
        throws ArchivaDatabaseException
    {
        return repoBrowsing.selectVersion( groupId, artifactId, version );
    }

    private boolean checkParameters()
    {
        boolean result = true;

        if ( StringUtils.isEmpty( groupId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a group ID to browse" );
            result = false;
        }

        else if ( StringUtils.isEmpty( artifactId ) )
        {
            // TODO: i18n
            addActionError( "You must specify a artifact ID to browse" );
            result = false;
        }

        else if ( StringUtils.isEmpty( version ) )
        {
            // TODO: i18n
            addActionError( "You must specify a version to browse" );
            result = false;
        }
        return result;
    }

    public ArchivaProjectModel getModel()
    {
        return model;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public List getReports()
    {
        return reports;
    }
}
