package org.apache.maven.archiva.indexing.record;

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

import org.apache.maven.archiva.indexing.RepositoryIndexException;
import org.apache.maven.artifact.Artifact;

/**
 * The layout of a record in a repository index.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface RepositoryIndexRecordFactory
{
    /**
     * The Plexus role.
     */
    String ROLE = RepositoryIndexRecordFactory.class.getName();

    /**
     * Create an index record from an artifact.
     *
     * @param artifact the artifact
     * @return the index record
     * @throws RepositoryIndexException if there is a problem constructing the record (due to not being able to read the artifact file as a POM)
     */
    RepositoryIndexRecord createRecord( Artifact artifact )
        throws RepositoryIndexException;

}
