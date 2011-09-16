package org.apache.archiva.repository.scanner.functors;

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

import org.apache.commons.collections.Closure;
import org.apache.archiva.common.utils.BaseFile;
import org.apache.maven.archiva.consumers.RepositoryContentConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * ConsumerProcessFileClosure 
 *
 * @version $Id$
 */
public class ConsumerProcessFileClosure
    implements Closure
{
    private Logger log = LoggerFactory.getLogger( ConsumerProcessFileClosure.class );
    
    private BaseFile basefile;

    private boolean executeOnEntireRepo;

    private Map<String,Long> consumerTimings;
    
    private Map<String,Long> consumerCounts;

    public void execute( Object input )
    {
        if ( input instanceof RepositoryContentConsumer )
        {
            RepositoryContentConsumer consumer = (RepositoryContentConsumer) input;

            String id = consumer.getId();
            try
            {
                log.debug( "Sending to consumer: {}", id );

                long startTime = System.currentTimeMillis();
                consumer.processFile( basefile.getRelativePath(), executeOnEntireRepo );
                long endTime = System.currentTimeMillis();

                if ( consumerTimings != null )
                {
                    Long value = consumerTimings.get( id );
                    consumerTimings.put( id, ( value != null ? value : 0 ) + endTime - startTime );
                }

                if ( consumerCounts != null )
                {
                    Long value = consumerCounts.get( id );
                    consumerCounts.put( id, ( value != null ? value : 0 ) + 1 );
                }
            }
            catch ( Exception e )
            {
                /* Intentionally Catch all exceptions.
                 * So that the discoverer processing can continue.
                 */
                log.error( "Consumer [" + id + "] had an error when processing file ["
                    + basefile.getAbsolutePath() + "]: " + e.getMessage(), e );
            }
        }
    }

    public BaseFile getBasefile()
    {
        return basefile;
    }

    public void setBasefile( BaseFile basefile )
    {
        this.basefile = basefile;
    }

    public boolean isExecuteOnEntireRepo()
    {
        return executeOnEntireRepo;
    }

    public void setExecuteOnEntireRepo( boolean executeOnEntireRepo )
    {
        this.executeOnEntireRepo = executeOnEntireRepo;
    }

    public void setConsumerTimings( Map<String, Long> consumerTimings )
    {
        this.consumerTimings = consumerTimings;
    }

    public void setConsumerCounts( Map<String, Long> consumerCounts )
    {
        this.consumerCounts = consumerCounts;
    }

    public Logger getLogger()
    {
        return log;
    }

    public void setLogger( Logger logger )
    {
        this.log = logger;
    }
}
