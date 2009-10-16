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

import java.util.Collections;
import java.util.List;

import com.opensymphony.xwork2.Preparable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.DatabaseScanningConfiguration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.database.updater.DatabaseConsumers;
import org.apache.maven.archiva.repository.audit.AuditEvent;
import org.apache.maven.archiva.repository.audit.Auditable;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

/**
 * DatabaseAction
 *
 * @version $Id$
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="databaseAction" instantiation-strategy="per-lookup"
 */
public class DatabaseAction
    extends PlexusActionSupport
    implements Preparable, SecureAction, Auditable
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
    private List<AdminDatabaseConsumer> unprocessedConsumers;

    /**
     * List of enabled {@link AdminDatabaseConsumer} objects for unprocessed artifacts.
     */
    private List<String> enabledUnprocessedConsumers;

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
        Collections.sort( this.unprocessedConsumers, AdminDatabaseConsumerComparator.getInstance() );
    }

    public String updateUnprocessedConsumers()
    {
        List<String> oldConsumers = archivaConfiguration.getConfiguration().getDatabaseScanning().getUnprocessedConsumers();
        
        archivaConfiguration.getConfiguration().getDatabaseScanning().setUnprocessedConsumers(
            enabledUnprocessedConsumers );
        
        if ( enabledUnprocessedConsumers != null )
        {
            filterAddedConsumers( oldConsumers, enabledUnprocessedConsumers );
            filterRemovedConsumers( oldConsumers, enabledUnprocessedConsumers );    
        }
        else
        {
            disableAllEnabledConsumers( oldConsumers );
        }

        return saveConfiguration();
    }

    public String updateSchedule()
    {
        String oldCron = archivaConfiguration.getConfiguration().getDatabaseScanning().getCronExpression();
        
        archivaConfiguration.getConfiguration().getDatabaseScanning().setCronExpression( cron );
        
        if ( !oldCron.equals( cron ) )
        {
            triggerAuditEvent( AuditEvent.DB_SCHEDULE + " " + cron );
        }

        return saveConfiguration();
    }

    private String saveConfiguration()
    {
        try
        {
            archivaConfiguration.save( archivaConfiguration.getConfiguration() );
            addActionMessage( "Successfully saved configuration" );
        }
        catch ( RegistryException e )
        {
            log.error( e.getMessage(), e );
            addActionError( "Error in saving configuration" );
            return INPUT;
        }
        catch ( IndeterminateConfigurationException e )
        {
            addActionError( e.getMessage() );
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

    public List getUnprocessedConsumers()
    {
        return unprocessedConsumers;
    }

    public List<String> getEnabledUnprocessedConsumers()
    {
        return enabledUnprocessedConsumers;
    }

    public void setEnabledUnprocessedConsumers( List<String> enabledUnprocessedConsumers )
    {
        this.enabledUnprocessedConsumers = enabledUnprocessedConsumers;
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }

    private void filterAddedConsumers( List<String> oldList, List<String> newList )
    {
        for ( String consumer : newList )
        {
            if ( !oldList.contains( consumer ) )
            {
                triggerAuditEvent( consumer, AuditEvent.ENABLE_DB_CONSUMER );
            }
        }
    }

    private void filterRemovedConsumers( List<String> oldList, List<String> newList )
    {
        for ( String consumer : oldList )
        {
            if ( !newList.contains( consumer ) )
            {
                triggerAuditEvent( consumer, AuditEvent.DISABLE_DB_CONSUMER );
            }
        }
    }

    private void disableAllEnabledConsumers( List<String> enabledConsumers )
    {
        for( String consumer : enabledConsumers )
        {
            triggerAuditEvent( consumer, AuditEvent.DISABLE_DB_CONSUMER );
        }
    }
}
