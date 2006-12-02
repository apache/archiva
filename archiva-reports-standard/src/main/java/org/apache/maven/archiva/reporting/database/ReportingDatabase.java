package org.apache.maven.archiva.reporting.database;

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
import org.apache.maven.archiva.reporting.group.ReportGroup;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    private long startTime;

    private final ReportGroup reportGroup;

    private Set metadataWithProblems;

    private Map filteredDatabases = new HashMap();

    private int numNotices;

    public ReportingDatabase( ReportGroup reportGroup )
    {
        this( reportGroup, new Reporting() );
    }

    public ReportingDatabase( ReportGroup reportGroup, Reporting reporting )
    {
        this( reportGroup, reporting, null );
    }

    public ReportingDatabase( ReportGroup reportGroup, ArtifactRepository repository )
    {
        this( reportGroup, new Reporting(), repository );
    }

    public ReportingDatabase( ReportGroup reportGroup, Reporting reporting, ArtifactRepository repository )
    {
        this.reportGroup = reportGroup;

        this.reporting = reporting;

        this.repository = repository;

        initArtifactMap();

        initMetadataMap();
    }

    public void addFailure( Artifact artifact, String processor, String problem, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        results.addFailure( createResult( processor, problem, reason ) );
        numFailures++;
        updateTimings();

        if ( filteredDatabases.containsKey( problem ) )
        {
            ReportingDatabase reportingDatabase = (ReportingDatabase) filteredDatabases.get( problem );

            reportingDatabase.addFailure( artifact, processor, problem, reason );
        }
    }

    public void addNotice( Artifact artifact, String processor, String problem, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        results.addNotice( createResult( processor, problem, reason ) );
        numNotices++;
        updateTimings();

        if ( filteredDatabases.containsKey( problem ) )
        {
            ReportingDatabase reportingDatabase = (ReportingDatabase) filteredDatabases.get( problem );

            reportingDatabase.addNotice( artifact, processor, problem, reason );
        }
    }

    public void addWarning( Artifact artifact, String processor, String problem, String reason )
    {
        ArtifactResults results = getArtifactResults( artifact );
        results.addWarning( createResult( processor, problem, reason ) );
        numWarnings++;
        updateTimings();

        if ( filteredDatabases.containsKey( problem ) )
        {
            ReportingDatabase reportingDatabase = (ReportingDatabase) filteredDatabases.get( problem );

            reportingDatabase.addWarning( artifact, processor, problem, reason );
        }
    }

    private ArtifactResults getArtifactResults( Artifact artifact )
    {
        return getArtifactResults( artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                                   artifact.getType(), artifact.getClassifier() );
    }

    private ArtifactResults getArtifactResults( String groupId, String artifactId, String version, String type,
                                                String classifier )
    {
        Map artifactMap = this.artifactMap;

        String key = getArtifactKey( groupId, artifactId, version, type, classifier );
        ArtifactResults results = (ArtifactResults) artifactMap.get( key );
        if ( results == null )
        {
            results = new ArtifactResults();
            results.setArtifactId( artifactId );
            results.setClassifier( classifier );
            results.setGroupId( groupId );
            results.setType( type );
            results.setVersion( version );

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
            numNotices += result.getNotices().size();
        }
        artifactMap = map;
    }

    private static String getArtifactKey( String groupId, String artifactId, String version, String type,
                                          String classifier )
    {
        return groupId + ":" + artifactId + ":" + version + ":" + type + ":" + classifier;
    }

    private static Result createResult( String processor, String problem, String reason )
    {
        Result result = new Result();
        result.setProcessor( processor );
        result.setProblem( problem );
        result.setReason( reason );
        return result;
    }

    public void addFailure( RepositoryMetadata metadata, String processor, String problem, String reason )
    {
        MetadataResults results = getMetadataResults( metadata, System.currentTimeMillis() );
        if ( !metadataWithProblems.contains( results ) )
        {
            metadataWithProblems.add( results );
        }
        results.addFailure( createResult( processor, problem, reason ) );
        numFailures++;
        updateTimings();

        if ( filteredDatabases.containsKey( problem ) )
        {
            ReportingDatabase reportingDatabase = (ReportingDatabase) filteredDatabases.get( problem );

            reportingDatabase.addFailure( metadata, processor, problem, reason );
        }
    }

    public void addWarning( RepositoryMetadata metadata, String processor, String problem, String reason )
    {
        MetadataResults results = getMetadataResults( metadata, System.currentTimeMillis() );
        if ( !metadataWithProblems.contains( results ) )
        {
            metadataWithProblems.add( results );
        }
        results.addWarning( createResult( processor, problem, reason ) );
        numWarnings++;
        updateTimings();

        if ( filteredDatabases.containsKey( problem ) )
        {
            ReportingDatabase reportingDatabase = (ReportingDatabase) filteredDatabases.get( problem );

            reportingDatabase.addWarning( metadata, processor, problem, reason );
        }
    }

    public void addNotice( RepositoryMetadata metadata, String processor, String problem, String reason )
    {
        MetadataResults results = getMetadataResults( metadata, System.currentTimeMillis() );
        if ( !metadataWithProblems.contains( results ) )
        {
            metadataWithProblems.add( results );
        }
        results.addNotice( createResult( processor, problem, reason ) );
        numNotices++;
        updateTimings();

        if ( filteredDatabases.containsKey( problem ) )
        {
            ReportingDatabase reportingDatabase = (ReportingDatabase) filteredDatabases.get( problem );

            reportingDatabase.addNotice( metadata, processor, problem, reason );
        }
    }

    public Set getMetadataWithProblems()
    {
        return metadataWithProblems;
    }

    private void initMetadataMap()
    {
        Map map = new HashMap();
        Set problems = new LinkedHashSet();

        for ( Iterator i = reporting.getMetadata().iterator(); i.hasNext(); )
        {
            MetadataResults result = (MetadataResults) i.next();

            String key = getMetadataKey( result.getGroupId(), result.getArtifactId(), result.getVersion() );

            map.put( key, result );

            numFailures += result.getFailures().size();
            numWarnings += result.getWarnings().size();
            numNotices += result.getNotices().size();

            if ( !result.getFailures().isEmpty() || !result.getWarnings().isEmpty() || !result.getNotices().isEmpty() )
            {
                problems.add( result );
            }
        }
        metadataMap = map;
        metadataWithProblems = problems;
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
        String key = getMetadataKey( metadata.getGroupId(), metadata.getArtifactId(), metadata.getBaseVersion() );
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

        numNotices -= results.getWarnings().size();
        results.getNotices().clear();

        metadataWithProblems.remove( results );
    }

    private MetadataResults getMetadataResults( RepositoryMetadata metadata, long lastModified )
    {
        return getMetadataResults( metadata.getGroupId(), metadata.getArtifactId(), metadata.getBaseVersion(),
                                   lastModified );
    }

    private MetadataResults getMetadataResults( String groupId, String artifactId, String baseVersion,
                                                long lastModified )
    {
        String key = getMetadataKey( groupId, artifactId, baseVersion );
        Map metadataMap = this.metadataMap;
        MetadataResults results = (MetadataResults) metadataMap.get( key );
        if ( results == null )
        {
            results = new MetadataResults();
            results.setArtifactId( artifactId );
            results.setGroupId( groupId );
            results.setVersion( baseVersion );
            results.setLastModified( lastModified );

            metadataMap.put( key, results );
            reporting.getMetadata().add( results );
        }
        return results;
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
            numNotices -= results.getNotices().size();

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

        if ( inProgress )
        {
            startTime = System.currentTimeMillis();
        }
    }

    public void clear()
    {
        // clear the values rather than destroy the instance so that the "inProgress" indicator is in tact.
        numWarnings = 0;
        numNotices = 0;
        numFailures = 0;

        artifactMap.clear();
        metadataMap.clear();
        metadataWithProblems.clear();
        filteredDatabases.clear();

        reporting.getArtifacts().clear();
        reporting.getMetadata().clear();

        updateTimings();
    }

    public void setStartTime( long startTime )
    {
        this.startTime = startTime;
    }

    public long getStartTime()
    {
        return startTime;
    }

    public void updateTimings()
    {
        long startTime = getStartTime();
        Date endTime = new Date();
        if ( startTime > 0 )
        {
            getReporting().setExecutionTime( endTime.getTime() - startTime );
        }
        getReporting().setLastModified( endTime.getTime() );
    }

    public ReportGroup getReportGroup()
    {
        return reportGroup;
    }

    public ReportingDatabase getFilteredDatabase( String filter )
    {
        ReportingDatabase reportingDatabase = (ReportingDatabase) filteredDatabases.get( filter );

        if ( reportingDatabase == null )
        {
            reportingDatabase = new ReportingDatabase( reportGroup, repository );

            Reporting reporting = reportingDatabase.getReporting();
            reporting.setExecutionTime( this.reporting.getExecutionTime() );
            reporting.setLastModified( this.reporting.getLastModified() );

            for ( Iterator i = this.reporting.getArtifacts().iterator(); i.hasNext(); )
            {
                ArtifactResults results = (ArtifactResults) i.next();
                ArtifactResults targetResults = null;
                for ( Iterator j = results.getFailures().iterator(); j.hasNext(); )
                {
                    Result result = (Result) j.next();

                    if ( filter.equals( result.getProcessor() ) )
                    {
                        if ( targetResults == null )
                        {
                            // lazily create so it is not added unless it has to be
                            targetResults = createArtifactResults( reportingDatabase, results );
                        }

                        targetResults.addFailure( result );
                        reportingDatabase.numFailures++;
                    }
                }
                for ( Iterator j = results.getWarnings().iterator(); j.hasNext(); )
                {
                    Result result = (Result) j.next();

                    if ( filter.equals( result.getProcessor() ) )
                    {
                        if ( targetResults == null )
                        {
                            // lazily create so it is not added unless it has to be
                            targetResults = createArtifactResults( reportingDatabase, results );
                        }

                        targetResults.addWarning( result );
                        reportingDatabase.numWarnings++;
                    }
                }
                for ( Iterator j = results.getNotices().iterator(); j.hasNext(); )
                {
                    Result result = (Result) j.next();

                    if ( filter.equals( result.getProcessor() ) )
                    {
                        if ( targetResults == null )
                        {
                            // lazily create so it is not added unless it has to be
                            targetResults = createArtifactResults( reportingDatabase, results );
                        }

                        targetResults.addNotice( result );
                        reportingDatabase.numNotices++;
                    }
                }
            }
            for ( Iterator i = this.reporting.getMetadata().iterator(); i.hasNext(); )
            {
                MetadataResults results = (MetadataResults) i.next();
                MetadataResults targetResults = null;
                for ( Iterator j = results.getFailures().iterator(); j.hasNext(); )
                {
                    Result result = (Result) j.next();

                    if ( filter.equals( result.getProcessor() ) )
                    {
                        if ( targetResults == null )
                        {
                            // lazily create so it is not added unless it has to be
                            targetResults = createMetadataResults( reportingDatabase, results );
                        }

                        targetResults.addFailure( result );
                        reportingDatabase.numFailures++;
                    }
                }
                for ( Iterator j = results.getWarnings().iterator(); j.hasNext(); )
                {
                    Result result = (Result) j.next();

                    if ( filter.equals( result.getProcessor() ) )
                    {
                        if ( targetResults == null )
                        {
                            // lazily create so it is not added unless it has to be
                            targetResults = createMetadataResults( reportingDatabase, results );
                        }

                        targetResults.addWarning( result );
                        reportingDatabase.numWarnings++;
                    }
                }
                for ( Iterator j = results.getNotices().iterator(); j.hasNext(); )
                {
                    Result result = (Result) j.next();

                    if ( filter.equals( result.getProcessor() ) )
                    {
                        if ( targetResults == null )
                        {
                            // lazily create so it is not added unless it has to be
                            targetResults = createMetadataResults( reportingDatabase, results );
                        }

                        targetResults.addNotice( result );
                        reportingDatabase.numNotices++;
                    }
                }
            }

            filteredDatabases.put( filter, reportingDatabase );
        }

        return reportingDatabase;
    }

    private static MetadataResults createMetadataResults( ReportingDatabase reportingDatabase, MetadataResults results )
    {
        MetadataResults targetResults = reportingDatabase.getMetadataResults( results.getGroupId(),
                                                                              results.getArtifactId(),
                                                                              results.getVersion(),
                                                                              results.getLastModified() );
        reportingDatabase.metadataWithProblems.add( targetResults );
        return targetResults;
    }

    private static ArtifactResults createArtifactResults( ReportingDatabase reportingDatabase, ArtifactResults results )
    {
        return reportingDatabase.getArtifactResults( results.getGroupId(), results.getArtifactId(),
                                                     results.getVersion(), results.getType(), results.getClassifier() );
    }

    public int getNumNotices()
    {
        return numNotices;
    }
}
