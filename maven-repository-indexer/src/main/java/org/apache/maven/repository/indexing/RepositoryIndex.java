package org.apache.maven.repository.indexing;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.maven.artifact.repository.ArtifactRepository;

/**
 * @author Edwin Punzalan
 */
public interface RepositoryIndex
{
    /**
     * Method used to query the index status
     *
     * @return true if the index is open.
     */
    boolean isOpen();

    /**
     * Method to close open streams to the index directory
     */
    void close()
        throws RepositoryIndexException;

    ArtifactRepository getRepository();

    /**
     * Method to encapsulate the optimize() method for lucene
     */
    void optimize()
        throws RepositoryIndexException;

    /**
     * Method to retrieve the lucene analyzer object used in creating the document fields for this index
     *
     * @return lucene Analyzer object used in creating the index fields
     */
    Analyzer getAnalyzer();

    /**
     * Method to retrieve the path where the index is made available
     *
     * @return the path where the index resides
     */
    String getIndexPath();

    /**
     * Tests an index field if it is a keyword field
     *
     * @param field the name of the index field to test
     * @return true if the index field passed is a keyword, otherwise its false
     */
    boolean isKeywordField( String field );
}
