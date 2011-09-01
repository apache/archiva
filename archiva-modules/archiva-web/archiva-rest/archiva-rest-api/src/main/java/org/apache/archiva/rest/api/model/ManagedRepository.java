package org.apache.archiva.rest.api.model;

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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @since 1.4
 */
@XmlRootElement( name = "managedRepository" )
public class ManagedRepository
    implements Serializable
{
    private String id;

    private String name;

    private String location;

    private String layout;

    private boolean snapshots = false;

    private boolean releases = false;

    private boolean blockRedeployments;

    private boolean stageRepoNeeded;

    private String cronExpression;

    private boolean resetStats;

    public ManagedRepository()
    {
        // no op
    }

    public ManagedRepository( String id, String name, String location, String layout, boolean snapshots, boolean releases,
                              boolean blockRedeployments, boolean stageRepoNeeded, String cronExpression )
    {
        this.id = id;
        this.name = name;
        this.location = location;
        this.layout = layout;
        this.snapshots = snapshots;
        this.releases = releases;
        this.blockRedeployments = blockRedeployments;
        this.stageRepoNeeded = stageRepoNeeded;
        this.cronExpression = cronExpression;
    }

    public String getId()
    {
        return this.id;
    }

    public String getLayout()
    {
        return this.layout;
    }

    public String getName()
    {
        return this.name;
    }

    public String getLocation()
    {
        return this.location;
    }


    public boolean isReleases()
    {
        return this.releases;
    }

    /**
     * Get null
     */
    public boolean isSnapshots()
    {
        return this.snapshots;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public void setLayout( String layout )
    {
        this.layout = layout;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setReleases( boolean releases )
    {
        this.releases = releases;
    }

    public void setSnapshots( boolean snapshots )
    {
        this.snapshots = snapshots;
    }

    public void setLocation( String location )
    {
        this.location = location;
    }

    public boolean isBlockRedeployments()
    {
        return blockRedeployments;
    }

    public void setBlockRedeployments( boolean blockRedeployments )
    {
        this.blockRedeployments = blockRedeployments;
    }

    public String getCronExpression()
    {
        return cronExpression;
    }

    public void setCronExpression( String cronExpression )
    {
        this.cronExpression = cronExpression;
    }

    public boolean isStageRepoNeeded()
    {
        return stageRepoNeeded;
    }

    public void setStageRepoNeeded( boolean stageRepoNeeded )
    {
        this.stageRepoNeeded = stageRepoNeeded;
    }

    public boolean isResetStats()
    {
        return resetStats;
    }

    public void setResetStats( boolean resetStats )
    {
        this.resetStats = resetStats;
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + ( id != null ? id.hashCode() : 0 );
        return result;
    }

    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof ManagedRepository ) )
        {
            return false;
        }

        ManagedRepository that = (ManagedRepository) other;
        boolean result = true;
        result = result && ( getId() == null ? that.getId() == null : getId().equals( that.getId() ) );
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ManagedRepository" );
        sb.append( "{id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", location='" ).append( location ).append( '\'' );
        sb.append( ", layout='" ).append( layout ).append( '\'' );
        sb.append( ", snapshots=" ).append( snapshots );
        sb.append( ", releases=" ).append( releases );
        sb.append( ", blockRedeployments=" ).append( blockRedeployments );
        sb.append( ", stageRepoNeeded=" ).append( stageRepoNeeded );
        sb.append( ", cronExpression='" ).append( cronExpression ).append( '\'' );
        sb.append( ", resetStats=" ).append( resetStats );
        sb.append( '}' );
        return sb.toString();
    }
}