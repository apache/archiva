package org.apache.archiva.rss.processor;

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

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import org.apache.archiva.metadata.repository.MetadataRepository;

import java.util.Map;

/**
 * Retrieve and process the data that will be fed into the RssFeedGenerator.
 */
public interface RssFeedProcessor
{
    public static final String KEY_REPO_ID = "repoId";

    public static final String KEY_GROUP_ID = "groupId";

    public static final String KEY_ARTIFACT_ID = "artifactId";

    SyndFeed process( Map<String, String> reqParams, MetadataRepository metadataRepository )
        throws FeedException;
}
