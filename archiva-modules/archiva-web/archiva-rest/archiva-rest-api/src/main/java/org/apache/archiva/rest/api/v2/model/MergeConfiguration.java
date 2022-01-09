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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

import static org.apache.archiva.indexer.ArchivaIndexManager.DEFAULT_INDEX_PATH;

/**
 * Index merge configuration.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@XmlRootElement(name="mergeConfiguration")
@Schema(name="MergeConfiguration", description = "Configuration settings for index merge of remote repositories.")
public class MergeConfiguration implements Serializable, RestModel
{
    private static final long serialVersionUID = -3629274059574459133L;

    private String mergedIndexPath = DEFAULT_INDEX_PATH;
    private int mergedIndexTtlMinutes = 30;
    private String indexMergeSchedule = "";

    @Schema(name="merged_index_path", description = "The path where the merged index is stored. The path is relative to the repository directory of the group.")
    public String getMergedIndexPath( )
    {
        return mergedIndexPath;
    }

    public void setMergedIndexPath( String mergedIndexPath )
    {
        this.mergedIndexPath = mergedIndexPath;
    }

    @Schema(name="merged_index_ttl_minutes", description = "The Time to Life of the merged index in minutes.")
    public int getMergedIndexTtlMinutes( )
    {
        return mergedIndexTtlMinutes;
    }

    public void setMergedIndexTtlMinutes( int mergedIndexTtlMinutes )
    {
        this.mergedIndexTtlMinutes = mergedIndexTtlMinutes;
    }

    @Schema(name="index_merge_schedule", description = "Cron expression that defines the times/intervals for index merging.")
    public String getIndexMergeSchedule( )
    {
        return indexMergeSchedule;
    }

    public void setIndexMergeSchedule( String indexMergeSchedule )
    {
        this.indexMergeSchedule = indexMergeSchedule;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        MergeConfiguration that = (MergeConfiguration) o;

        if ( mergedIndexTtlMinutes != that.mergedIndexTtlMinutes ) return false;
        if ( !Objects.equals( mergedIndexPath, that.mergedIndexPath ) )
            return false;
        return Objects.equals( indexMergeSchedule, that.indexMergeSchedule );
    }

    @Override
    public int hashCode( )
    {
        int result = mergedIndexPath != null ? mergedIndexPath.hashCode( ) : 0;
        result = 31 * result + mergedIndexTtlMinutes;
        result = 31 * result + ( indexMergeSchedule != null ? indexMergeSchedule.hashCode( ) : 0 );
        return result;
    }

    @SuppressWarnings( "StringBufferReplaceableByString" )
    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "MergeConfiguration{" );
        sb.append( "mergedIndexPath='" ).append( mergedIndexPath ).append( '\'' );
        sb.append( ", mergedIndexTtlMinutes=" ).append( mergedIndexTtlMinutes );
        sb.append( ", indexMergeSchedule='" ).append( indexMergeSchedule ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
