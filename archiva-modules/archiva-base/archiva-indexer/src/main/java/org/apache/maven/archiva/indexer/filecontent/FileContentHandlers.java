package org.apache.maven.archiva.indexer.filecontent;

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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.maven.archiva.indexer.lucene.LuceneEntryConverter;
import org.apache.maven.archiva.indexer.lucene.LuceneIndexHandlers;

/**
 * FileContentHandlers 
 *
 * @version $Id$
 */
public class FileContentHandlers
    implements LuceneIndexHandlers
{
    private FileContentAnalyzer analyzer;

    private FileContentConverter converter;

    private QueryParser queryParser;

    public FileContentHandlers()
    {
        analyzer = new FileContentAnalyzer();
        converter = new FileContentConverter();
        queryParser = new MultiFieldQueryParser( new String[] { FileContentKeys.FILENAME, FileContentKeys.CONTENT },
                                                 analyzer );
    }

    public String getId()
    {
        return FileContentKeys.ID;
    }

    public Analyzer getAnalyzer()
    {
        return analyzer;
    }

    public LuceneEntryConverter getConverter()
    {
        return converter;
    }

    public QueryParser getQueryParser()
    {
        return queryParser;
    }
}
