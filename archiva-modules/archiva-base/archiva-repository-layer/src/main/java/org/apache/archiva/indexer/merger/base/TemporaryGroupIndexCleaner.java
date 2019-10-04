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

import org.apache.archiva.indexer.merger.IndexMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@Service
public class TemporaryGroupIndexCleaner
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndexMerger indexMerger;


    public TemporaryGroupIndexCleaner( )
    {

    }

    // 900000
    @Scheduled(fixedDelay = 900000)
    public void cleanTemporaryIndex()
    {

        indexMerger.getTemporaryGroupIndexes()
            .stream()
            .forEach( temporaryGroupIndex ->
                 {
                     // cleanup files older than the ttl
                     if ( new Date().getTime() - temporaryGroupIndex.getCreationTime() >
                         temporaryGroupIndex.getMergedIndexTtl() )
                     {
                         log.info( "cleanTemporaryIndex for groupId {}", temporaryGroupIndex.getGroupId() );
                         indexMerger.cleanTemporaryGroupIndex( temporaryGroupIndex );

                     }
                 }
        );
    }
}
