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

import org.apache.maven.repository.indexing.query.Query;

import java.util.List;

/**
 * Repository search layer.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public interface RepositoryIndexSearchLayer
{
    /**
     * The Plexus component role name.
     */
    String ROLE = RepositoryIndexSearchLayer.class.getName();

    /**
     * Method for searching the keyword in all the fields in the index. "Query everything" search.
     * The index fields will be retrieved and query objects will be constructed using the
     * optional (OR) CompoundQuery.
     *
     * @param keyword
     * @param index
     * @return
     * @throws RepositoryIndexSearchException
     *
     */
    List searchGeneral( String keyword, RepositoryIndex index )
        throws RepositoryIndexSearchException;

    /**
     * Method for "advanced search" of the index
     *
     * @param qry   the query object that will be used for searching the index
     * @param index
     * @return
     * @throws RepositoryIndexSearchException
     *
     */
    List searchAdvanced( Query qry, RepositoryIndex index )
        throws RepositoryIndexSearchException;
}
