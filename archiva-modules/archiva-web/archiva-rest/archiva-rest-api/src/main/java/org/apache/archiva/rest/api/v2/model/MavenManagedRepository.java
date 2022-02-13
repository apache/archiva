package org.apache.archiva.rest.api.v2.model;/*
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

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;

import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="MavenManagedRepository",description = "A managed repository stores artifacts locally")
public class MavenManagedRepository extends Repository
{
    private static final long serialVersionUID = -6853748886201905029L;

    boolean blocksRedeployments;
    List<String> releaseSchemes = new ArrayList<>(  );
    boolean deleteSnapshotsOfRelease = false;
    private Period retentionPeriod;
    private int retentionCount;
    private String indexPath;
    private String packedIndexPath;
    private boolean skipPackedIndexCreation;
    private boolean hasStagingRepository;
    private String stagingRepository;


    public MavenManagedRepository( )
    {
        super.setCharacteristic( Repository.CHARACTERISTIC_MANAGED );
        super.setType( RepositoryType.MAVEN.name( ) );
    }

    @Schema(name="blocks_redeployments",description = "True, if redeployments to this repository are not allowed")
    public boolean isBlocksRedeployments( )
    {
        return blocksRedeployments;
    }

    public void setBlocksRedeployments( boolean blocksRedeployments )
    {
        this.blocksRedeployments = blocksRedeployments;
    }

    @Schema(name="release_schemes", description = "The release schemes this repository is used for (e.g. RELEASE, SNAPSHOT)")
    public List<String> getReleaseSchemes( )
    {
        return releaseSchemes;
    }

    public void setReleaseSchemes( List<String> releaseSchemes )
    {
        this.releaseSchemes = new ArrayList<>( releaseSchemes );
    }

    public void addReleaseScheme(String scheme) {
        if (!this.releaseSchemes.contains( scheme ))
        {
            this.releaseSchemes.add( scheme );
        }
    }

    @Schema(name="delete_snaphots_of_release", description = "True, if snapshots are deleted, after a version is released")
    public boolean isDeleteSnapshotsOfRelease( )
    {
        return deleteSnapshotsOfRelease;
    }

    public void setDeleteSnapshotsOfRelease( boolean deleteSnapshotsOfRelease )
    {
        this.deleteSnapshotsOfRelease = deleteSnapshotsOfRelease;
    }

    @Schema(name="retention_period", description = "The period after which snapshots are deleted.")
    public Period getRetentionPeriod( )
    {
        return retentionPeriod;
    }

    public void setRetentionPeriod( Period retentionPeriod )
    {
        this.retentionPeriod = retentionPeriod;
    }

    @Schema(name="retention_count", description = "Number of snapshot artifacts to keep.")
    public int getRetentionCount( )
    {
        return retentionCount;
    }

    public void setRetentionCount( int retentionCount )
    {
        this.retentionCount = retentionCount;
    }

    @Schema( name = "index_path", description = "Path to the directory that contains the index, relative to the repository base directory" )
    public String getIndexPath( )
    {
        return indexPath;
    }

    public void setIndexPath( String indexPath )
    {
        this.indexPath = indexPath;
    }

    @Schema( name = "packed_index_path", description = "Path to the directory that contains the packed index, relative to the repository base directory" )
    public String getPackedIndexPath( )
    {
        return packedIndexPath;
    }

    public void setPackedIndexPath( String packedIndexPath )
    {
        this.packedIndexPath = packedIndexPath;
    }

    @Schema(name="skip_packed_index_creation", description = "True, if packed index is not created during index update")
    public boolean isSkipPackedIndexCreation( )
    {
        return skipPackedIndexCreation;
    }

    public void setSkipPackedIndexCreation( boolean skipPackedIndexCreation )
    {
        this.skipPackedIndexCreation = skipPackedIndexCreation;
    }

    @Schema(name="has_staging_repository", description = "True, if this repository has a staging repository assigned")
    public boolean hasStagingRepository( )
    {
        return hasStagingRepository;
    }

    public void setHasStagingRepository( boolean hasStagingRepository )
    {
        this.hasStagingRepository = hasStagingRepository;
    }

    @Schema(name="staging_repository", description = "The id of the assigned staging repository")
    public String getStagingRepository( )
    {
        return stagingRepository;
    }

    public void setStagingRepository( String stagingRepository )
    {
        this.stagingRepository = stagingRepository;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        MavenManagedRepository that = (MavenManagedRepository) o;

        if ( blocksRedeployments != that.blocksRedeployments ) return false;
        return releaseSchemes != null ? releaseSchemes.equals( that.releaseSchemes ) : that.releaseSchemes == null;
    }

    @Override
    public int hashCode( )
    {
        int result = super.hashCode( );
        result = 31 * result + ( blocksRedeployments ? 1 : 0 );
        result = 31 * result + ( releaseSchemes != null ? releaseSchemes.hashCode( ) : 0 );
        return result;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "ManagedRepository{" );
        sb.append( "blocksRedeployments=" ).append( blocksRedeployments );
        sb.append( ", releaseSchemes=" ).append( releaseSchemes );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", type='" ).append( type ).append( '\'' );
        sb.append( ", location='" ).append( location ).append( '\'' );
        sb.append( ", scanned=" ).append( scanned );
        sb.append( ", schedulingDefinition='" ).append( schedulingDefinition ).append( '\'' );
        sb.append( ", index=" ).append( index );
        sb.append( ", layout='" ).append( layout ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
