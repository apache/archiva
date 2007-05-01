package org.apache.maven.archiva.reporting.database;

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
import org.apache.maven.archiva.reporting.model.ArtifactResults;
import org.apache.maven.archiva.reporting.model.ArtifactResultsKey;
import org.apache.maven.archiva.reporting.model.ResultReason;
import org.apache.maven.artifact.Artifact;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;

/**
 * ArtifactResultsDatabase - Database of ArtifactResults. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase"
 *                   role-hint="default"
 */
public class ArtifactResultsDatabase
    extends AbstractResultsDatabase
{
    // -------------------------------------------------------------------
    // ArtifactResults methods.
    // -------------------------------------------------------------------

    public static final String ROLE = ArtifactResultsDatabase.class.getName();

    public void addFailure( Artifact artifact, String processor, String problem, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        ResultReason result = createResultReason( processor, problem, reason );

        if ( !results.getFailures().contains( result ) )
        {
            results.addFailure( result );
        }

        saveObject( results );
    }

    public void addNotice( Artifact artifact, String processor, String problem, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        ResultReason result = createResultReason( processor, problem, reason );

        if ( !results.getNotices().contains( result ) )
        {
            results.addNotice( result );
        }

        saveObject( results );
    }

    public void addWarning( Artifact artifact, String processor, String problem, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        ResultReason result = createResultReason( processor, problem, reason );

        if ( !results.getWarnings().contains( result ) )
        {
            results.addWarning( result );
        }

        saveObject( results );
    }

    public void clearResults( ArtifactResults results )
    {
        results.getFailures().clear();
        results.getWarnings().clear();
        results.getNotices().clear();

        saveObject( results );
    }

    public List getAllArtifactResults()
    {
        return getAllObjects( ArtifactResults.class, null );
    }

    public Iterator getIterator()
    {
        List allartifacts = getAllArtifactResults();
        if ( allartifacts == null )
        {
            return Collections.EMPTY_LIST.iterator();
        }

        return allartifacts.iterator();
    }

    public List findArtifactResults( String groupId, String artifactId, String version )
    {
        PersistenceManager pm = getPersistenceManager();
        Transaction tx = pm.currentTransaction();

        try
        {
            tx.begin();

            Query query = pm.newQuery( "javax.jdo.query.JDOQL", "SELECT FROM " + ArtifactResults.class.getName()
                + " WHERE groupId == findGroupId && " + " artifactId == findArtifactId && "
                + " version == findVersionId" );
            query.declareParameters( "String findGroupId, String findArtifactId, String findVersionId" );
            query.setOrdering( "findArtifactId ascending" );

            List result = (List) query.execute( groupId, artifactId, version );

            result = (List) pm.detachCopyAll( result );

            tx.commit();

            return result;
        }
        finally
        {
            rollbackIfActive( tx );
        }
    }

    public void remove( ArtifactResults results )
    {
        removeObject( results );
    }

    public void remove( Artifact artifact )
    {
        try
        {
            ArtifactResults results = lookupArtifactResults( artifact );
            remove( results );
        }
        catch ( JDOObjectNotFoundException e )
        {
            // nothing to do.
        }
    }

    /**
     * Get an {@link ArtifactResults} from the store.
     * If the store does not have one, create it.
     * 
     * Equivalent to calling {@link #lookupArtifactResults(Artifact)} then if
     * not found, using {@link #createArtifactResults(Artifact)}.
     * 
     * @param artifact the artifact information
     * @return the ArtifactResults object (may not be in database yet, so don't forget to {@link #saveObject(Object)})
     * @see #lookupArtifactResults(Artifact)
     * @see #createArtifactResults(Artifact)
     */
    public ArtifactResults getArtifactResults( Artifact artifact )
    {
        ArtifactResults results;

        try
        {
            results = lookupArtifactResults( artifact );
        }
        catch ( JDOObjectNotFoundException e )
        {
            results = createArtifactResults( artifact );
        }

        return results;
    }

    /**
     * Create a new {@link ArtifactResults} object from the provided Artifact information.
     * 
     * @param artifact the artifact information.
     * @return the new {@link ArtifactResults} object.
     * @see #getArtifactResults(Artifact)
     * @see #lookupArtifactResults(Artifact)
     */
    private ArtifactResults createArtifactResults( Artifact artifact )
    {
        /* The funky StringUtils.defaultString() is used because of database constraints.
         * The ArtifactResults object has a complex primary key consisting of groupId, artifactId, version,
         * type, classifier.
         * This also means that none of those fields may be null.  however, that doesn't eliminate the
         * ability to have an empty string in place of a null.
         */

        ArtifactResults results = new ArtifactResults();
        results.setGroupId( StringUtils.defaultString( artifact.getGroupId() ) );
        results.setArtifactId( StringUtils.defaultString( artifact.getArtifactId() ) );
        results.setVersion( StringUtils.defaultString( artifact.getVersion() ) );
        results.setArtifactType( StringUtils.defaultString( artifact.getType() ) );
        results.setClassifier( StringUtils.defaultString( artifact.getClassifier() ) );

        return results;
    }

    /**
     * Lookup the {@link ArtifactResults} in the JDO store from the information in
     * the provided Artifact.
     * 
     * @param artifact the artifact information.
     * @return the previously saved {@link ArtifactResults} from the JDO store.
     * @throws JDOObjectNotFoundException if the {@link ArtifactResults} are not found.
     * @see #getArtifactResults(Artifact)
     * @see #createArtifactResults(Artifact)
     */
    private ArtifactResults lookupArtifactResults( Artifact artifact )
        throws JDOObjectNotFoundException
    {
        /* The funky StringUtils.defaultString() is used because of database constraints.
         * The ArtifactResults object has a complex primary key consisting of groupId, artifactId, version,
         * type, classifier.
         * This also means that none of those fields may be null.  however, that doesn't eliminate the
         * ability to have an empty string in place of a null.
         */

        ArtifactResultsKey key = new ArtifactResultsKey();
        key.groupId = StringUtils.defaultString( artifact.getGroupId() );
        key.artifactId = StringUtils.defaultString( artifact.getArtifactId() );
        key.version = StringUtils.defaultString( artifact.getVersion() );
        key.artifactType = StringUtils.defaultString( artifact.getType() );
        key.classifier = StringUtils.defaultString( artifact.getClassifier() );

        return (ArtifactResults) getObjectByKey( ArtifactResults.class, key );
    }

    public int getNumFailures()
    {
        int count = 0;
        for ( Iterator it = getIterator(); it.hasNext(); )
        {
            ArtifactResults results = (ArtifactResults) it.next();
            count += results.getFailures().size();
        }
        return count;
    }

    public int getNumNotices()
    {
        int count = 0;
        for ( Iterator it = getIterator(); it.hasNext(); )
        {
            ArtifactResults results = (ArtifactResults) it.next();
            count += results.getNotices().size();
        }
        return count;
    }

    public int getNumWarnings()
    {
        int count = 0;
        for ( Iterator it = getIterator(); it.hasNext(); )
        {
            ArtifactResults results = (ArtifactResults) it.next();
            count += results.getWarnings().size();
        }
        return count;
    }
}
