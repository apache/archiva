package org.apache.maven.archiva.reporting;

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

import org.apache.maven.archiva.reporting.model.ArtifactResults;
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.archiva.reporting.model.Reporting;
import org.apache.maven.archiva.reporting.model.Result;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @todo i18n, including message formatting and parameterisation
 */
public class ReportingDatabase
{
    private final Reporting reporting;

    private Map artifactMap;

    private Map metadataMap;

    private int numFailures;

    private int numWarnings;

    private ArtifactRepository repository;

    private boolean inProgress;

    public ReportingDatabase()
    {
        this( new Reporting(), null );
    }

    public ReportingDatabase( Reporting reporting )
    {
        this( reporting, null );
    }

    public ReportingDatabase( ArtifactRepository repository )
    {
        this( new Reporting(), repository );
    }

    public ReportingDatabase( Reporting reporting, ArtifactRepository repository )
    {
        this.reporting = reporting;

        this.repository = repository;

        initArtifactMap();

        initMetadataMap();
    }

    public void addFailure( Artifact artifact, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        results.addFailure( createResults( reason ) );
        numFailures++;
    }

    public void addWarning( Artifact artifact, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        results.addWarning( createResults( reason ) );
        numWarnings++;
    }

    private ArtifactResults getArtifactResults( Artifact artifact )
    {
        Map artifactMap = this.artifactMap;

        String key = getArtifactKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                     artifact.getType(), artifact.getClassifier() );
        ArtifactResults results = (ArtifactResults) artifactMap.get( key );
        if ( results == null )
        {
            results = new ArtifactResults();
            results.setArtifactId( artifact.getArtifactId() );
            results.setClassifier( artifact.getClassifier() );
            results.setGroupId( artifact.getGroupId() );
            results.setType( artifact.getType() );
            results.setVersion( artifact.getVersion() );

            artifactMap.put( key, results );
            reporting.getArtifacts().add( results );
        }

        return results;
    }

    private void initArtifactMap()
    {
        Map map = new HashMap();
        for ( Iterator i = reporting.getArtifacts().iterator(); i.hasNext(); )
        {
            ArtifactResults result = (ArtifactResults) i.next();

            String key = getArtifactKey( result.getGroupId(), result.getArtifactId(), result.getVersion(),
                                         result.getType(), result.getClassifier() );
            map.put( key, result );

            numFailures += result.getFailures().size();
            numWarnings += result.getWarnings().size();
        }
        artifactMap = map;
    }

    private static String getArtifactKey( String groupId, String artifactId, String version, String type,
                                          String classifier )
    {
        return groupId + ":" + artifactId + ":" + version + ":" + type + ":" + classifier;
    }

    private static Result createResults( String reason )
    {
        Result result = new Result();
        result.setReason( reason );
        return result;
    }

    public void addFailure( RepositoryMetadata metadata, String reason )
    {
        MetadataResults results = getMetadataResults( metadata, System.currentTimeMillis() );
        results.addFailure( createResults( reason ) );
        numFailures++;
    }

    public void addWarning( RepositoryMetadata metadata, String reason )
    {
        MetadataResults results = getMetadataResults( metadata, System.currentTimeMillis() );
        results.addWarning( createResults( reason ) );
        numWarnings++;
    }

    private void initMetadataMap()
    {
        Map map = new HashMap();
        for ( Iterator i = reporting.getMetadata().iterator(); i.hasNext(); )
        {
            MetadataResults result = (MetadataResults) i.next();

            String key = getMetadataKey( result.getGroupId(), result.getArtifactId(), result.getVersion() );

            map.put( key, result );

            numFailures += result.getFailures().size();
            numWarnings += result.getWarnings().size();
        }
        metadataMap = map;
    }

    private static String getMetadataKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    public int getNumFailures()
    {
        return numFailures;
    }

    public int getNumWarnings()
    {
        return numWarnings;
    }

    public Reporting getReporting()
    {
        return reporting;
    }

    public Iterator getArtifactIterator()
    {
        return reporting.getArtifacts().iterator();
    }

    public Iterator getMetadataIterator()
    {
        return reporting.getMetadata().iterator();
    }

    public boolean isMetadataUpToDate( RepositoryMetadata metadata, long timestamp )
    {
        String key = getMetadataKey( metadata );
        Map map = metadataMap;
        MetadataResults results = (MetadataResults) map.get( key );
        return results != null && results.getLastModified() >= timestamp;
    }

    /**
     * Make sure the metadata record exists, but remove any previous reports in preparation for adding new ones.
     *
     * @param metadata     the metadata
     * @param lastModified the modification time of the file being tracked
     */
    public void cleanMetadata( RepositoryMetadata metadata, long lastModified )
    {
        MetadataResults results = getMetadataResults( metadata, lastModified );

        results.setLastModified( lastModified );

        numFailures -= results.getFailures().size();
        results.getFailures().clear();

        numWarnings -= results.getWarnings().size();
        results.getWarnings().clear();
    }

    private MetadataResults getMetadataResults( RepositoryMetadata metadata, long lastModified )
    {
        String key = getMetadataKey( metadata );
        Map metadataMap = this.metadataMap;
        MetadataResults results = (MetadataResults) metadataMap.get( key );
        if ( results == null )
        {
            results = new MetadataResults();
            results.setArtifactId( metadata.getArtifactId() );
            results.setGroupId( metadata.getGroupId() );
            results.setVersion( metadata.getBaseVersion() );
            results.setLastModified( lastModified );

            metadataMap.put( key, results );
            reporting.getMetadata().add( results );
        }
        return results;
    }

    private static String getMetadataKey( RepositoryMetadata metadata )
    {
        return getMetadataKey( metadata.getGroupId(), metadata.getArtifactId(), metadata.getBaseVersion() );
    }

    public void removeArtifact( Artifact artifact )
    {
        Map map = artifactMap;

        String key = getArtifactKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                     artifact.getType(), artifact.getClassifier() );
        ArtifactResults results = (ArtifactResults) map.get( key );
        if ( results != null )
        {
            for ( Iterator i = reporting.getArtifacts().iterator(); i.hasNext(); )
            {
                if ( results.equals( i.next() ) )
                {
                    i.remove();
                }
            }

            numFailures -= results.getFailures().size();
            numWarnings -= results.getWarnings().size();

            map.remove( key );
        }
    }

    public ArtifactRepository getRepository()
    {
        return repository;
    }

    public boolean isInProgress()
    {
        return inProgress;
    }

    public void setInProgress( boolean inProgress )
    {
        this.inProgress = inProgress;
    }

    public void clear()
    {
        // clear the values rather than destroy the instance so that the "inProgress" indicator is in tact.
        numWarnings = 0;
        numFailures = 0;

        artifactMap.clear();
        metadataMap.clear();

        reporting.getArtifacts().clear();
        reporting.getMetadata().clear();
    }
}
