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

import java.util.Collection;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
public interface IndexMerger
{

    /**
     * system property which contains the value in s for ttl of temporary index groups
     */
    static final String TMP_GROUP_INDEX_SYS_KEY = "archiva.tmp.group.index.ttl";

    /**
     * @param repositoriesIds repositories Ids to merge content
     * @param packIndex       will generate a downloadable index
     * @return a temporary directory with a merge index (directory marked deleteOnExit)
     * @throws IndexMergerException
     */
    IndexingContext buildMergedIndex( Collection<String> repositoriesIds, boolean packIndex )
        throws IndexMergerException;

    void cleanTemporaryGroupIndex( TemporaryGroupIndex temporaryGroupIndex );

    Collection<TemporaryGroupIndex> getTemporaryGroupIndexes();

    /**
     * @return ttl of temporay group index
     */
    int getGroupMergedIndexTtl();

}
