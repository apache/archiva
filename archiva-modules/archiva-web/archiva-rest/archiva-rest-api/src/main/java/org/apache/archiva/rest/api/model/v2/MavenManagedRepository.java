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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="MavenManagedRepository",description = "A managed repository stores artifacts locally")
public class MavenManagedRepository extends Repository
{
    private static final long serialVersionUID = -6853748886201905029L;

    boolean blocksRedeployments;
    List<String> releaseSchemes = new ArrayList<>(  );

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
