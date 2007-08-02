package org.apache.maven.archiva.web.action.reports;

import org.apache.maven.archiva.database.constraints.RangeConstraint;
import org.apache.maven.archiva.database.constraints.RepositoryProblemByGroupIdConstraint;

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
 * By group id reporting.
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="reportsByGroupIdAction"
 */
public class ReportsByGroupIdAction extends AbstractReportAction
{
    private String groupId;

    protected void configureConstraint()
    {
        this.constraint = new RepositoryProblemByGroupIdConstraint( range, groupId );
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }
}
