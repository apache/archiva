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
    String ROLE = RepositoryIndex.class.getName();

    String[] getIndexFields();

    boolean isOpen();

    void index( Object obj )
        throws RepositoryIndexException;

    void close()
        throws RepositoryIndexException;

    ArtifactRepository getRepository();

    void optimize()
        throws RepositoryIndexException;

    Analyzer getAnalyzer();

    String getIndexPath();
}
