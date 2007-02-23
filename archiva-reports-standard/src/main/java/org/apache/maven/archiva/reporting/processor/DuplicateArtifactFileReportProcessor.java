package org.apache.maven.archiva.reporting.processor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.indexer.record.StandardArtifactIndexRecord;
import org.apache.maven.archiva.indexer.record.StandardIndexRecordFields;
import org.apache.maven.archiva.reporting.database.ArtifactResultsDatabase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.codehaus.plexus.digest.Digester;
import org.codehaus.plexus.digest.DigesterException;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Validates an artifact file for duplicates within the same groupId based from what's available in a repository index.
 *
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.archiva.reporting.processor.ArtifactReportProcessor" role-hint="duplicate"
 */
public class DuplicateArtifactFileReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement role-hint="md5"
     */
    private Digester digester;

    /**
     * @plexus.requirement
     */
    private RepositoryArtifactIndexFactory indexFactory;

    /**
     * @plexus.configuration
     */
    private String indexDirectory;

    /**
     * @plexus.requirement
     */
    private ArtifactResultsDatabase database;

    private static final String ROLE_HINT = "duplicate";

    public void processArtifact( Artifact artifact, Model model )
    {
        ArtifactRepository repository = artifact.getRepository();
        if ( artifact.getFile() != null )
        {
            System.out.println( "indexDirectory = " + indexDirectory );

            File indexDirectoryFile = new File( indexDirectory );

            RepositoryArtifactIndex index = indexFactory.createStandardIndex( indexDirectoryFile );

            String checksum = null;
            try
            {
                checksum = digester.calc( artifact.getFile() );
            }
            catch ( DigesterException e )
            {
                addWarning( artifact, null, "Unable to generate checksum for " + artifact.getFile() + ": " + e );
            }

            if ( checksum != null )
            {
                try
                {
                    List results = index
                        .search( new LuceneQuery( new TermQuery( new Term( StandardIndexRecordFields.MD5, checksum
                            .toLowerCase() ) ) ) );

                    if ( !results.isEmpty() )
                    {
                        for ( Iterator i = results.iterator(); i.hasNext(); )
                        {
                            StandardArtifactIndexRecord result = (StandardArtifactIndexRecord) i.next();

                            //make sure it is not the same artifact
                            if ( !result.getFilename().equals( repository.pathOf( artifact ) ) )
                            {
                                //report only duplicates from the same groupId
                                String groupId = artifact.getGroupId();
                                if ( groupId.equals( result.getGroupId() ) )
                                {
                                    addFailure( artifact, "duplicate", "Found duplicate for " + artifact.getId() );
                                }
                            }
                        }
                    }
                }
                catch ( RepositoryIndexSearchException e )
                {
                    addWarning( artifact, null, "Failed to search in index" + e );
                }
            }
        }
        else
        {
            addWarning( artifact, null, "Artifact file is null" );
        }
    }

    private void addFailure( Artifact artifact, String problem, String reason )
    {
        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
        database.addFailure( artifact, ROLE_HINT, problem, reason );
    }

    private void addWarning( Artifact artifact, String problem, String reason )
    {
        // TODO: reason could be an i18n key derived from the processor and the problem ID and the
        database.addWarning( artifact, ROLE_HINT, problem, reason );
    }
}
