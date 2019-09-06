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

import org.apache.archiva.repository.storage.StorageAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * PropagateErrorsPolicy - a policy applied on error to determine how to treat the error.
 */
@Service( "downloadErrorPolicy#propagate-errors" )
public class PropagateErrorsDownloadPolicy
    extends AbstractPolicy implements DownloadErrorPolicy
{
    private Logger log = LoggerFactory.getLogger( PropagateErrorsDownloadPolicy.class );
    private static final String ID = "propagate-errors";


    /**
     * Signifies any error should stop searching for other proxies.
     */
    public static final DownloadErrorOption STOP = DownloadErrorOption.STOP;

    /**
     * Propagate errors at the end after all are gathered, if there was no successful download from other proxies.
     */
    public static final DownloadErrorOption QUEUE = DownloadErrorOption.QUEUE;

    /**
     * Ignore errors and treat as if it were not found.
     */
    public static final DownloadErrorOption IGNORE = DownloadErrorOption.IGNORE;

    private List<PolicyOption> options = new ArrayList<>( 3 );

    public PropagateErrorsDownloadPolicy()
    {
        options.add( STOP );
        options.add( QUEUE );
        options.add( IGNORE );
    }

    @Override
    public boolean applyPolicy( PolicyOption policySetting, Properties request, StorageAsset localFile, Exception exception,
                                Map<String, Exception> previousExceptions )
        throws PolicyConfigurationException
    {
        if ( !options.contains( policySetting ) )
        {
            // Not a valid code.
            throw new PolicyConfigurationException( "Unknown error policy setting [" + policySetting +
                                                        "], valid settings are [" + StringUtils.join(
                options.iterator(), "," ) + "]" );
        }

        if ( IGNORE.equals( policySetting ) )
        {
            // Ignore.
            log.debug( "Error policy set to IGNORE." );
            return false;
        }

        String repositoryId = request.getProperty( "remoteRepositoryId" );
        if ( STOP.equals( policySetting ) )
        {
            return true;
        }

        if ( QUEUE.equals( policySetting ) )
        {
            previousExceptions.put( repositoryId, exception );
            return true;
        }

        throw new PolicyConfigurationException(
            "Unable to process checksum policy of [" + policySetting + "], please file a bug report." );
    }

    @Override
    public PolicyOption getDefaultOption()
    {
        return QUEUE;
    }

    @Override
    public String getId()
    {
        return ID ;
    }

    @Override
    public List<PolicyOption> getOptions()
    {
        return options;
    }
}