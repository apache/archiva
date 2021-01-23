package org.apache.archiva.rest.api.model.v2;/*
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="repositoryGroup")
@Schema(name="RepositoryGroup", description = "Information about a repository group, which combines multiple repositories as one virtual repository.")
public class RepositoryGroup implements Serializable
{
    private static final long serialVersionUID = -7319687481737616081L;
    private String id;
    private List<String> repositories = new ArrayList<>(  );
    private String location;
    MergeConfiguration mergeConfiguration;

    public RepositoryGroup( )
    {
    }

    public RepositoryGroup(String id) {
        this.id = id;
    }

    public static RepositoryGroup of( org.apache.archiva.admin.model.beans.RepositoryGroup modelObj ) {
        RepositoryGroup result = new RepositoryGroup( );
        MergeConfiguration mergeConfig = new MergeConfiguration( );
        result.setMergeConfiguration( mergeConfig );
        result.setId( modelObj.getId() );
        result.setLocation( modelObj.getLocation() );
        result.setRepositories( modelObj.getRepositories() );
        mergeConfig.setMergedIndexPath( modelObj.getMergedIndexPath() );
        mergeConfig.setMergedIndexTtlMinutes( modelObj.getMergedIndexTtl( ) );
        mergeConfig.setIndexMergeSchedule( modelObj.getCronExpression( ) );
        return result;
    }

    @Schema(description = "The unique id of the repository group.")
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema(description = "The list of ids of repositories which are member of the repository group.")
    public List<String> getRepositories( )
    {
        return repositories;
    }

    public void setRepositories( List<String> repositories )
    {
        this.repositories = new ArrayList<>( repositories );
    }

    public void addRepository(String repositoryId) {
        if (!this.repositories.contains( repositoryId )) {
            this.repositories.add( repositoryId );
        }
    }

    @Schema(name="merge_configuration",description = "The configuration for index merge.")
    public MergeConfiguration getMergeConfiguration( )
    {
        return mergeConfiguration;
    }

    public void setMergeConfiguration( MergeConfiguration mergeConfiguration )
    {
        this.mergeConfiguration = mergeConfiguration;
    }

    @Schema(description = "The storage location of the repository. The merged index is stored relative to this location.")
    public String getLocation( )
    {
        return location;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        RepositoryGroup that = (RepositoryGroup) o;

        if ( !Objects.equals( id, that.id ) ) return false;
        if ( !repositories.equals( that.repositories ) )
            return false;
        if ( !Objects.equals( location, that.location ) ) return false;
        return Objects.equals( mergeConfiguration, that.mergeConfiguration );
    }

    @Override
    public int hashCode( )
    {
        int result = id != null ? id.hashCode( ) : 0;
        result = 31 * result + repositories.hashCode( );
        result = 31 * result + ( location != null ? location.hashCode( ) : 0 );
        result = 31 * result + ( mergeConfiguration != null ? mergeConfiguration.hashCode( ) : 0 );
        return result;
    }

    @SuppressWarnings( "StringBufferReplaceableByString" )
    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "RepositoryGroup{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", repositories=" ).append( repositories );
        sb.append( ", location='" ).append( location ).append( '\'' );
        sb.append( ", mergeConfiguration=" ).append( mergeConfiguration );
        sb.append( '}' );
        return sb.toString( );
    }
}
