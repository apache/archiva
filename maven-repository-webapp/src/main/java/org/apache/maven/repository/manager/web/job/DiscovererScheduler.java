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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.DefaultArtifactRepositoryFactory;
import org.apache.maven.repository.discovery.ArtifactDiscoverer;
import org.apache.maven.repository.discovery.MetadataDiscoverer;
import org.apache.maven.repository.indexing.RepositoryIndexingFactory;
import org.codehaus.plexus.scheduler.Scheduler;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Properties;

/**
 * This class sets the job to be executed in the plexus-quartz scheduler
 *
 * @plexus.component role="org.apache.maven.repository.manager.web.job.DiscovererScheduler"
 */
public class DiscovererScheduler
{
    /**
     * @plexus.requirement
     */
    private Configuration config;

    /**
     * @plexus.requirement
     */
    private Scheduler scheduler;

    /**
     * @plexus.requirement role="org.apache.maven.repository.discovery.ArtifactDiscoverer" role-hint="org.apache.maven.repository.discovery.DefaultArtifactDiscoverer"
     */
    private ArtifactDiscoverer defaultArtifactDiscoverer;

    /**
     * @plexus.requirement role="org.apache.maven.repository.discovery.ArtifactDiscoverer" role-hint="org.apache.maven.repository.discovery.LegacyArtifactDiscoverer"
     */
    private ArtifactDiscoverer legacyArtifactDiscoverer;

    /**
     * @plexus.requirement role="org.apache.maven.repository.discovery.MetadataDiscoverer" role-hint="org.apache.maven.repository.discovery.DefaultMetadataDiscoverer"
     */
    private MetadataDiscoverer defaultMetadataDiscoverer;

    /**
     * @plexus.requirement
     */
    private RepositoryIndexingFactory indexFactory;

    /**
     * @plexus.requirement
     */
    private ArtifactRepositoryFactory repoFactory;

    private Properties props;

    /**
     * Method that sets the schedule in the plexus-quartz scheduler
     *
     * @throws IOException
     * @throws ParseException
     * @throws SchedulerException
     */
    public void setSchedule()
        throws IOException, ParseException, SchedulerException
    {
        props = config.getProperties();
        JobDetail jobDetail = new JobDetail( "discovererJob", "DISCOVERER", DiscovererJob.class );
        JobDataMap dataMap = new JobDataMap();
        dataMap.put( DiscovererJob.MAP_INDEXPATH, props.getProperty( "index.path" ) );
        dataMap.put( DiscovererJob.MAP_BLACKLIST, props.getProperty( "blacklist.patterns" ) );
        dataMap.put( DiscovererJob.MAP_DEFAULT_REPOSITORY, getDefaultRepository() );
        dataMap.put( DiscovererJob.MAP_LAYOUT, props.getProperty( "layout" ) );
        dataMap.put( DiscovererJob.MAP_SNAPSHOTS, new Boolean( props.getProperty( "include.snapshots" ) ) );
        dataMap.put( DiscovererJob.MAP_CONVERT, new Boolean( props.getProperty( "convert.snapshots" ) ) );
        dataMap.put( DiscovererJob.MAP_DEF_ARTIFACT_DISCOVERER, defaultArtifactDiscoverer );
        dataMap.put( DiscovererJob.MAP_LEG_ARTIFACT_DISCOVERER, legacyArtifactDiscoverer );
        dataMap.put( DiscovererJob.MAP_DEF_METADATA_DISCOVERER, defaultMetadataDiscoverer );
        dataMap.put( DiscovererJob.MAP_IDX_FACTORY, indexFactory );
        dataMap.put( DiscovererJob.MAP_REPO_LAYOUT, config.getLayout() );
        dataMap.put( DiscovererJob.MAP_REPO_FACTORY, repoFactory );
        jobDetail.setJobDataMap( dataMap );

        CronTrigger trigger =
            new CronTrigger( "DiscovererTrigger", "DISCOVERER", props.getProperty( "cron.expression" ) );
        scheduler.scheduleJob( jobDetail, trigger );
    }

    /**
     * Method that creates the artifact repository
     *
     * @return an ArtifactRepository instance
     * @throws java.net.MalformedURLException
     */
    private ArtifactRepository getDefaultRepository()
        throws MalformedURLException
    {
        File repositoryDirectory = new File( config.getRepositoryDirectory() );
        String repoDir = repositoryDirectory.toURL().toString();
        ArtifactRepositoryFactory repoFactory = new DefaultArtifactRepositoryFactory();

        return repoFactory.createArtifactRepository( "test", repoDir, config.getLayout(), null, null );
    }

    /**
     * Method that sets the configuration object
     *
     * @param config
     */
    public void setConfiguration( Configuration config )
    {
        this.config = config;
    }

    /**
     * Returns the cofiguration
     *
     * @return a Configuration object that contains the configuration values
     */
    public Configuration getConfiguration()
    {
        return config;
    }

}
