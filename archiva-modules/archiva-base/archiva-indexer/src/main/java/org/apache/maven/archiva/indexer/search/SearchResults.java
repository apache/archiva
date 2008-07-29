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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.indexer.bytecode.BytecodeRecord;
import org.apache.maven.archiva.indexer.filecontent.FileContentRecord;
import org.apache.maven.archiva.indexer.hashcodes.HashcodesRecord;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryContentRecord;
import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SearchResults 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class SearchResults
{
    private List repositories = new ArrayList();

    private Map hits = new HashMap();

    private int totalHits;

    private SearchResultLimits limits;

    public SearchResults()
    {
        /* do nothing */
    }

    public void addHit( LuceneRepositoryContentRecord record )
    {
        if ( record instanceof FileContentRecord )
        {
            FileContentRecord filecontent = (FileContentRecord) record;
            addFileContentHit( filecontent );
        }
        else if ( record instanceof HashcodesRecord )
        {
            HashcodesRecord hashcodes = (HashcodesRecord) record;
            addHashcodeHit( hashcodes );
        }
        else if ( record instanceof BytecodeRecord )
        {
            BytecodeRecord bytecode = (BytecodeRecord) record;
            addBytecodeHit( bytecode );
        }
    }

    private void addBytecodeHit( BytecodeRecord bytecode )
    {
        String key = toKey( bytecode.getArtifact() );

        SearchResultHit hit = (SearchResultHit) this.hits.get( key );

        if ( hit == null )
        {
            hit = new SearchResultHit();
        }
        
        hit.setRepositoryId( bytecode.getRepositoryId() );
        hit.addArtifact( bytecode.getArtifact() );
        hit.setContext( null ); // TODO: provide context on why this is a valuable hit.

        this.hits.put( key, hit );
    }

    private String toKey( ArchivaArtifact artifact )
    {
        StringBuffer key = new StringBuffer();

        key.append( StringUtils.defaultString( artifact.getModel().getRepositoryId() ) ).append( ":" );
        key.append( StringUtils.defaultString( artifact.getGroupId() ) ).append( ":" );
        key.append( StringUtils.defaultString( artifact.getArtifactId() ) );

        return key.toString();
    }

    private void addHashcodeHit( HashcodesRecord hashcodes )
    {
        String key = toKey( hashcodes.getArtifact() );

        SearchResultHit hit = (SearchResultHit) this.hits.get( key );

        if ( hit == null )
        {
            hit = new SearchResultHit();
        }

        hit.addArtifact( hashcodes.getArtifact() );
        hit.setContext( null ); // TODO: provide context on why this is a valuable hit.

        this.hits.put( key, hit );
    }

    public void addFileContentHit( FileContentRecord filecontent )
    {
        String key = filecontent.getPrimaryKey();

        SearchResultHit hit = (SearchResultHit) this.hits.get( key );

        if ( hit == null )
        {
            // Only need to worry about this hit if it is truely new.
            hit = new SearchResultHit();

            hit.setRepositoryId( filecontent.getRepositoryId() );
            hit.setUrl( filecontent.getRepositoryId() + "/" + filecontent.getFilename() );
            hit.setContext( null ); // TODO: handle context + highlight later.
            
            // Test for possible artifact reference ...
            if( filecontent.getArtifact() != null )
            {
                hit.addArtifact( filecontent.getArtifact() );
            }

            this.hits.put( key, hit );
        }
    }

    /**
     * Get the list of {@link SearchResultHit} objects.
     * 
     * @return the list of {@link SearchResultHit} objects.
     */
    public List getHits()
    {
        return new ArrayList( hits.values() );
    }

    public List getRepositories()
    {
        return repositories;
    }

    public boolean isEmpty()
    {
        return hits.isEmpty();
    }

    public void setRepositories( List repositories )
    {
        this.repositories = repositories;
    }

    public SearchResultLimits getLimits()
    {
        return limits;
    }

    public void setLimits( SearchResultLimits limits )
    {
        this.limits = limits;
    }

    public int getTotalHits()
    {
        return totalHits;
    }

    public void setTotalHits( int totalHits )
    {
        this.totalHits = totalHits;
    }
}
