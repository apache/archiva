package org.apache.maven.archiva.common.rss;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.PlexusTestCase;
import org.custommonkey.xmlunit.XMLAssert;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class RssFeedGeneratorTest
    extends PlexusTestCase
{
    private RssFeedGenerator generator;

    public void setUp()
        throws Exception
    {
        super.setUp();

        generator = (RssFeedGenerator) lookup( RssFeedGenerator.class );

        File outputDir = new File( getBasedir(), "/target/test-classes/rss-feeds" );
        outputDir.mkdir();
    }

    public void testGenerateFeed()
        throws Exception
    {
        File outputFile = new File( getBasedir(), "/target/test-classes/rss-feeds/generated-rss2.0-feed.xml" );

        List<RssFeedEntry> entries = new ArrayList<RssFeedEntry>();
        RssFeedEntry entry = new RssFeedEntry();

        entry.setTitle( "Item 1" );
        entry.setLink( "http://rss-2.0-test-feed.com" );
        entry.setDescription( "RSS 2.0 feed item 1." );
        entry.setGuid( "http://rss-2.0-test-feed.com/item1" );
        entries.add( entry );

        entry = new RssFeedEntry();
        entry.setTitle( "Item 2" );
        entry.setLink( "http://rss-2.0-test-feed.com" );
        entry.setDescription( "RSS 2.0 feed item 2." );
        entry.setGuid( "http://rss-2.0-test-feed.com/item2" );
        entries.add( entry );

        entry = new RssFeedEntry();
        entry.setTitle( "Item 3" );
        entry.setLink( "http://rss-2.0-test-feed.com" );
        entry.setDescription( "RSS 2.0 feed item 3." );
        entry.setGuid( "http://rss-2.0-test-feed.com/item3" );
        entries.add( entry );

        generator.generateFeed( "Test Feed", "http://localhost:8080/archiva", "The test feed from Archiva.", entries,
                                outputFile );

        String generatedContent = FileUtils.readFileToString( outputFile );

        XMLAssert.assertXpathEvaluatesTo( "Test Feed", "//channel/title", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "http://localhost:8080/archiva", "//channel/link", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "The test feed from Archiva.", "//channel/description", generatedContent );
        XMLAssert.assertXpathEvaluatesTo( "en-us", "//channel/language", generatedContent );

        String expectedItem1 =
            "<channel><item><title>Item 1</title></item><item><title>Item 2</title></item>"
                + "<item><title>Item 3</title></item></channel>";
        XMLAssert.assertXpathsEqual( "//channel/item/title", expectedItem1, "//channel/item/title", generatedContent );
    }
}
