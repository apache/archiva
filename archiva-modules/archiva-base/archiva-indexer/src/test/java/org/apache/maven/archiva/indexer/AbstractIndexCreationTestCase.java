package org.apache.maven.archiva.indexer;

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

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

/**
 * AbstractIndexCreationTestCase 
 *
 * @version $Id$
 */
public abstract class AbstractIndexCreationTestCase extends AbstractIndexerTestCase
{
    protected abstract LuceneRepositoryContentRecord createSimpleRecord();

    public void testIndexExists() throws Exception
    {
        assertFalse( "check index doesn't exist", index.exists() );

        File indexLocation = index.getIndexDirectory();
        
        // create empty directory
        indexLocation.mkdirs();
        assertFalse( "check index doesn't exist even if directory does", index.exists() );

        // create index, with no records
        createEmptyIndex();
        assertTrue( "check index is considered to exist", index.exists() );

        // Test non-directory
        FileUtils.deleteDirectory( indexLocation );
        indexLocation.createNewFile();
        try
        {
            index.exists();
            fail( "Index operation should fail as the location is not valid" );
        }
        catch ( RepositoryIndexException e )
        {
            // great
        }
        finally
        {
            indexLocation.delete();
        }
    }

    public void testAddRecordNoIndex() throws IOException, RepositoryIndexException, ParseException
    {
        LuceneRepositoryContentRecord record = createSimpleRecord();

        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( index.getIndexDirectory() );
        try
        {
            assertEquals( "Check index size", 1, reader.numDocs() );

            Document document = reader.document( 0 );
            assertRecord( record, document );
        }
        finally
        {
            reader.close();
        }
    }

    public void testAddRecordExistingEmptyIndex() throws IOException, RepositoryIndexException, ParseException
    {
        createEmptyIndex();

        LuceneRepositoryContentRecord record = createSimpleRecord();

        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( index.getIndexDirectory() );
        try
        {
            assertEquals( "Check index size", 1, reader.numDocs() );

            Document document = reader.document( 0 );
            assertRecord( record, document );
        }
        finally
        {
            reader.close();
        }
    }

    public void testAddRecordInIndex() throws IOException, RepositoryIndexException, ParseException
    {
        createEmptyIndex();

        LuceneRepositoryContentRecord record = createSimpleRecord();

        index.indexRecords( Collections.singletonList( record ) );

        // Do it again
        record = createSimpleRecord();

        index.indexRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( index.getIndexDirectory() );
        try
        {
            assertEquals( "Check index size", 1, reader.numDocs() );

            Document document = reader.document( 0 );
            assertRecord( record, document );
        }
        finally
        {
            reader.close();
        }
    }

    public void testDeleteRecordInIndex() throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        LuceneRepositoryContentRecord record = createSimpleRecord();

        index.indexRecords( Collections.singletonList( record ) );

        index.deleteRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( index.getIndexDirectory() );
        try
        {
            assertEquals( "No documents", 0, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testDeleteRecordNotInIndex() throws IOException, RepositoryIndexException
    {
        createEmptyIndex();

        LuceneRepositoryContentRecord record = createSimpleRecord();

        index.deleteRecords( Collections.singletonList( record ) );

        IndexReader reader = IndexReader.open( index.getIndexDirectory() );
        try
        {
            assertEquals( "No documents", 0, reader.numDocs() );
        }
        finally
        {
            reader.close();
        }
    }

    public void testDeleteRecordNoIndex() throws IOException, RepositoryIndexException
    {
        LuceneRepositoryContentRecord record = createSimpleRecord();

        index.deleteRecords( Collections.singleton( record ) );

        assertFalse( index.exists() );
    }
}
