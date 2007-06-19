package org.apache.maven.archiva.web.action.admin.database;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.opensymphony.xwork.Preparable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.apache.maven.archiva.database.updater.DatabaseConsumers;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.admin.scanning.AdminRepositoryConsumerComparator;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.redback.xwork.interceptor.SecureAction;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionBundle;
import org.codehaus.plexus.redback.xwork.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.Collections;
import java.util.List;

/**
 * DatabaseAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="databaseAction"
 */
public class DatabaseAction
    extends PlexusActionSupport
    implements Preparable, SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement
     */
    private DatabaseConsumers databaseConsumers;

    private String cron;

    /**
     * List of available {@link AdminDatabaseConsumer} objects for unprocessed artifacts.
     */
    private List unprocessedConsumers;
    
    /**
     * List of enabled {@link AdminDatabaseConsumer} objects for unprocessed artifacts.
     */
    private List enabledUnprocessedConsumers;
    
    /**
     * List of {@link AdminDatabaseConsumer} objects for "to cleanup" artifacts.
     */
    private List cleanupConsumers;
    
    /**
     * List of enabled {@link AdminDatabaseConsumer} objects for "to cleanup" artifacts.
     */
    private List enabledCleanupConsumers;

    public void prepare()
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();
        DatabaseScanningConfiguration dbscanning = config.getDatabaseScanning();

        this.cron = dbscanning.getCronExpression();

        AddAdminDatabaseConsumerClosure addAdminDbConsumer;
        
        addAdminDbConsumer = new AddAdminDatabaseConsumerClosure( dbscanning.getUnprocessedConsumers() );
        CollectionUtils.forAllDo( databaseConsumers.getAvailableUnprocessedConsumers(), addAdminDbConsumer );
        this.unprocessedConsumers = addAdminDbConsumer.getList();
        Collections.sort( this.unprocessedConsumers, AdminRepositoryConsumerComparator.getInstance() );

        addAdminDbConsumer = new AddAdminDatabaseConsumerClosure( dbscanning.getCleanupConsumers() );
        CollectionUtils.forAllDo( databaseConsumers.getAvailableCleanupConsumers(), addAdminDbConsumer );
        this.cleanupConsumers = addAdminDbConsumer.getList();
        Collections.sort( this.cleanupConsumers, AdminRepositoryConsumerComparator.getInstance() );
    }

    public String updateUnprocessedConsumers()
    {
        archivaConfiguration.getConfiguration().getDatabaseScanning().setUnprocessedConsumers( enabledUnprocessedConsumers );
        
        return saveConfiguration();
    }
    
    public String updateCleanupConsumers()
    {
        archivaConfiguration.getConfiguration().getDatabaseScanning().setCleanupConsumers( enabledCleanupConsumers );
        
        return saveConfiguration();
    }
    
    public String updateSchedule()
    {
        archivaConfiguration.getConfiguration().getDatabaseScanning().setCronExpression( cron );
        
        return saveConfiguration();
    }
    
    private String saveConfiguration()
    {
        try
        {
            archivaConfiguration.save( archivaConfiguration.getConfiguration() );
            addActionMessage( "Successfully saved configuration" );
        }
        catch ( Exception e)
        {
            addActionError( "Error in saving configuration" );
            return INPUT;
        }
    
        return SUCCESS;
    }
    
    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public String getCron()
    {
        return cron;
    }

    public void setCron( String cron )
    {
        this.cron = cron;
    }

    public List getCleanupConsumers()
    {
        return cleanupConsumers;
    }

    public List getUnprocessedConsumers()
    {
        return unprocessedConsumers;
    }

    public List getEnabledUnprocessedConsumers()
    {
        return enabledUnprocessedConsumers;
    }

    public void setEnabledUnprocessedConsumers( List enabledUnprocessedConsumers )
    {
        this.enabledUnprocessedConsumers = enabledUnprocessedConsumers;
    }

    public List getEnabledCleanupConsumers()
    {
        return enabledCleanupConsumers;
    }

    public void setEnabledCleanupConsumers( List enabledCleanupConsumers )
    {
        this.enabledCleanupConsumers = enabledCleanupConsumers;
    }
}
