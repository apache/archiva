package org.apache.archiva.admin.model.managed;

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

import org.apache.archiva.admin.model.AbstractRepository;

import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
public class ManagedRepository
    extends AbstractRepository
    implements Serializable
{

    private String location;

    private boolean snapshots = false;

    private boolean releases = true;

    private boolean blockRedeployments = false;

    /**
     * default model value
     */
    private String cronExpression = "0 0 * * * ?";


    /**
     * not need when creating the repo : only available when reading
     */
    private ManagedRepository stagingRepository;

    private boolean scanned = false;

    private String indexDirectory;

    /**
     * default model value
     */
    private int daysOlder = 100;

    /**
     * default model value
     */
    private int retentionCount = 2;

    private boolean deleteReleasedSnapshots;

    public ManagedRepository()
    {
        // no op
    }

    public ManagedRepository( String id, String name, String location, String layout, boolean snapshots,
                              boolean releases, boolean blockRedeployments, String cronExpression, String indexDir,
                              boolean scanned, int daysOlder, int retentionCount, boolean deleteReleasedSnapshots )
    {
        super(id, name, layout);

        this.location = location;
        this.snapshots = snapshots;
        this.releases = releases;
        this.blockRedeployments = blockRedeployments;
        this.cronExpression = cronExpression;
        this.indexDirectory = indexDir;
        this.scanned = scanned;
        this.daysOlder = daysOlder;
        this.retentionCount = retentionCount;
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
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

    public ManagedRepository getStagingRepository()
    {
        return stagingRepository;
    }


    public void setStagingRepository( ManagedRepository stagingRepository )
    {
        this.stagingRepository = stagingRepository;
    }

    public boolean isScanned()
    {
        return scanned;
    }

    public void setScanned( boolean scanned )
    {
        this.scanned = scanned;
    }

    public String getIndexDirectory()
    {
        return indexDirectory;
    }

    public void setIndexDirectory( String indexDirectory )
    {
        this.indexDirectory = indexDirectory;
    }

    public int getDaysOlder()
    {
        return daysOlder;
    }

    public void setDaysOlder( int daysOlder )
    {
        this.daysOlder = daysOlder;
    }

    public int getRetentionCount()
    {
        return retentionCount;
    }

    public void setRetentionCount( int retentionCount )
    {
        this.retentionCount = retentionCount;
    }

    public boolean isDeleteReleasedSnapshots()
    {
        return deleteReleasedSnapshots;
    }

    public void setDeleteReleasedSnapshots( boolean deleteReleasedSnapshots )
    {
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
    }


    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "ManagedRepository" );
        sb.append( "{location='" ).append( location ).append( '\'' );
        sb.append( ", snapshots=" ).append( snapshots );
        sb.append( ", releases=" ).append( releases );
        sb.append( ", blockRedeployments=" ).append( blockRedeployments );
        sb.append( ", cronExpression='" ).append( cronExpression ).append( '\'' );
        sb.append( ", stagingRepository=" ).append( stagingRepository );
        sb.append( ", scanned=" ).append( scanned );
        sb.append( ", indexDirectory='" ).append( indexDirectory ).append( '\'' );
        sb.append( ", daysOlder=" ).append( daysOlder );
        sb.append( ", retentionCount=" ).append( retentionCount );
        sb.append( ", deleteReleasedSnapshots=" ).append( deleteReleasedSnapshots );
        sb.append( '}' );
        sb.append( super.toString() );
        return sb.toString();
    }


}