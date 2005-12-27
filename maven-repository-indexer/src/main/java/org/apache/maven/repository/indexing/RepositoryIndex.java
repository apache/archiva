package org.apache.maven.repository.indexing;

import org.apache.lucene.analysis.Analyzer;

/**
 *
 * @author Edwin Punzalan
 */
public interface RepositoryIndex
{
    String ROLE = RepositoryIndex.class.getName();
    
    String[] getIndexFields();
    
    boolean isOpen();

    void index( Object obj ) throws RepositoryIndexException;

    void close() throws RepositoryIndexException;

    void open( String indexPath ) throws RepositoryIndexException;

    void optimize() throws RepositoryIndexException;

    Analyzer getAnalyzer();
    
    String getIndexPath();
}
