package org.apache.maven.archiva.indexer.search;

/**
 * Search across repositories for specified term. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * @todo add security to not perform search in repositories you don't have access to.
 */
public interface CrossRepositorySearch
{
    /**
     * Search for the specific term across all repositories.
     * 
     * @param term the term to search for.
     * @return the results.
     */
    public SearchResults searchForTerm( String term );

    /**
     * Search for the specific MD5 string across all repositories.
     * 
     * @param md5 the md5 string to search for.
     * @return the results.
     */
    public SearchResults searchForMd5( String md5 );
}
