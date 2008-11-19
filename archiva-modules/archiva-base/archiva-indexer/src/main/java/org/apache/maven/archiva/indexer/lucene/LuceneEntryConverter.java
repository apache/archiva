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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.lucene.document.Document;

import java.text.ParseException;

/**
 * A converter for {@link LuceneRepositoryContentRecord} to Lucene {@link Document} objects and back.
 *
 */
public interface LuceneEntryConverter
{
    /**
     * Convert an index record to a Lucene document.
     *
     * @param record the record
     * @return the document
     */
    Document convert( LuceneRepositoryContentRecord record );

    /**
     * Convert a Lucene document to an index record.
     *
     * @param document the document
     * @return the record
     * @throws java.text.ParseException if there is a problem parsing a field (specifically, dates)
     */
    LuceneRepositoryContentRecord convert( Document document )
        throws ParseException;
}
