package org.apache.maven.archiva.indexer.bytecode;

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
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.maven.archiva.indexer.ArtifactKeys;
import org.apache.maven.archiva.indexer.lucene.analyzers.ClassnameTokenizer;
import org.apache.maven.archiva.indexer.lucene.analyzers.FilenamesTokenizer;
import org.apache.maven.archiva.indexer.lucene.analyzers.GroupIdTokenizer;
import org.apache.maven.archiva.indexer.lucene.analyzers.VersionTokenizer;

import java.io.Reader;

/**
 * BytecodeAnalyzer 
 *
 * @version $Id$
 */
public class BytecodeAnalyzer extends Analyzer
{
    private static final Analyzer STANDARD = new StandardAnalyzer();

    public TokenStream tokenStream( String field, Reader reader )
    {
        TokenStream tokenStream = null;

        if ( BytecodeKeys.CLASSES.equals( field ) )
        {
            tokenStream = new ClassnameTokenizer( reader );
        }
        else if ( BytecodeKeys.FILES.equals( field ) )
        {
            tokenStream = new FilenamesTokenizer( reader );
        }
        else if ( ArtifactKeys.GROUPID.equals( field ) )
        {
            tokenStream = new GroupIdTokenizer( reader );
        }
        else if ( ArtifactKeys.VERSION.equals( field ) )
        {
            tokenStream = new VersionTokenizer( reader );
        }
        else
        {
            tokenStream = STANDARD.tokenStream( field, reader );
        }
        
        return new LowerCaseFilter( new StopFilter( tokenStream, StopAnalyzer.ENGLISH_STOP_WORDS ) );
    }
}
