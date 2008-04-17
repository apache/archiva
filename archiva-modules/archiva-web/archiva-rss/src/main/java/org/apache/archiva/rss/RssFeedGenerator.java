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
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.XmlReader;

/**
 * Generates RSS feeds.
 * 
 * @plexus.component role="org.apache.archiva.rss.RssFeedGenerator" 
 *      instantiation-strategy="per-lookup"
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

    /**
     * @plexus.configuration default-value="./apps/archiva/rss/"
     */
    private String rssDirectory;

    public void generateFeed( String title, String link, String description, List<RssFeedEntry> dataEntries,
                              String outputFilename )
    {           
        File outputFile = new File( rssDirectory, outputFilename );
        SyndFeed feed = null;
        List<SyndEntry> existingEntries = null;

        if ( outputFile.exists() )
        {
            try
            {
                SyndFeedInput input = new SyndFeedInput();
                feed = input.build( new XmlReader( outputFile ) );
                existingEntries = feed.getEntries();
            }
            catch ( IOException ie )
            {
                log.error( "Error occurred while reading existing feed : " + ie.getLocalizedMessage() );
            }
            catch ( FeedException fe )
            {
                log.error( "Error occurred while reading existing feed : " + fe.getLocalizedMessage() );
            }
        }
        else
        {
            feed = new SyndFeedImpl();

            feed.setTitle( title );
            feed.setLink( link );
            feed.setDescription( description );
            feed.setLanguage( DEFAULT_LANGUAGE );
        }
        
        feed.setPublishedDate( Calendar.getInstance().getTime() );
        feed.setFeedType( DEFAULT_FEEDTYPE );        
        feed.setEntries( getEntries( dataEntries, existingEntries ) );

        try
        {
            Writer writer = new FileWriter( outputFile );
            SyndFeedOutput output = new SyndFeedOutput();
            output.output( feed, writer );
            writer.close();

            log.debug( "Finished writing feed to " + outputFile.getAbsolutePath() );
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

    private List<SyndEntry> getEntries( List<RssFeedEntry> dataEntries, List<SyndEntry> existingEntries )
    {
        List<SyndEntry> entries = existingEntries;
        if ( entries == null )
        {
            entries = new ArrayList<SyndEntry>();
        }

        SyndEntry entry;
        SyndContent description;

        for ( RssFeedEntry dataEntry : dataEntries )
        {
            entry = new SyndEntryImpl();
            entry.setTitle( dataEntry.getTitle() );
            entry.setPublishedDate( Calendar.getInstance().getTime() );

            description = new SyndContentImpl();
            description.setType( "text/plain" );
            description.setValue( dataEntry.getDescription() );
            entry.setDescription( description );

            entries.add( entry );
        }

        return entries;
    }

    public void setRssDirectory( String rssDirectory )
    {
        this.rssDirectory = rssDirectory;
    }
    
}
