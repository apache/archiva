package org.apache.archiva.rest.api.v2.model.map;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.configuration.model.ManagedRepositoryConfiguration;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;
import org.apache.archiva.rest.api.v2.model.MavenManagedRepository;
import org.springframework.stereotype.Service;

/**
 * Mapping implementation for Maven managed repository to managed repository configuration
 *
 * @author Martin Schreier <martin_s@apache.org>
 */
@Service("mapper#managed_repository#maven")
public class MavenRepositoryMapper extends RestServiceMapper<MavenManagedRepository, ManagedRepositoryConfiguration, ManagedRepository>
{

    private static final String TYPE = RepositoryType.MAVEN.name( );

    @Override
    public ManagedRepositoryConfiguration map( MavenManagedRepository source )
    {
        ManagedRepositoryConfiguration target = new ManagedRepositoryConfiguration( );
        update( source, target );
        return target;
    }

    @Override
    public void update( MavenManagedRepository source, ManagedRepositoryConfiguration target )
    {
        target.setId( source.getId() );
        target.setName( source.getName() );
        target.setDescription( source.getDescription( ) );
        target.setType( TYPE );

        target.setBlockRedeployments( source.isBlocksRedeployments() );
        target.setDeleteReleasedSnapshots( source.isDeleteSnapshotsOfRelease() );
        target.setIndexDir( source.getIndexPath() );
        target.setLayout( source.getLayout() );
        target.setLocation( source.getLocation() );
        target.setPackedIndexDir( source.getPackedIndexPath() );
        target.setRefreshCronExpression( source.getSchedulingDefinition() );
        target.setReleases( source.getReleaseSchemes( ).contains( ReleaseScheme.RELEASE ) );
        target.setRetentionCount( source.getRetentionCount() );
        target.setRetentionPeriod( source.getRetentionPeriod().getDays() );
        target.setScanned( source.isScanned() );
        target.setSkipPackedIndexCreation( source.isSkipPackedIndexCreation() );
        target.setSnapshots( source.getReleaseSchemes( ).contains( ReleaseScheme.SNAPSHOT ) );
        target.setStageRepoNeeded( source.hasStagingRepository() );

    }

    @Override
    public MavenManagedRepository reverseMap( ManagedRepository source )
    {
        MavenManagedRepository result = new MavenManagedRepository( );
        StagingRepositoryFeature srf = source.getFeature( StagingRepositoryFeature.class ).get( );
        ArtifactCleanupFeature acf = source.getFeature( ArtifactCleanupFeature.class ).get( );
        result.setHasStagingRepository( srf.isStageRepoNeeded() );
        result.setBlocksRedeployments( source.blocksRedeployments() );
        result.setIndex( source.hasIndex() );
        result.setStagingRepository( srf.getStagingRepository().getId() );
        return null;
    }

    @Override
    public void reverseUpdate( ManagedRepository source, MavenManagedRepository target )
    {

    }

    @Override
    public Class<MavenManagedRepository> getBaseType( )
    {
        return MavenManagedRepository.class;
    }

    @Override
    public Class<ManagedRepositoryConfiguration> getDestinationType( )
    {
        return ManagedRepositoryConfiguration.class;
    }

    @Override
    public Class<ManagedRepository> getReverseSourceType( )
    {
        return ManagedRepository.class;
    }
}
