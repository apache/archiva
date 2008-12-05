package org.apache.maven.archiva.indexer.lucene;

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

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.util.List;

/**
 * LuceneDocumentMaker - a utility class for making lucene documents. 
 *
 * @version $Id$
 */
public class LuceneDocumentMaker
{
    public static final String PRIMARY_KEY = "pk";
    
    public static final String REPOSITORY_ID = "repoId";

    private Document document;

    /**
     * Construct a LuceneDocumentMaker based on the record provider.
     * 
     * @param record the record.
     * @throws IllegalArgumentException if the primary key is invalid.
     */
    public LuceneDocumentMaker( LuceneRepositoryContentRecord record ) throws IllegalArgumentException
    {
        if ( record == null )
        {
            throw new IllegalArgumentException( "Not allowed to have a null record provider." );
        }

        String primaryKey = record.getPrimaryKey();

        if ( StringUtils.isBlank( primaryKey ) )
        {
            throw new IllegalArgumentException( "Not allowed to have a blank primary key." );
        }

        String repositoryId = record.getRepositoryId();
        
        if ( StringUtils.isBlank( repositoryId ) )
        {
            throw new IllegalArgumentException( "Not allowed to have a blank repository id." );
        }

        document = new Document();

        document.add( new Field( PRIMARY_KEY, primaryKey, Field.Store.NO, Field.Index.UN_TOKENIZED ) );
        document.add( new Field( REPOSITORY_ID, repositoryId, Field.Store.YES, Field.Index.UN_TOKENIZED ) );
    }

    public LuceneDocumentMaker addFieldTokenized( String key, String value )
    {
        if ( value != null )
        {
            document.add( new Field( key, value, Field.Store.YES, Field.Index.TOKENIZED ) );
        }

        return this;
    }

    public LuceneDocumentMaker addFieldTokenized( String key, List list )
    {
        if ( ( list != null ) && ( !list.isEmpty() ) )
        {
            return addFieldTokenized( key, StringUtils.join( list.iterator(), "\n" ) );
        }

        return this;
    }

    public LuceneDocumentMaker addFieldUntokenized( String name, String value )
    {
        if ( value != null )
        {
            document.add( new Field( name, value, Field.Store.YES, Field.Index.UN_TOKENIZED ) );
        }

        return this;
    }

    public LuceneDocumentMaker addFieldExact( String name, String value )
    {
        if ( value != null )
        {
            document.add( new Field( name, value, Field.Store.NO, Field.Index.UN_TOKENIZED ) );
        }

        return this;
    }

    public Document getDocument()
    {
        return this.document;
    }
}
