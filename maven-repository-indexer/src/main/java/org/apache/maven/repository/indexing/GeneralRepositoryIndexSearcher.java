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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.repository.indexing.query.CompoundQuery;
import org.apache.maven.repository.indexing.query.Query;
import org.apache.maven.repository.indexing.query.SinglePhraseQuery;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Maria Odea Ching
 *         <p/>
 *         This class is for "query everything" search in the repository index.
 *         It creates the Query object that will be passed to the DefaultRepositoryIndexSearcher
 *         for searching through all the fields in the index.
 */
public class GeneralRepositoryIndexSearcher
{
    private RepositoryIndex index;

    private ArtifactFactory factory;

    /**
     * Class constructor
     *
     * @param index
     */
    public GeneralRepositoryIndexSearcher( RepositoryIndex index, ArtifactFactory factory )
    {
        this.index = index;
        this.factory = factory;
    }

    /**
     * Method for searching the keyword in all the fields in the index. The index fields will be retrieved
     * and query objects will be constructed using the optional (OR) CompoundQuery.
     *
     * @param keyword
     * @return
     * @throws RepositoryIndexSearchException
     */
    public List search( String keyword )
        throws RepositoryIndexSearchException
    {
        List qryList = new ArrayList();
        for ( int i = 0; i < index.FIELDS.length; i++ )
        {
            Query qry = new SinglePhraseQuery( index.FIELDS[i], keyword );
            qryList.add( qry );
        }

        CompoundQuery cQry = new CompoundQuery();
        for ( Iterator iter = qryList.iterator(); iter.hasNext(); )
        {
            cQry.or( (Query) iter.next() );
        }
        RepositoryIndexSearcher searcher = new DefaultRepositoryIndexSearcher( index, factory );

        return searcher.search( cQry );
    }

}
