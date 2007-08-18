package org.apache.maven.archiva.database.browsing;

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
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ProjectsByArtifactUsageConstraint;
import org.apache.maven.archiva.database.constraints.UniqueArtifactIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueGroupIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.database.updater.DatabaseUpdater;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

/**
 * DefaultRepositoryBrowsing
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.database.browsing.RepositoryBrowsing"
 */
public class DefaultRepositoryBrowsing
    extends AbstractLogEnabled
    implements RepositoryBrowsing
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private DatabaseUpdater dbUpdater;

    public BrowsingResults getRoot()
    {
        List groups = dao.query( new UniqueGroupIdConstraint() );

        BrowsingResults results = new BrowsingResults();

        results.setGroupIds( GroupIdFilter.filterGroups( groups ) );

        return results;
    }

    public BrowsingResults selectArtifactId( String groupId, String artifactId )
    {
        // NOTE: No group Id or artifact Id's should be returned here. 
        List versions = dao.query( new UniqueVersionConstraint( groupId, artifactId ) );

        BrowsingResults results = new BrowsingResults( groupId, artifactId );

        processSnapshots( versions );

        results.setVersions( versions );

        return results;
    }

    public BrowsingResults selectGroupId( String groupId )
    {
        List groups = dao.query( new UniqueGroupIdConstraint( groupId ) );
        List artifacts = dao.query( new UniqueArtifactIdConstraint( groupId ) );

        BrowsingResults results = new BrowsingResults( groupId );

        // Remove searched for groupId from groups list.
        // Easier to do this here, vs doing it in the SQL query.
        CollectionUtils.filter( groups, NotPredicate.getInstance( PredicateUtils.equalPredicate( groupId ) ) );

        results.setGroupIds( groups );
        results.setArtifacts( artifacts );

        return results;
    }

    public ArchivaProjectModel selectVersion( String groupId, String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaArtifact pomArtifact = getArtifact( groupId, artifactId, version );

        ArchivaProjectModel model;
        version = pomArtifact.getVersion();

        if ( !pomArtifact.getModel().isProcessed() )
        {
            // Process it.
            dbUpdater.updateUnprocessed( pomArtifact );
        }

        model = getProjectModel( groupId, artifactId, version );

        return model;
    }

    private ArchivaArtifact getArtifact( String groupId, String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaArtifact pomArtifact = null;

        try
        {
            pomArtifact = dao.getArtifactDAO().getArtifact( groupId, artifactId, version, null, "pom" );
        }
        catch ( ObjectNotFoundException e )
        {
            pomArtifact = handleGenericSnapshots( groupId, artifactId, version, pomArtifact );
        }

        if ( pomArtifact == null )
        {
            throw new ObjectNotFoundException(
                "Unable to find artifact [" + groupId + ":" + artifactId + ":" + version + "]" );
        }
        return pomArtifact;
    }

    public List getUsedBy( String groupId, String artifactId, String version )
        throws ArchivaDatabaseException
    {
        ProjectsByArtifactUsageConstraint constraint =
            new ProjectsByArtifactUsageConstraint( groupId, artifactId, version );
        List results = dao.getProjectModelDAO().queryProjectModels( constraint );
        if ( results == null )
        {
            // defensive. to honor contract as specified. never null.
            return Collections.EMPTY_LIST;
        }

        return results;
    }

    /**
     * Add generic (*-SNAPSHOT) snapshot versions in the list for artifacts with only unique version (timestamped)
     * snapshots.
     * <p/>
     * Ex.
     * artifact1 has the ff. versions retrieved from the db:
     * - 1.0
     * - 1.1-20061118.060401-2
     * - 1.1-20061118.060402-3
     * - 2.2-20071007.070101-1
     * - 2.2-20071007.070110-2
     * - 2.2-SNAPSHOT
     * <p/>
     * This method will add a '1.1-SNAPSHOT' in the list since there is no generic snapshot entry for it.
     * When this version is browsed, the pom of the latest snapshot will be displayed.
     *
     * @param versions
     */
    private void processSnapshots( List versions )
    {
        Map snapshots = new HashMap();

        getLogger().info( "Processing snapshots." );

        for ( Iterator iter = versions.iterator(); iter.hasNext(); )
        {
            String version = (String) iter.next();
            if ( VersionUtil.isSnapshot( version ) )
            {
                String baseVersion = VersionUtil.getBaseVersion( version );
                if ( !snapshots.containsKey( baseVersion ) )
                {
                    snapshots.put( baseVersion, baseVersion );
                }
            }
        }

        for ( Iterator it = ( snapshots.entrySet() ).iterator(); it.hasNext(); )
        {
            String baseVersion = (String) ( (Map.Entry) it.next() ).getValue();
            if ( !versions.contains( baseVersion ) )
            {
                versions.add( baseVersion );
            }
        }
    }

    /**
     * Handles querying of generic (*-SNAPSHOT) snapshot version.
     * Process:
     * - Get all the timestamped/unique versions of the artifact from the db
     * - Sort the queried project models
     * - Reverse the list of queried project models to get the latest timestamp version
     * - Loop through the list and get the first one to match the generic (*-SNAPHOT) version
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @param pomArtifact
     * @throws ArchivaDatabaseException
     */
    private ArchivaArtifact handleGenericSnapshots( String groupId, String artifactId, String version,
                                                    ArchivaArtifact pomArtifact )
        throws ArchivaDatabaseException
    {
        if ( VersionUtil.isGenericSnapshot( version ) )
        {
            List versions = dao.query( new UniqueVersionConstraint( groupId, artifactId ) );
            Collections.sort( versions );
            Collections.reverse( versions );

            for ( Iterator iter = versions.iterator(); iter.hasNext(); )
            {
                String uniqueVersion = (String) iter.next();

                if ( VersionUtil.getBaseVersion( uniqueVersion ).equals( version ) )
                {
                    getLogger().info( "Retrieving artifact with version " + uniqueVersion );
                    pomArtifact = dao.getArtifactDAO().getArtifact( groupId, artifactId, uniqueVersion, null, "pom" );

                    return pomArtifact;
                }
            }
        }

        return null;
    }

    /**
     * Get the project model from the database.
     *
     * @param groupId
     * @param artifactId
     * @param version
     * @return
     * @throws ArchivaDatabaseException
     */
    private ArchivaProjectModel getProjectModel( String groupId, String artifactId, String version )
        throws ArchivaDatabaseException
    {
        try
        {
            ArchivaProjectModel model = dao.getProjectModelDAO().getProjectModel( groupId, artifactId, version );

            if ( model == null )
            {
                throw new ObjectNotFoundException(
                    "Unable to find project model for [" + groupId + ":" + artifactId + ":" + version + "]" );
            }

            return model;
        }
        catch ( ObjectNotFoundException e )
        {
            throw e;
        }
    }

}
