package org.apache.maven.repository.manager.web.job;

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
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.model.Model;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.discovery.DefaultArtifactDiscoverer;
import org.apache.maven.repository.discovery.DefaultMetadataDiscoverer;
import org.apache.maven.repository.discovery.LegacyArtifactDiscoverer;
import org.apache.maven.repository.discovery.MetadataDiscoverer;
import org.apache.maven.repository.indexing.ArtifactRepositoryIndex;
import org.apache.maven.repository.indexing.MetadataRepositoryIndex;
import org.apache.maven.repository.indexing.PomRepositoryIndex;
import org.apache.maven.repository.indexing.RepositoryIndexException;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.apache.maven.repository.manager.web.execution.DiscovererExecution;
import org.codehaus.plexus.scheduler.AbstractJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the discoverer job that is executed by the scheduler.
 *
 * @plexus.component role="org.apache.maven.repository.manager.web.job.DiscovererJob"
 */
public class DiscovererJob
    extends AbstractJob
{
    public static final String ROLE = DiscovererJob.class.getName();

    public static String MAP_DISCOVERER_EXECUTION = "EXECUTION";

    /**
     * Execute the discoverer and the indexer.
     *
     * @param context
     * @throws org.quartz.JobExecutionException
     *
     */
    public void execute( JobExecutionContext context )
        throws JobExecutionException
    {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        setJobDataMap( dataMap );
        getLogger().info( "[DiscovererJob] Start execution of DiscovererJob.." );

        try
        {
            DiscovererExecution execution = (DiscovererExecution) dataMap.get( MAP_DISCOVERER_EXECUTION );
            execution.executeDiscoverer();
        }
        catch ( RepositoryIndexException e )
        {
            e.printStackTrace();
        }
        catch ( MalformedURLException me )
        {
            me.printStackTrace();
        }

        getLogger().info( "[DiscovererJob] DiscovererJob has finished executing." );
    }

}
