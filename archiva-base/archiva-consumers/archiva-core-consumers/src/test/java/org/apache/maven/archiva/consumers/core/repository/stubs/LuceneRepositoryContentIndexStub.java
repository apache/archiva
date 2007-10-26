package org.apache.maven.archiva.consumers.core.repository.stubs;

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
import java.util.Collection;

import junit.framework.Assert;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Searchable;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.lucene.LuceneEntryConverter;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;

/**
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 * @version
 */
public class LuceneRepositoryContentIndexStub
    implements RepositoryContentIndex
{

    public void deleteRecords( Collection records )
        throws RepositoryIndexException
    {
        // TODO Auto-generated method stub
        Assert.assertEquals( 2, records.size() );
    }

    public boolean exists()
        throws RepositoryIndexException
    {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection getAllRecordKeys()
        throws RepositoryIndexException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Analyzer getAnalyzer()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public LuceneEntryConverter getEntryConverter()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public File getIndexDirectory()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public QueryParser getQueryParser()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public ManagedRepositoryConfiguration getRepository()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Searchable getSearchable()
        throws RepositoryIndexSearchException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void indexRecords( Collection records )
        throws RepositoryIndexException
    {
        // TODO Auto-generated method stub

    }

    public void modifyRecord( LuceneRepositoryContentRecord record )
        throws RepositoryIndexException
    {
        // TODO Auto-generated method stub

    }

    public void modifyRecords( Collection records )
        throws RepositoryIndexException
    {
        // TODO Auto-generated method stub

    }

}
