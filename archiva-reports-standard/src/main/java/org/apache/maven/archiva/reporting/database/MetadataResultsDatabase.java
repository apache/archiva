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
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.archiva.reporting.model.MetadataResultsKey;
import org.apache.maven.archiva.reporting.model.ResultReason;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jdo.JDOObjectNotFoundException;

/**
 * MetadataResultsDatabase 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.reporting.database.MetadataResultsDatabase"
 *                   role-hint="default"
 */
public class MetadataResultsDatabase
    extends AbstractResultsDatabase
{
    public static final String ROLE = MetadataResultsDatabase.class.getName();

    public void addFailure( RepositoryMetadata metadata, String processor, String problem, String reason )
    {
        MetadataResults results = getMetadataResults( metadata );
        ResultReason result = createResultReason( processor, problem, reason );

        if ( !results.getFailures().contains( result ) )
        {
            results.addFailure( result );
        }

        saveObject( results );
    }

    public void addWarning( RepositoryMetadata metadata, String processor, String problem, String reason )
    {
        MetadataResults results = getMetadataResults( metadata );
        ResultReason result = createResultReason( processor, problem, reason );

        if ( !results.getWarnings().contains( result ) )
        {
            results.addWarning( result );
        }

        saveObject( results );
    }

    public void addNotice( RepositoryMetadata metadata, String processor, String problem, String reason )
    {
        MetadataResults results = getMetadataResults( metadata );
        ResultReason result = createResultReason( processor, problem, reason );

        if ( !results.getNotices().contains( result ) )
        {
            results.addNotice( result );
        }

        saveObject( results );
    }

    public void clearResults( MetadataResults results )
    {
        results.getFailures().clear();
        results.getWarnings().clear();
        results.getNotices().clear();

        saveObject( results );
    }

    public List getAllMetadataResults()
    {
        return getAllObjects( MetadataResults.class, null );
    }

    public Iterator getIterator()
    {
        List allmetadatas = getAllMetadataResults();
        if ( allmetadatas == null )
        {
            return Collections.EMPTY_LIST.iterator();
        }

        return allmetadatas.iterator();
    }

    public void remove( MetadataResults results )
    {
        removeObject( results );
    }

    public void remove( RepositoryMetadata metadata )
    {
        try
        {
            MetadataResults results = lookupMetadataResults( metadata );
            remove( results );
        }
        catch ( JDOObjectNotFoundException e )
        {
            // nothing to do.
        }
    }

    public MetadataResults getMetadataResults( RepositoryMetadata metadata )
    {
        MetadataResults results;

        try
        {
            results = lookupMetadataResults( metadata );
        }
        catch ( JDOObjectNotFoundException e )
        {
            results = createMetadataResults( metadata );
        }

        return results;
    }

    private MetadataResults createMetadataResults( RepositoryMetadata metadata )
    {
        /* The funky StringUtils.defaultString() is used because of database constraints.
         * The MetadataResults object has a complex primary key consisting of groupId, artifactId, and version.
         * This also means that none of those fields may be null.  however, that doesn't eliminate the
         * ability to have an empty string in place of a null.
         */

        MetadataResults results = new MetadataResults();
        results.setGroupId( StringUtils.defaultString( metadata.getGroupId() ) );
        results.setArtifactId( StringUtils.defaultString( metadata.getArtifactId() ) );
        results.setVersion( StringUtils.defaultString( metadata.getBaseVersion() ) );

        return results;
    }

    private MetadataResults lookupMetadataResults( RepositoryMetadata metadata )
    {
        /* The funky StringUtils.defaultString() is used because of database constraints.
         * The MetadataResults object has a complex primary key consisting of groupId, artifactId, and version.
         * This also means that none of those fields may be null.  however, that doesn't eliminate the
         * ability to have an empty string in place of a null.
         */

        MetadataResultsKey key = new MetadataResultsKey();
        key.groupId = StringUtils.defaultString( metadata.getGroupId(), "" );
        key.artifactId = StringUtils.defaultString( metadata.getArtifactId(), "" );
        key.version = StringUtils.defaultString( metadata.getBaseVersion(), "" );

        return (MetadataResults) getObjectByKey( MetadataResults.class, key );
    }

    public int getNumFailures()
    {
        int count = 0;
        for ( Iterator it = getIterator(); it.hasNext(); )
        {
            MetadataResults results = (MetadataResults) it.next();
            count += results.getFailures().size();
        }
        return count;
    }

    public int getNumNotices()
    {
        int count = 0;
        for ( Iterator it = getIterator(); it.hasNext(); )
        {
            MetadataResults results = (MetadataResults) it.next();
            count += results.getNotices().size();
        }
        return count;
    }

    public int getNumWarnings()
    {
        int count = 0;
        for ( Iterator it = getIterator(); it.hasNext(); )
        {
            MetadataResults results = (MetadataResults) it.next();
            count += results.getWarnings().size();
        }
        return count;
    }
}
