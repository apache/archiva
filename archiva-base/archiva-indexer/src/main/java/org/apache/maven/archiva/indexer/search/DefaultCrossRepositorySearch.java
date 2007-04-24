package org.apache.maven.archiva.indexer.search;

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

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.indexer.RepositoryContentIndex;
import org.apache.maven.archiva.indexer.RepositoryContentIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.bytecode.BytecodeKeys;
import org.apache.maven.archiva.indexer.filecontent.FileContentKeys;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesKeys;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * DefaultCrossRepositorySearch 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.indexer.search.CrossRepositorySearch" role-hint="default"
 */
public class DefaultCrossRepositorySearch
    extends AbstractLogEnabled
    implements CrossRepositorySearch, RegistryListener, Initializable
{

    private static final int UNKNOWN = 0;

    private static final int FILE_CONTENT = 1;

    private static final int BYTECODE = 2;

    private static final int HASHCODE = 3;

    /**
     * @plexus.requirement role-hint="lucene"
     */
    private RepositoryContentIndexFactory indexFactory;

    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration configuration;

    private Map repositoryMap = new HashMap();

    public SearchResults searchForMd5( String md5 )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public SearchResults searchForTerm( String term )
    {
        List indexes = new ArrayList();

        indexes.addAll( getBytecodeIndexes() );
        indexes.addAll( getFileContentIndexes() );
        indexes.addAll( getHashcodeIndexes() );

        SearchResults results = new SearchResults();

        results.getRepositories().addAll( this.repositoryMap.values() );

        Iterator it = indexes.iterator();
        while ( it.hasNext() )
        {
            RepositoryContentIndex index = (RepositoryContentIndex) it.next();

            try
            {
                QueryParser parser = index.getQueryParser();
                LuceneQuery query = new LuceneQuery( parser.parse( term ) );
                List hits = index.search( query );

                switch ( getIndexId( index ) )
                {
                    case BYTECODE:
                        results.getBytecodeHits().addAll( hits );
                        break;
                    case FILE_CONTENT:
                        results.getContentHits().addAll( hits );
                        break;
                    case HASHCODE:
                        results.getHashcodeHits().addAll( hits );
                        break;
                }
            }
            catch ( ParseException e )
            {
                getLogger().warn( "Unable to parse query [" + term + "]: " + e.getMessage(), e );
            }
            catch ( RepositoryIndexSearchException e )
            {
                getLogger().warn( "Unable to search index [" + index + "] for term [" + term + "]: " + e.getMessage(),
                                  e );
            }
        }

        return results;
    }

    private int getIndexId( RepositoryContentIndex index )
    {
        if ( FileContentKeys.ID.equals( index.getId() ) )
        {
            return FILE_CONTENT;
        }

        if ( BytecodeKeys.ID.equals( index.getId() ) )
        {
            return BYTECODE;
        }

        if ( HashcodesKeys.ID.equals( index.getId() ) )
        {
            return HASHCODE;
        }

        return UNKNOWN;
    }

    public List getBytecodeIndexes()
    {
        List ret = new ArrayList();

        synchronized ( this.repositoryMap )
        {
            Iterator it = this.repositoryMap.values().iterator();
            while ( it.hasNext() )
            {
                ArchivaRepository repo = (ArchivaRepository) it.next();

                if ( !isSearchAllowed( repo ) )
                {
                    continue;
                }

                ret.add( indexFactory.createBytecodeIndex( repo ) );
            }
        }

        return ret;
    }

    public List getFileContentIndexes()
    {
        List ret = new ArrayList();

        synchronized ( this.repositoryMap )
        {
            Iterator it = this.repositoryMap.values().iterator();
            while ( it.hasNext() )
            {
                ArchivaRepository repo = (ArchivaRepository) it.next();

                if ( !isSearchAllowed( repo ) )
                {
                    continue;
                }

                ret.add( indexFactory.createFileContentIndex( repo ) );
            }
        }

        return ret;
    }

    public List getHashcodeIndexes()
    {
        List ret = new ArrayList();

        synchronized ( this.repositoryMap )
        {
            Iterator it = this.repositoryMap.values().iterator();
            while ( it.hasNext() )
            {
                ArchivaRepository repo = (ArchivaRepository) it.next();

                if ( !isSearchAllowed( repo ) )
                {
                    continue;
                }

                ret.add( indexFactory.createHashcodeIndex( repo ) );
            }
        }

        return ret;
    }

    public boolean isSearchAllowed( ArchivaRepository repo )
    {
        // TODO: test if user has permissions to search in this repo.

        return true;
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isRepositories( propertyName ) )
        {
            initRepositoryMap();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* Nothing to do here */
    }

    private void initRepositoryMap()
    {
        synchronized ( this.repositoryMap )
        {
            this.repositoryMap.clear();

            Iterator it = configuration.getConfiguration().createRepositoryMap().entrySet().iterator();
            while ( it.hasNext() )
            {
                Map.Entry entry = (Entry) it.next();
                String key = (String) entry.getKey();
                RepositoryConfiguration repoConfig = (RepositoryConfiguration) entry.getValue();
                ArchivaRepository repository = ArchivaConfigurationAdaptor.toArchivaRepository( repoConfig );
                this.repositoryMap.put( key, repository );
            }
        }
    }

    public void initialize()
        throws InitializationException
    {
        initRepositoryMap();
        configuration.addChangeListener( this );
    }
}
