package org.apache.maven.archiva.indexer.lucene.analyzers;

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

import org.apache.lucene.analysis.CharTokenizer;

import java.io.Reader;

/**
 * Lucene Tokenizer for {@link BytecodeKeys#CLASSES} fields. 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ClassnameTokenizer extends CharTokenizer
{
    public ClassnameTokenizer( Reader reader )
    {
        super( reader );
    }

    /**
     * Determine Token Character.
     * 
     * The field is a list of full classnames "com.foo.Object" seperated by
     * newline characters. "\n".
     * 
     * Identify newline "\n" and "." as the token delimiters.
     */
    protected boolean isTokenChar( char c )
    {
        return ( ( c != '\n' ) && ( c != '.' ) );
    }

    /*
    protected char normalize( char c )
    {
        return Character.toLowerCase( c );
    }
    */
}
