package org.apache.maven.repository.indexing;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;

/**
 *
 * @author Edwin Punzalan
 */
public abstract class AbstractRepositoryIndexer
    implements RepositoryIndexer
{
    protected String indexPath;
    protected IndexReader indexReader;
    protected IndexWriter indexWriter;

    protected void getIndexWriter()
        throws IOException
    {
        if ( indexWriter == null )
        {
            indexWriter = new IndexWriter( indexPath, getAnalyzer(), true );
        }
    }

    protected void getIndexReader()
        throws IOException
    {
        if ( indexReader == null )
        {
            indexReader = IndexReader.open( indexPath );
        }
    }

    protected Analyzer getAnalyzer()
    {
        return new StandardAnalyzer();
    }
}
