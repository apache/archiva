package org.apache.maven.archiva.scheduler.task;

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

/**
 * Task for discovering changes in the repository and updating the index accordingly.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class IndexerTask
    implements RepositoryTask
{
    private String jobName;

    private String policy;

    public long getMaxExecutionTime()
    {
        return 0;
    }

    public String getJobName()
    {
        return jobName;
    }

    public String getQueuePolicy()
    {
        return policy;
    }

    public void setQueuePolicy( String policy )
    {
        this.policy = policy;
    }

    public void setJobName( String jobName )
    {
        this.jobName = jobName;
    }


}
