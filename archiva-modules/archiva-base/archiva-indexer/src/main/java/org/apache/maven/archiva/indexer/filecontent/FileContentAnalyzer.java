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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.maven.archiva.indexer.lucene.analyzers.FilenamesTokenizer;
import org.apache.maven.archiva.indexer.lucene.analyzers.ArtifactIdTokenizer;
import org.apache.maven.archiva.indexer.lucene.analyzers.GroupIdTokenizer;

import java.io.Reader;
import org.apache.maven.archiva.indexer.lucene.analyzers.VersionTokenizer;

/**
 * FileContentAnalyzer 
 *
 * @version $Id$
 */
public class FileContentAnalyzer extends Analyzer
{
    private static final Analyzer STANDARD = new StandardAnalyzer();

    public TokenStream tokenStream( String field, Reader reader )
    {
        if ( FileContentKeys.FILENAME.equals( field ) )
        {
            return new FilenamesTokenizer( reader );
        }

        if ( FileContentKeys.ARTIFACTID.equals( field ))
        {
            return new ArtifactIdTokenizer(reader);
        }

        if ( FileContentKeys.GROUPID.equals( field ) )
        {
            return new GroupIdTokenizer(reader);
        }

        if ( FileContentKeys.VERSION.equals( field ))
        {
            return new VersionTokenizer(reader);
        }

        return STANDARD.tokenStream( field, reader );
    }
}
