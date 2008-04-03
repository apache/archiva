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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Generates RSS feeds.
 * 
 * @plexus.component role="org.apache.archiva.rss.RssFeedGenerator"
 * 
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class RssFeedGenerator
{
    private Logger log = LoggerFactory.getLogger( RssFeedGenerator.class );

    // TODO: make configurable
    public static String DEFAULT_FEEDTYPE = "rss_2.0";

    public static String DEFAULT_LANGUAGE = "en-us";

    public void generateFeed( String title, String link, String description, List<RssFeedEntry> dataEntries,
                              File outputFile )
    {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType( DEFAULT_FEEDTYPE );

        feed.setTitle( title );
        feed.setLink( link );
        feed.setDescription( description );
        feed.setLanguage( DEFAULT_LANGUAGE );
        feed.setPublishedDate( Calendar.getInstance().getTime() );

        feed.setEntries( getEntries( dataEntries ) );

        try
        {
            Writer writer = new FileWriter( outputFile );
            SyndFeedOutput output = new SyndFeedOutput();
            output.output( feed, writer );
            writer.close();
        }
        catch ( IOException ie )
        {
            log.error( "Error occurred while generating the feed : " + ie.getMessage() );
        }
        catch ( FeedException fe )
        {
            log.error( "Error occurred while generating the feed : " + fe.getMessage() );
        }
    }

    private List<SyndEntry> getEntries( List<RssFeedEntry> dataEntries )
    {
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        SyndEntry entry;
        SyndContent description;

        for ( RssFeedEntry dataEntry : dataEntries )
        {
            entry = new SyndEntryImpl();
            entry.setTitle( dataEntry.getTitle() );
            entry.setLink( dataEntry.getLink() );
            entry.setPublishedDate( Calendar.getInstance().getTime() );

            description = new SyndContentImpl();
            description.setType( "text/plain" );
            description.setValue( dataEntry.getDescription() );
            entry.setDescription( description );

            entries.add( entry );
        }

        return entries;
    }

}
