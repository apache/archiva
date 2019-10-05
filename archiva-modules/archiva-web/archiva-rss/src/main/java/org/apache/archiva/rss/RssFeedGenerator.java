package org.apache.archiva.rss;

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

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates RSS feeds.
 *
 */
@Service("rssFeedGenerator#default")
@Scope("prototype")
public class RssFeedGenerator
{
    private Logger log = LoggerFactory.getLogger( RssFeedGenerator.class );

    // TODO: make configurable
    public static String DEFAULT_FEEDTYPE = "rss_2.0";

    public static String DEFAULT_LANGUAGE = "en-us";

    public SyndFeed generateFeed( String title, String description, List<RssFeedEntry> dataEntries )
    {
        if( dataEntries.size() ==  0 )
        {
            log.debug( "No updates found, feed not generated." );
            return null;
        }
        
        SyndFeed feed = new SyndFeedImpl();
        feed.setTitle( title );        
        feed.setDescription( description );
        feed.setLanguage( DEFAULT_LANGUAGE );
        feed.setPublishedDate( dataEntries.get( dataEntries.size() - 1 ).getPublishedDate() );
        feed.setFeedType( DEFAULT_FEEDTYPE );
        feed.setEntries( getEntries( dataEntries ) );

        log.debug( "Finished generating the feed \'{}\'.", title );
        
        return feed;
    }

    private List<SyndEntry> getEntries( List<RssFeedEntry> dataEntries )
    {
        List<SyndEntry> entries = new ArrayList<>();

        SyndEntry entry;
        SyndContent description;

        for ( RssFeedEntry dataEntry : dataEntries )
        {
            entry = new SyndEntryImpl();
            entry.setTitle( dataEntry.getTitle() );
            entry.setPublishedDate( dataEntry.getPublishedDate() );

            description = new SyndContentImpl();
            description.setType( "text/plain" );
            description.setValue( dataEntry.getDescription() );
            entry.setDescription( description );

            entries.add( entry );
        }

        return entries;
    }
}
