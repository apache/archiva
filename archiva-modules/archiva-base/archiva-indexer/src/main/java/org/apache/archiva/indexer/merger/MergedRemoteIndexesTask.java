package org.apache.archiva.indexer.merger;

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

import org.apache.maven.index.context.IndexingContext;
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
        IndexMerger indexMerger = mergedRemoteIndexesTaskRequest.indexMerger;

        IndexingContext indexingContext =
            indexMerger.buildMergedIndex( mergedRemoteIndexesTaskRequest.indexMergerRequest );

        return new MergedRemoteIndexesTaskResult( indexingContext );
    }

    public static class MergedRemoteIndexesTaskRequest
    {
        private IndexMergerRequest indexMergerRequest;

        private IndexMerger indexMerger;

        public MergedRemoteIndexesTaskRequest( IndexMergerRequest indexMergerRequest, IndexMerger indexMerger )
        {
            this.indexMergerRequest = indexMergerRequest;
            this.indexMerger = indexMerger;
        }

        public IndexMergerRequest getIndexMergerRequest()
        {
            return indexMergerRequest;
        }

        public void setIndexMergerRequest( IndexMergerRequest indexMergerRequest )
        {
            this.indexMergerRequest = indexMergerRequest;
        }

        public IndexMerger getIndexMerger()
        {
            return indexMerger;
        }

        public void setIndexMerger( IndexMerger indexMerger )
        {
            this.indexMerger = indexMerger;
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            MergedRemoteIndexesTaskRequest that = (MergedRemoteIndexesTaskRequest) o;

            if ( !indexMergerRequest.equals( that.indexMergerRequest ) )
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            return indexMergerRequest.hashCode();
        }
    }

    public static class MergedRemoteIndexesTaskResult
    {
        private IndexingContext indexingContext;

        public MergedRemoteIndexesTaskResult( IndexingContext indexingContext )
        {
            this.indexingContext = indexingContext;
        }

        public IndexingContext getIndexingContext()
        {
            return indexingContext;
        }

        public void setIndexingContext( IndexingContext indexingContext )
        {
            this.indexingContext = indexingContext;
        }
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

        if ( !mergedRemoteIndexesTaskRequest.equals( that.mergedRemoteIndexesTaskRequest ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return mergedRemoteIndexesTaskRequest.hashCode();
    }
}
