package org.apache.archiva.configuration;

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

/**
 * Class ManagedRepositoryConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ManagedRepositoryConfiguration
    extends AbstractRepositoryConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The file system location for this repository.
     *           
     */
    private String location;

    /**
     * True if this repository contains release versioned artifacts.
     */
    private boolean releases = true;

    /**
     * True if re-deployment of artifacts already in the repository
     * will be blocked.
     */
    private boolean blockRedeployments = false;

    /**
     * True if this repository contains snapshot versioned artifacts
     */
    private boolean snapshots = false;

    /**
     * True if this repository should be scanned and processed.
     */
    private boolean scanned = true;

    /**
     * 
     *             When to run the refresh task.
     *             Default is every hour
     *           .
     */
    private String refreshCronExpression = "0 0 * * * ?";

    /**
     * 
     *             The total count of the artifact to be retained
     * for each snapshot.
     *           
     */
    private int retentionCount = 2;

    /**
     * 
     *             The number of days after which snapshots will be
     * removed.
     *           
     */
    private int retentionPeriod = 100;

    /**
     * 
     *             True if the released snapshots are to be removed
     * from the repo during repository purge.
     *           
     */
    private boolean deleteReleasedSnapshots = false;

    /**
     * 
     *             True to not generate packed index (note you
     * won't be able to export your index.
     *           
     */
    private boolean skipPackedIndexCreation = false;

    /**
     * 
     *             Need a staging repository
     *           .
     */
    private boolean stageRepoNeeded = false;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the file system location for this repository.
     * 
     * @return String
     */
    public String getLocation()
    {
        return this.location;
    } //-- String getLocation()

    /**
     * Get when to run the refresh task.
     *             Default is every hour.
     * 
     * @return String
     */
    public String getRefreshCronExpression()
    {
        return this.refreshCronExpression;
    } //-- String getRefreshCronExpression()

    /**
     * Get the total count of the artifact to be retained for each
     * snapshot.
     * 
     * @return int
     */
    public int getRetentionCount()
    {
        return this.retentionCount;
    } //-- int getRetentionCount()

    /**
     * Get the number of days after which snapshots will be
     * removed.
     * 
     * @return int
     */
    public int getRetentionPeriod()
    {
        return this.retentionPeriod;
    } //-- int getRetentionPeriod()

    /**
     * Get true if re-deployment of artifacts already in the
     * repository will be blocked.
     * 
     * @return boolean
     */
    public boolean isBlockRedeployments()
    {
        return this.blockRedeployments;
    } //-- boolean isBlockRedeployments()

    /**
     * Get true if the released snapshots are to be removed from
     * the repo during repository purge.
     * 
     * @return boolean
     */
    public boolean isDeleteReleasedSnapshots()
    {
        return this.deleteReleasedSnapshots;
    } //-- boolean isDeleteReleasedSnapshots()

    /**
     * Get true if this repository contains release versioned
     * artifacts.
     * 
     * @return boolean
     */
    public boolean isReleases()
    {
        return this.releases;
    } //-- boolean isReleases()

    /**
     * Get true if this repository should be scanned and processed.
     * 
     * @return boolean
     */
    public boolean isScanned()
    {
        return this.scanned;
    } //-- boolean isScanned()

    /**
     * Get true to not generate packed index (note you won't be
     * able to export your index.
     * 
     * @return boolean
     */
    public boolean isSkipPackedIndexCreation()
    {
        return this.skipPackedIndexCreation;
    } //-- boolean isSkipPackedIndexCreation()

    /**
     * Get true if this repository contains snapshot versioned
     * artifacts.
     * 
     * @return boolean
     */
    public boolean isSnapshots()
    {
        return this.snapshots;
    } //-- boolean isSnapshots()

    /**
     * Get need a staging repository.
     * 
     * @return boolean
     */
    public boolean isStageRepoNeeded()
    {
        return this.stageRepoNeeded;
    } //-- boolean isStageRepoNeeded()

    /**
     * Set true if re-deployment of artifacts already in the
     * repository will be blocked.
     * 
     * @param blockRedeployments
     */
    public void setBlockRedeployments( boolean blockRedeployments )
    {
        this.blockRedeployments = blockRedeployments;
    } //-- void setBlockRedeployments( boolean )

    /**
     * Set true if the released snapshots are to be removed from
     * the repo during repository purge.
     * 
     * @param deleteReleasedSnapshots
     */
    public void setDeleteReleasedSnapshots( boolean deleteReleasedSnapshots )
    {
        this.deleteReleasedSnapshots = deleteReleasedSnapshots;
    } //-- void setDeleteReleasedSnapshots( boolean )

    /**
     * Set the file system location for this repository.
     * 
     * @param location
     */
    public void setLocation( String location )
    {
        this.location = location;
    } //-- void setLocation( String )

    /**
     * Set when to run the refresh task.
     *             Default is every hour.
     * 
     * @param refreshCronExpression
     */
    public void setRefreshCronExpression( String refreshCronExpression )
    {
        this.refreshCronExpression = refreshCronExpression;
    } //-- void setRefreshCronExpression( String )

    /**
     * Set true if this repository contains release versioned
     * artifacts.
     * 
     * @param releases
     */
    public void setReleases( boolean releases )
    {
        this.releases = releases;
    } //-- void setReleases( boolean )

    /**
     * Set the total count of the artifact to be retained for each
     * snapshot.
     * 
     * @param retentionCount
     */
    public void setRetentionCount( int retentionCount )
    {
        this.retentionCount = retentionCount;
    } //-- void setRetentionCount( int )

    /**
     * Set the number of days after which snapshots will be
     * removed.
     * 
     * @param retentionPeriod
     */
    public void setRetentionPeriod( int retentionPeriod )
    {
        this.retentionPeriod = retentionPeriod;
    } //-- void setRetentionPeriod( int )

    /**
     * Set true if this repository should be scanned and processed.
     * 
     * @param scanned
     */
    public void setScanned( boolean scanned )
    {
        this.scanned = scanned;
    } //-- void setScanned( boolean )

    /**
     * Set true to not generate packed index (note you won't be
     * able to export your index.
     * 
     * @param skipPackedIndexCreation
     */
    public void setSkipPackedIndexCreation( boolean skipPackedIndexCreation )
    {
        this.skipPackedIndexCreation = skipPackedIndexCreation;
    } //-- void setSkipPackedIndexCreation( boolean )

    /**
     * Set true if this repository contains snapshot versioned
     * artifacts.
     * 
     * @param snapshots
     */
    public void setSnapshots( boolean snapshots )
    {
        this.snapshots = snapshots;
    } //-- void setSnapshots( boolean )

    /**
     * Set need a staging repository.
     * 
     * @param stageRepoNeeded
     */
    public void setStageRepoNeeded( boolean stageRepoNeeded )
    {
        this.stageRepoNeeded = stageRepoNeeded;
    } //-- void setStageRepoNeeded( boolean )

}
