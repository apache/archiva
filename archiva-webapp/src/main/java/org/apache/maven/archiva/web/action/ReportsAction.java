package org.apache.maven.archiva.web.action;

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

import com.opensymphony.xwork.ActionSupport;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.reporting.ReportingDatabase;
import org.apache.maven.archiva.reporting.ReportingStore;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Repository reporting.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="reportsAction"
 */
public class ReportsAction
    extends ActionSupport
{
    /**
     * @plexus.requirement
     */
    private ReportingStore reportingStore;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory factory;

    private List databases;

    public String execute()
        throws Exception
    {
        databases = new ArrayList();

        Configuration configuration = configurationStore.getConfigurationFromStore();

        for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
        {
            RepositoryConfiguration repositoryConfiguration = (RepositoryConfiguration) i.next();

            ArtifactRepository repository = factory.createRepository( repositoryConfiguration );

            ReportingDatabase database = reportingStore.getReportsFromStore( repository );

            databases.add( database );
        }
        return SUCCESS;
    }

    public List getDatabases()
    {
        return databases;
    }
}
