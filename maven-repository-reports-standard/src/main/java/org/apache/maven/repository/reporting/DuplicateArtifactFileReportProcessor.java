package org.apache.maven.repository.reporting;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Model;
import org.apache.maven.repository.digest.Digester;
import org.apache.maven.repository.digest.DigesterException;
import org.apache.maven.repository.indexing.RepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchException;
import org.apache.maven.repository.indexing.RepositoryIndexSearchLayer;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.apache.maven.repository.indexing.SearchResult;
import org.apache.maven.repository.indexing.query.SingleTermQuery;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * Validates an artifact file for duplicates within the same groupId based from what's available in a RepositoryIndex
 *
 * @author Edwin Punzalan
 * @plexus.component role="org.apache.maven.repository.reporting.ArtifactReportProcessor" role-hint="duplicate"
 */
public class DuplicateArtifactFileReportProcessor
    implements ArtifactReportProcessor
{
    /**
     * @plexus.requirement
     */
    private Digester digester;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexingFactory indexFactory;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexSearchLayer searchLayer;

    /**
     * @plexus.configuration
     */
    private String indexDirectory;

    //@todo configurable?
    private String algorithm = RepositoryIndex.FLD_MD5;

    public void processArtifact( Model model, Artifact artifact, ArtifactReporter reporter,
                                 ArtifactRepository repository )
        throws ReportProcessorException
    {
        if ( artifact.getFile() != null )
        {
            RepositoryIndex index;
            try
            {
                index = indexFactory.createArtifactRepositoryIndex( new File( indexDirectory ), repository );
            }
            catch ( RepositoryIndexException e )
            {
                throw new ReportProcessorException( "Unable to create RepositoryIndex instance", e );
            }

            String checksum;
            try
            {
                checksum = digester.createChecksum( artifact.getFile(), algorithm );
            }
            catch ( DigesterException e )
            {
                throw new ReportProcessorException( "Failed to generate checksum", e );
            }

            try
            {
                List results = searchLayer.searchAdvanced( new SingleTermQuery( algorithm, checksum.trim() ), index );

                if ( results.isEmpty() )
                {
                    reporter.addSuccess( artifact );
                }
                else
                {
                    String id = artifact.getId();

                    boolean hasDuplicates = false;
                    for ( Iterator hits = results.iterator(); hits.hasNext(); )
                    {
                        SearchResult result = (SearchResult) hits.next();
                        Artifact artifactMatch = result.getArtifact();

                        //make sure it is not the same artifact
                        if ( !id.equals( artifactMatch.getId() ) )
                        {
                            //report only duplicates from the same groupId
                            String groupId = artifact.getGroupId();
                            if ( groupId.equals( artifactMatch.getGroupId() ) )
                            {
                                hasDuplicates = true;
                                reporter.addFailure( artifact, "Found duplicate for " + artifactMatch.getId() );
                            }
                        }
                    }

                    if ( !hasDuplicates )
                    {
                        reporter.addSuccess( artifact );
                    }
                }
            }
            catch ( RepositoryIndexSearchException e )
            {
                throw new ReportProcessorException( "Failed to search in index", e );
            }
        }
        else
        {
            reporter.addWarning( artifact, "Artifact file is null" );
        }
    }
}
