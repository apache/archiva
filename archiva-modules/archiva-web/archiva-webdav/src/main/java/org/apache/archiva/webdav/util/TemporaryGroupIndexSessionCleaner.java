package org.apache.archiva.webdav.util;
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
import org.apache.archiva.indexer.merger.TemporaryGroupIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * this http session listener will delete repository group index requested by a user
 * at this end of the http session
 *
 * @author Olivier Lamy
 * @since 1.4-M2
 */
public class TemporaryGroupIndexSessionCleaner
    implements HttpSessionListener
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private IndexMerger indexMerger;

    public static final String TEMPORARY_INDEX_SESSION_KEY = TemporaryGroupIndexSessionCleaner.class.getName();

    @Override
    public void sessionCreated( HttpSessionEvent httpSessionEvent )
    {
        // ensure the map is here to avoid NPE
        if ( httpSessionEvent.getSession().getAttribute( TEMPORARY_INDEX_SESSION_KEY ) == null )
        {
            httpSessionEvent.getSession().setAttribute( TEMPORARY_INDEX_SESSION_KEY,
                                                        new HashMap<>() );
        }

        if ( indexMerger == null )
        {
            WebApplicationContext webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(
                httpSessionEvent.getSession().getServletContext() );
            indexMerger = webApplicationContext.getBean( IndexMerger.class );
        }
    }

    @Override
    public void sessionDestroyed( HttpSessionEvent httpSessionEvent )
    {
        @SuppressWarnings( "unchecked" ) Map<String, TemporaryGroupIndex> tempFilesPerKey =
            (Map<String, TemporaryGroupIndex>) httpSessionEvent.getSession().getAttribute(
                TEMPORARY_INDEX_SESSION_KEY );

        for ( TemporaryGroupIndex temporaryGroupIndex : tempFilesPerKey.values() )
        {
            log.info( "cleanup temporaryGroupIndex {} directory {}", temporaryGroupIndex.getIndexId(),
                      temporaryGroupIndex.getDirectory().getPath() );
            getIndexMerger( httpSessionEvent ).cleanTemporaryGroupIndex( temporaryGroupIndex );
        }
    }

    private IndexMerger getIndexMerger( HttpSessionEvent httpSessionEvent )
    {
        if ( indexMerger == null )
        {
            WebApplicationContext webApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(
                httpSessionEvent.getSession().getServletContext() );
            indexMerger = webApplicationContext.getBean( IndexMerger.class );
        }
        return indexMerger;
    }
}

