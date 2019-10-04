package org.apache.archiva.indexer.merger.base;

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

import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.merger.IndexMerger;
import org.apache.archiva.indexer.merger.IndexMergerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class MergedRemoteIndexesTask
    implements Runnable
{

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private MergedRemoteIndexesTaskRequest mergedRemoteIndexesTaskRequest;

    public MergedRemoteIndexesTask( MergedRemoteIndexesTaskRequest mergedRemoteIndexesTaskRequest )
    {
        this.mergedRemoteIndexesTaskRequest = mergedRemoteIndexesTaskRequest;
    }

    @Override
    public void run()
    {
        try
        {
            this.execute();
        }
        catch ( IndexMergerException e )
        {
            logger.error( e.getMessage(), e );
        }
    }

    public MergedRemoteIndexesTaskResult execute()
        throws IndexMergerException
    {
        IndexMerger indexMerger = mergedRemoteIndexesTaskRequest.getIndexMerger();

        ArchivaIndexingContext indexingContext =
            indexMerger.buildMergedIndex( mergedRemoteIndexesTaskRequest.getIndexMergerRequest() );

        return new MergedRemoteIndexesTaskResult( indexingContext );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof MergedRemoteIndexesTask ) )
        {
            return false;
        }

        MergedRemoteIndexesTask that = (MergedRemoteIndexesTask) o;

        return mergedRemoteIndexesTaskRequest.equals( that.mergedRemoteIndexesTaskRequest );
    }

    @Override
    public int hashCode()
    {
        return mergedRemoteIndexesTaskRequest.hashCode();
    }
}
