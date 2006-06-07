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

import org.apache.maven.repository.manager.web.execution.DiscovererExecution;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.scheduler.AbstractJob;
import org.codehaus.plexus.scheduler.Scheduler;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import java.text.ParseException;
import java.util.Properties;

/**
 * This class sets the job to be executed in the plexus-quartz scheduler
 *
 * @plexus.component role="org.apache.maven.repository.manager.web.job.DiscovererScheduler"
 */
public class DiscovererScheduler
    extends AbstractLogEnabled
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
     * @plexus.requirement
     */
    private DiscovererExecution execution;

    /**
     * Method that sets the schedule in the plexus-quartz scheduler
     *
     * @throws ParseException
     * @throws SchedulerException
     */
    public void setSchedule()
        throws ParseException, SchedulerException
    {
        Properties props = config.getProperties();
        JobDetail jobDetail = new JobDetail( "discovererJob", "DISCOVERER", DiscovererJob.class );
        JobDataMap dataMap = new JobDataMap();
        dataMap.put( AbstractJob.LOGGER, getLogger() );
        dataMap.put( DiscovererJob.MAP_DISCOVERER_EXECUTION, execution );
        jobDetail.setJobDataMap( dataMap );

        CronTrigger trigger =
            new CronTrigger( "DiscovererTrigger", "DISCOVERER", props.getProperty( "cron.expression" ) );
        scheduler.scheduleJob( jobDetail, trigger );
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
