package org.apache.maven.archiva.web.action.reports;

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

import com.opensymphony.xwork.Preparable;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.constraints.UniqueFieldConstraint;
import org.apache.maven.archiva.model.RepositoryProblem;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.ArrayList;
import java.util.Collection;

/**
 * PickReportAction 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="pickReport"
 */
public class PickReportAction
    extends PlexusActionSupport
    implements Preparable
{
    /**
     * @plexus.requirement role-hint="jdo"
     */
    protected ArchivaDAO dao;

    private Collection<String> repositoryIds = new ArrayList<String>();

    public static final String ALL_REPOSITORIES = "All Repositories";

    public void prepare()
    {
        repositoryIds.add( ALL_REPOSITORIES );
        repositoryIds.addAll( dao
            .query( new UniqueFieldConstraint( RepositoryProblem.class.getName(), "repositoryId" ) ) );
    }
    
    public String input()
        throws Exception
    {
        return INPUT;
    }

    public Collection<String> getRepositoryIds()
    {
        return repositoryIds;
    }
}
