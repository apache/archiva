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

    private int totalFailures;

    private int totalWarnings;

    public ReportingDatabase()
    {
        reporting = new Reporting();
    }

    public ReportingDatabase( Reporting reporting )
    {
        this.reporting = reporting;
    }

    public void addFailure( Artifact artifact, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        results.addFailure( createResults( reason ) );
        totalFailures++;
    }

    public void addWarning( Artifact artifact, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        results.addWarning( createResults( reason ) );
        totalWarnings++;
    }

    private ArtifactResults getArtifactResults( Artifact artifact )
    {
        Map artifactMap = getArtifactMap();

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

    private Map getArtifactMap()
    {
        if ( artifactMap == null )
        {
            Map map = new HashMap();
            for ( Iterator i = reporting.getArtifacts().iterator(); i.hasNext(); )
            {
                ArtifactResults result = (ArtifactResults) i.next();

                String key = getArtifactKey( result.getGroupId(), result.getArtifactId(), result.getVersion(),
                                             result.getType(), result.getClassifier() );
                map.put( key, result );

                totalFailures += result.getFailures().size();
                totalWarnings += result.getWarnings().size();
            }
            artifactMap = map;
        }
        return artifactMap;
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
        MetadataResults results = getMetadataResults( metadata );
        results.addFailure( createResults( reason ) );
        totalFailures++;
    }

    public void addWarning( RepositoryMetadata metadata, String reason )
    {
        MetadataResults results = getMetadataResults( metadata );
        results.addWarning( createResults( reason ) );
        totalWarnings++;
    }

    private MetadataResults getMetadataResults( RepositoryMetadata metadata )
    {
        Map metadataMap = getMetadataMap();

        String key = getMetadataKey( metadata.getGroupId(), metadata.getArtifactId(), metadata.getBaseVersion() );

        MetadataResults results = (MetadataResults) metadataMap.get( key );
        if ( results == null )
        {
            results = new MetadataResults();
            results.setArtifactId( metadata.getArtifactId() );
            results.setGroupId( metadata.getGroupId() );
            results.setVersion( metadata.getBaseVersion() );

            metadataMap.put( key, results );
            reporting.getMetadata().add( results );
        }

        return results;
    }

    private Map getMetadataMap()
    {
        if ( metadataMap == null )
        {
            Map map = new HashMap();
            for ( Iterator i = reporting.getMetadata().iterator(); i.hasNext(); )
            {
                MetadataResults result = (MetadataResults) i.next();

                String key = getMetadataKey( result.getGroupId(), result.getArtifactId(), result.getVersion() );

                map.put( key, result );

                totalFailures += result.getFailures().size();
                totalWarnings += result.getWarnings().size();
            }
            metadataMap = map;
        }
        return metadataMap;
    }

    private static String getMetadataKey( String groupId, String artifactId, String version )
    {
        return groupId + ":" + artifactId + ":" + version;
    }

    public int getNumFailures()
    {
        return totalFailures;
    }

    public int getNumWarnings()
    {
        return totalWarnings;
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
}
