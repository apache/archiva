package org.apache.maven.archiva.indexer.functors;

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

import org.apache.commons.collections.Transformer;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentIndex;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * SearchableTransformer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.commons.collections.Transformer" role-hint="searchable"
 */
public class SearchableTransformer
    extends AbstractLogEnabled
    implements Transformer
{
    public Object transform( Object input )
    {
        if ( input instanceof LuceneRepositoryContentIndex )
        {
            try
            {
                LuceneRepositoryContentIndex index = (LuceneRepositoryContentIndex) input;
                return index.getSearchable();
            }
            catch ( RepositoryIndexSearchException e )
            {
                getLogger().warn("Unable to get searchable for index:" + e.getMessage(), e);
            }
        }
        
        return input;
    }
}
