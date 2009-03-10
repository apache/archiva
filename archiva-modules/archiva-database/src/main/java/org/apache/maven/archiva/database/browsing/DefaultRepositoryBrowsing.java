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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.functors.NotPredicate;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.ArtifactsRelatedConstraint;
import org.apache.maven.archiva.database.constraints.ProjectsByArtifactUsageConstraint;
import org.apache.maven.archiva.database.constraints.UniqueArtifactIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueGroupIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.database.updater.DatabaseUpdater;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DefaultRepositoryBrowsing
 *
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.database.browsing.RepositoryBrowsing"
 */
public class DefaultRepositoryBrowsing
    implements RepositoryBrowsing
{
    private Logger log = LoggerFactory.getLogger( DefaultRepositoryBrowsing.class );

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement role-hint="jdo"
     */
    private DatabaseUpdater dbUpdater;

    /**
     * @see RepositoryBrowsing#getRoot(String, List)
     */
    public BrowsingResults getRoot( final String principal, final List<String> observableRepositoryIds )
    {
        final BrowsingResults results = new BrowsingResults();

        if ( !observableRepositoryIds.isEmpty() )
        {
            final List<String> groups = dao.query( new UniqueGroupIdConstraint( observableRepositoryIds ) );
            results.setSelectedRepositoryIds( observableRepositoryIds );
            results.setGroupIds( GroupIdFilter.filterGroups( groups ) );
        }
        return results;
    }

    /**
     * @see RepositoryBrowsing#selectArtifactId(String, List, String, String)
     */
    public BrowsingResults selectArtifactId( final String principal, final List<String> observableRepositoryIds,
                                             final String groupId, final String artifactId )
    {
        final BrowsingResults results = new BrowsingResults( groupId, artifactId );

        if ( !observableRepositoryIds.isEmpty() )
        {
            // NOTE: No group Id or artifact Id's should be returned here.
            List<String> versions =
                dao.query( new UniqueVersionConstraint( observableRepositoryIds, groupId, artifactId ) );
            results.setSelectedRepositoryIds( observableRepositoryIds );

            results.setVersions( processSnapshots( versions ) );
        }
        return results;
    }

    /**
     * @see RepositoryBrowsing#selectGroupId(String, List, String)
     */
    public BrowsingResults selectGroupId( final String principal, final List<String> observableRepositoryIds,
                                          final String groupId )
    {
        final BrowsingResults results = new BrowsingResults( groupId );

        if ( !observableRepositoryIds.isEmpty() )
        {
            final List<String> groups = dao.query( new UniqueGroupIdConstraint( observableRepositoryIds, groupId ) );
            final List<String> artifacts =
                dao.query( new UniqueArtifactIdConstraint( observableRepositoryIds, groupId ) );

            // Remove searched for groupId from groups list.
            // Easier to do this here, vs doing it in the SQL query.
            CollectionUtils.filter( groups, NotPredicate.getInstance( PredicateUtils.equalPredicate( groupId ) ) );

            results.setGroupIds( groups );
            results.setArtifacts( artifacts );
        }

        return results;
    }

    /**
     * @see RepositoryBrowsing#selectVersion(String, List, String, String, String)
     */
    public ArchivaProjectModel selectVersion( final String principal, final List<String> observableRepositoryIds,
                                              final String groupId, final String artifactId, final String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( observableRepositoryIds.isEmpty() )
        {
            throw new ArchivaDatabaseException( "There are no observable repositories for the user " + principal );
        }

        ArchivaArtifact pomArtifact = getArtifact( principal, observableRepositoryIds, groupId, artifactId, version );

        ArchivaProjectModel model;

        if ( !pomArtifact.getModel().isProcessed() )
        {
            // Process it.
            dbUpdater.updateUnprocessed( pomArtifact );
        }

        model = getProjectModel( groupId, artifactId, pomArtifact.getVersion() );

        return model;
    }

    public String getRepositoryId( final String principal, final List<String> observableRepositoryIds,
                                   final String groupId, final String artifactId, final String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        if ( observableRepositoryIds.isEmpty() )
        {
            throw new ArchivaDatabaseException( "There are no observable repositories for the user " + principal );
        }

        try
        {
            ArchivaArtifact pomArchivaArtifact =
                getArtifact( principal, observableRepositoryIds, groupId, artifactId, version );

            return pomArchivaArtifact.getModel().getRepositoryId();
        }
        catch ( ObjectNotFoundException e )
        {
            return getNoPomArtifactRepoId( principal, observableRepositoryIds, groupId, artifactId, version,
                                           observableRepositoryIds.get( 0 ) );
        }
    }

    /**
     * @see RepositoryBrowsing#getOtherSnapshotVersions(List, String, String, String)
     */
    public List<String> getOtherSnapshotVersions( List<String> observableRepositoryIds, String groupId,
                                                 String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        List<String> timestampedVersions = new ArrayList<String>();

        if ( VersionUtil.isSnapshot( version ) )
        {
            List<String> versions =
                dao.query( new UniqueVersionConstraint( observableRepositoryIds, groupId, artifactId ) );

            for ( String uniqueVersion : versions )
            {   
                if ( VersionUtil.getBaseVersion( uniqueVersion ).equals( version ) || 
                        VersionUtil.getBaseVersion( uniqueVersion ).equals( VersionUtil.getBaseVersion( version ) ) )
                {
                    if ( !timestampedVersions.contains( uniqueVersion ) )
                    {
                        timestampedVersions.add( uniqueVersion );
                    }
                }      
            }
        }

        return timestampedVersions;
    }

    private ArchivaArtifact getArtifact( final String principal, final List<String> observableRepositoryIds,
                                         final String groupId, final String artifactId, final String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaArtifact pomArtifact = null;

        for ( final String repositoryId : observableRepositoryIds )
        {
            try
            {
                pomArtifact =
                    dao.getArtifactDAO().getArtifact( groupId, artifactId, version, null, "pom", repositoryId );               
                break;
            }
            catch ( ArchivaDatabaseException e )
            {
                pomArtifact = handleGenericSnapshots( groupId, artifactId, version, repositoryId );
            }
        }

        if ( pomArtifact == null )
        {
            String type = getArtifactType( groupId, artifactId, version );

            // We dont want these to persist in the database
            pomArtifact =
                new ArchivaArtifact( groupId, artifactId, version, null, type, observableRepositoryIds.get( 0 ) );
            pomArtifact.getModel().setWhenProcessed( new Date() );
        }

        // Allowed to see this?
        if ( observableRepositoryIds.contains( pomArtifact.getModel().getRepositoryId() ) )
        {
            return pomArtifact;
        }
        else
        {
            throw new ObjectNotFoundException( "Unable to find artifact " + Keys.toKey( groupId, artifactId, version ) +
                " in observable repository [" + StringUtils.join( observableRepositoryIds.iterator(), ", " ) +
                "] for user " + principal );
        }
    }

    public List<ArchivaProjectModel> getUsedBy( final String principal, final List<String> observableRepositoryIds,
                                                final String groupId, final String artifactId, final String version )
        throws ArchivaDatabaseException
    {
        ProjectsByArtifactUsageConstraint constraint =
            new ProjectsByArtifactUsageConstraint( groupId, artifactId, version );
        List<ArchivaProjectModel> results = dao.getProjectModelDAO().queryProjectModels( constraint );
        if ( results == null )
        {
            // defensive. to honor contract as specified. never null.
            return Collections.EMPTY_LIST;
        }

        return results;
    }

    /**
     * Removes SNAPSHOT versions with build numbers. Retains only the generic SNAPSHOT version. 
     * Example, if the list of versions are: 
     * - 2.0 
     * - 2.0.1 
     * - 2.1-20070522.143249-1 
     * - 2.1-20070522.157829-2 
     * 
     * the returned version list would contain 2.0, 2.0.1 and 2.1-SNAPSHOT.
     * 
     * @param versions
     */
    private List<String> processSnapshots( List<String> versions )
    {
        List<String> cleansedVersions = new ArrayList<String>();

        for ( String version : versions )
        {
            if ( VersionUtil.isSnapshot( version ) )
            {   
                String baseVersion = VersionUtil.getBaseVersion( version );
                if ( !cleansedVersions.contains( baseVersion ) )
                {
                    cleansedVersions.add( baseVersion );
                }
            }
            else
            {
                cleansedVersions.add( version );
            }
        }

        return cleansedVersions;
    }

    /**
     * Handles querying of generic (*-SNAPSHOT) snapshot version. Process: - Get all the timestamped/unique versions of
     * the artifact from the db - Sort the queried project models - Reverse the list of queried project models to get
     * the latest timestamp version - Loop through the list and get the first one to match the generic (*-SNAPHOT)
     * version
     * 
     * @param groupId
     * @param artifactId
     * @param version
     * @param pomArtifact
     * @throws ArchivaDatabaseException
     */
    private ArchivaArtifact handleGenericSnapshots( final String groupId, final String artifactId,
                                                    final String version, final String repositoryId )
        throws ArchivaDatabaseException
    {
        ArchivaArtifact result = null;

        if ( VersionUtil.isGenericSnapshot( version ) )
        {
            final List<String> versions = dao.query( new UniqueVersionConstraint( groupId, artifactId ) );
            Collections.sort( versions );
            Collections.reverse( versions );

            for ( String uniqueVersion : versions )
            {
                if ( VersionUtil.getBaseVersion( uniqueVersion ).equals( version ) )
                {
                    try
                    {
                        log.debug( "Retrieving artifact with version " + uniqueVersion );
                        result =
                            dao.getArtifactDAO().getArtifact( groupId, artifactId, uniqueVersion, null, "pom", repositoryId );
                        break;
                    }
                    catch ( ObjectNotFoundException e )
                    {
                        log.debug( "Artifact '" + groupId + ":" + artifactId + ":" + uniqueVersion +
                            "' in repository '" + repositoryId + "' not found in the database." );
                        continue;
                    }
                }
            }
        }
        return result;
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
        ArchivaProjectModel model = null;

        try
        {
            model = dao.getProjectModelDAO().getProjectModel( groupId, artifactId, version );
        }
        catch ( ObjectNotFoundException e )
        {
            log.debug( "Unable to find project model for [" + Keys.toKey( groupId, artifactId, version ) + "]", e );
        }

        if ( model == null )
        {
            model = new ArchivaProjectModel();
            model.setGroupId( groupId );
            model.setArtifactId( artifactId );
            model.setVersion( version );
        }

        return model;
    }

    private String getNoPomArtifactRepoId( String principal, List<String> observableRepos, String groupId,
                                           String artifactId, String version, String repositoryId )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        ArchivaArtifact artifact = null;

        String type = getArtifactType( groupId, artifactId, version );

        artifact = dao.getArtifactDAO().createArtifact( groupId, artifactId, version, null, type, repositoryId );

        if ( artifact == null )
        {
            // Lets not persist these
            artifact = new ArchivaArtifact( groupId, artifactId, version, null, type, repositoryId );
        }

        // Allowed to see this?
        if ( !observableRepos.contains( artifact.getModel().getRepositoryId() ) )
        {
            throw new ObjectNotFoundException( "Unable to find artifact " + Keys.toKey( groupId, artifactId, version ) +
                " in observable repository [" + StringUtils.join( observableRepos.iterator(), ", " ) + "] for user " +
                principal );
        }

        return artifact.getModel().getRepositoryId();
    }

    private String getArtifactType( String groupId, String artifactId, String version )
        throws ObjectNotFoundException, ArchivaDatabaseException
    {
        String type = "jar";

        try
        {
            List<ArchivaArtifact> artifacts =
                dao.getArtifactDAO().queryArtifacts( new ArtifactsRelatedConstraint( groupId, artifactId, version ) );

            if ( artifacts.size() > 0 )
            {
                type = artifacts.get( 0 ).getType();
            }
        }
        catch ( ObjectNotFoundException e )
        {
            // swallow exception?
        }

        return type;
    }

}
