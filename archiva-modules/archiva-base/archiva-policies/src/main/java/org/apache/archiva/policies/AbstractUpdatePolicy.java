package org.apache.archiva.policies;

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

import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

/**
 * AbstractUpdatePolicy
 *
 *
 */
public abstract class AbstractUpdatePolicy
    extends AbstractPolicy implements PreDownloadPolicy
{
    private Logger log = LoggerFactory.getLogger( AbstractUpdatePolicy.class );

    /**
     * The ALWAYS policy setting means that the artifact is always updated from the remote repo.
     */
    public static final PolicyOption ALWAYS = UpdateOption.ALWAYS;

    /**
     * The NEVER policy setting means that the artifact is never updated from the remote repo.
     */
    public static final PolicyOption NEVER = UpdateOption.NEVER;

    /**
     * <p>
     * The DAILY policy means that the artifact retrieval occurs only if one of
     * the following conditions are met...
     * </p>
     * <ul>
     * <li>The local artifact is not present.</li>
     * <li>The local artifact has a last modified timestamp older than (now - 1 day).</li>
     * </ul>
     */
    public static final UpdateOption DAILY = UpdateOption.DAILY;

    /**
     * <p>
     * The HOURLY policy means that the artifact retrieval occurs only if one of
     * the following conditions are met...
     * </p>
     * <ul>
     * <li>The local artifact is not present.</li>
     * <li>The local artifact has a last modified timestamp older than (now - 1 hour).</li>
     * </ul>
     */
    public static final UpdateOption HOURLY = UpdateOption.HOURLY;

    /**
     * The ONCE policy means that the artifact retrieval occurs only if the
     * local artifact is not present.  This means that the retrieval can only
     * occur once.
     */
    public static final UpdateOption ONCE = UpdateOption.ONCE;

    private List<PolicyOption> options = new ArrayList<>( 5 );

    public AbstractUpdatePolicy()
    {
        super();
        super.setOptionPrefix("update.option.");
        options.add( ALWAYS );
        options.add( HOURLY );
        options.add( DAILY );
        options.add( ONCE );
        options.add( NEVER );
    }

    protected abstract boolean isSnapshotPolicy();

    protected abstract String getUpdateMode();

    @Override
    public List<PolicyOption> getOptions()
    {
        return options;
    }

    @Override
    public void applyPolicy( PolicyOption policySetting, Properties request, StorageAsset localFile )
        throws PolicyViolationException, PolicyConfigurationException
    {
        if ( !StringUtils.equals( request.getProperty( "filetype" ), "artifact" ) )
        {
            // Only process artifact file types.
            return;
        }

        String version = request.getProperty( "version", "" );
        boolean isSnapshotVersion = false;

        if ( StringUtils.isNotBlank( version ) )
        {
            isSnapshotVersion = VersionUtil.isSnapshot( version );
        }

        if ( !options.contains( policySetting ) )
        {
            // Not a valid code. 
            throw new PolicyConfigurationException(
                "Unknown " + getUpdateMode() + " policy setting [" + policySetting + "], valid settings are ["
                    + StringUtils.join( options.iterator(), "," ) + "]" );
        }

        if ( ALWAYS.equals( policySetting ) )
        {
            // Skip means ok to update.
            log.debug( "OK to update, {} policy set to ALWAYS.", getUpdateMode() );
            return;
        }

        // Test for mismatches.
        if ( !isSnapshotVersion && isSnapshotPolicy() )
        {
            log.debug( "OK to update, snapshot policy does not apply for non-snapshot versions." );
            return;
        }

        if ( isSnapshotVersion && !isSnapshotPolicy() )
        {
            log.debug( "OK to update, release policy does not apply for snapshot versions." );
            return;
        }

        if ( NEVER.equals( policySetting ) )
        {
            // Reject means no.
            throw new PolicyViolationException( "NO to update, " + getUpdateMode() + " policy set to NEVER." );
        }

        if ( !localFile.exists() )
        {
            // No file means it's ok.
            log.debug( "OK to update {}, local file does not exist.", getUpdateMode() );
            return;
        }

        if ( ONCE.equals( policySetting ) )
        {
            // File exists, but policy is once.
            throw new PolicyViolationException(
                "NO to update " + getUpdateMode() + ", policy is ONCE, and local file exist." );
        }

        if ( DAILY.equals( policySetting ) )
        {
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.DAY_OF_MONTH, -1 );
            Calendar fileCal = Calendar.getInstance();
            fileCal.setTimeInMillis( localFile.getModificationTime().toEpochMilli() );

            if ( cal.after( fileCal ) )
            {
                // Its ok.
                return;
            }
            else
            {
                throw new PolicyViolationException( "NO to update " + getUpdateMode()
                                                        + ", policy is DAILY, local file exist, and has been updated within the last day." );
            }
        }

        if ( HOURLY.equals( policySetting ) )
        {
            Calendar cal = Calendar.getInstance();
            cal.add( Calendar.HOUR, -1 );
            Calendar fileCal = Calendar.getInstance();
            fileCal.setTimeInMillis( localFile.getModificationTime().toEpochMilli());

            if ( cal.after( fileCal ) )
            {
                // Its ok.
                return;
            }
            else
            {
                throw new PolicyViolationException( "NO to update " + getUpdateMode()
                                                        + ", policy is HOURLY, local file exist, and has been updated within the last hour." );
            }
        }

        throw new PolicyConfigurationException(
            "Unable to process " + getUpdateMode() + " policy of [" + policySetting + "], please file a bug report." );
    }
}
