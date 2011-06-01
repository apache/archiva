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

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @version
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath*:/META-INF/spring-context.xml"} )
public class RssFeedGeneratorTest
    extends TestCase
{
    @Inject
    private RssFeedGenerator generator;

    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNewFeed()
        throws Exception
    {
        List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
        RssFeedEntry entry = new RssFeedEntry( "Item 1" );
        
        Date whenGathered = new Date( System.currentTimeMillis() );

        entry.setDescription( "RSS 2.0 feed item 1." );
        entry.setPublishedDate( whenGathered );
        entries.add( entry );

        entry = new RssFeedEntry( "Item 2" );
        entry.setDescription( "RSS 2.0 feed item 2." );
        entry.setPublishedDate( whenGathered );
        entries.add( entry );

        entry = new RssFeedEntry( "Item 3" );
        entry.setDescription( "RSS 2.0 feed item 3." );
        entry.setPublishedDate( whenGathered );
        entries.add( entry );

        SyndFeed feed =
            generator.generateFeed( "Test Feed", "The test feed from Archiva.", entries );

        assertEquals( "Test Feed", feed.getTitle() );        
        assertEquals( "The test feed from Archiva.", feed.getDescription() );
        assertEquals( "en-us", feed.getLanguage() );
        assertEquals( entries.get( 2 ).getPublishedDate(), feed.getPublishedDate() );

        List<SyndEntry> syndEntries = feed.getEntries();
        assertEquals( 3, syndEntries.size() );
        assertEquals( "Item 1", syndEntries.get( 0 ).getTitle() );
        assertEquals( "Item 2", syndEntries.get( 1 ).getTitle() );
        assertEquals( "Item 3", syndEntries.get( 2 ).getTitle() );
    }

    @Test
    public void testNoDataEntries()
        throws Exception
    {
        List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
        SyndFeed feed =
            generator.generateFeed( "Test Feed", "The test feed from Archiva.", entries );

        assertNull( feed );
    }

    /*
     * this test might need to be removed since
     * no updates are happening in the feeds anymore since everything's processed from the db.
     * 
    public void testUpdateFeed()
        throws Exception
    {
        generator.setRssDirectory( getBasedir() + "/target/test-classes/rss-feeds/" );

        List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
        RssFeedEntry entry = new RssFeedEntry( "Item 1" );

        entry.setDescription( "RSS 2.0 feed item 1." );
        entries.add( entry );

        entry = new RssFeedEntry( "Item 2" );
        entry.setDescription( "RSS 2.0 feed item 2." );
        entries.add( entry );

        generator.generateFeed( "Test Feed", "The test feed from Archiva.", entries,
                                "generated-test-update-rss2.0-feed.xml" );

        File outputFile = new File( getBasedir(), "/target/test-classes/rss-feeds/generated-test-update-rss2.0-feed.xml" );
        String generatedContent = FileUtils.readFileToString( outputFile );

        XMLAssert.assertXpathEvaluatesTo( "Test Feed", "//channel/title", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "http://localhost:8080/archiva/rss/generated-test-update-rss2.0-feed.xml", "//channel/link", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "The test feed from Archiva.", "//channel/description", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "en-us", "//channel/language", generatedContent );

        String expectedItem1 =
            "<channel><item><title>Item 1</title></item><item><title>Item 2</title></item></channel>";
        
        XMLAssert.assertXpathsEqual( "//channel/item/title", expectedItem1, "//channel/item/title", generatedContent );

        //update existing rss feed
        entries = new ArrayList<RssFeedEntry>();
        entry = new RssFeedEntry( "Item 3" );

        entry.setDescription( "RSS 2.0 feed item 3." );
        entries.add( entry );

        entry = new RssFeedEntry( "Item 4" );
        entry.setDescription( "RSS 2.0 feed item 4." );
        entries.add( entry );

        generator.generateFeed( "Test Feed", "The test feed from Archiva.", entries,
                                "generated-test-update-rss2.0-feed.xml" );
        
        outputFile = new File( getBasedir(), "/target/test-classes/rss-feeds/generated-test-update-rss2.0-feed.xml" );        
        generatedContent = FileUtils.readFileToString( outputFile );       
        
        XMLAssert.assertXpathEvaluatesTo( "Test Feed", "//channel/title", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "http://localhost:8080/archiva/rss/generated-test-update-rss2.0-feed.xml", "//channel/link", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "The test feed from Archiva.", "//channel/description", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "en-us", "//channel/language", generatedContent );

        expectedItem1 =
            "<channel><item><title>Item 1</title></item><item><title>Item 2</title></item>"
            + "<item><title>Item 3</title></item><item><title>Item 4</title></item></channel>";
        XMLAssert.assertXpathsEqual( "//channel/item/title", expectedItem1, "//channel/item/title", generatedContent );
        
        outputFile.deleteOnExit();
    }
     */

}
