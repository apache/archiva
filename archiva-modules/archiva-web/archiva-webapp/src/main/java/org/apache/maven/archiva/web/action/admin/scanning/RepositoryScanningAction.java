package org.apache.maven.archiva.web.action.admin.scanning;

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

import com.opensymphony.xwork2.Preparable;
import com.opensymphony.xwork2.Validateable;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.FileType;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.RepositoryScanningConfiguration;
import org.apache.maven.archiva.configuration.functors.FiletypeSelectionPredicate;
import org.apache.maven.archiva.configuration.functors.FiletypeToMapClosure;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
import org.codehaus.plexus.registry.RegistryException;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RepositoryScanningAction
 *
 * @version $Id$
 *          plexus.component role="com.opensymphony.xwork2.Action" role-hint="repositoryScanningAction" instantiation-strategy="per-lookup"
 */
@Controller( "repositoryScanningAction" )
@Scope( "prototype" )
public class RepositoryScanningAction
    extends AbstractActionSupport
    implements Preparable, Validateable, SecureAction, Auditable
{
    /**
     * plexus.requirement
     */
    @Inject
    private ArchivaConfiguration archivaConfiguration;

    /**
     * plexus.requirement
     */
    @Inject
    private RepositoryContentConsumers repoconsumerUtil;

    private Map<String, FileType> fileTypeMap;

    private List<String> fileTypeIds;

    /**
     * List of {@link AdminRepositoryConsumer} objects for consumers of known content.
     */
    private List<AdminRepositoryConsumer> knownContentConsumers;

    /**
     * List of enabled {@link AdminRepositoryConsumer} objects for consumers of known content.
     */
    private List<String> enabledKnownContentConsumers;

    /**
     * List of {@link AdminRepositoryConsumer} objects for consumers of invalid/unknown content.
     */
    private List<AdminRepositoryConsumer> invalidContentConsumers;

    /**
     * List of enabled {@link AdminRepositoryConsumer} objects for consumers of invalid/unknown content.
     */
    private List<String> enabledInvalidContentConsumers;

    private String pattern;

    private String fileTypeId;

    public void addActionError( String anErrorMessage )
    {
        super.addActionError( anErrorMessage );
        log.warn( "[ActionError] {}", anErrorMessage );
    }

    public void addActionMessage( String aMessage )
    {
        super.addActionMessage( aMessage );
        log.info( "[ActionMessage] {}", aMessage );
    }

    public String addFiletypePattern()
    {
        log.info( "Add New File Type Pattern [" + getFileTypeId() + ":" + getPattern() + "]" );

        if ( !isValidFiletypeCommand() )
        {
            return INPUT;
        }

        String id = getFileTypeId();
        String pattern = getPattern();

        FileType filetype = findFileType( id );
        if ( filetype == null )
        {
            addActionError( "Pattern not added, unable to find filetype " + id );
            return INPUT;
        }

        if ( filetype.getPatterns().contains( pattern ) )
        {
            addActionError( "Not adding pattern \"" + pattern + "\" to filetype " + id + " as it already exists." );
            return INPUT;
        }

        filetype.addPattern( pattern );
        addActionMessage( "Added pattern \"" + pattern + "\" to filetype " + id );

        triggerAuditEvent( AuditEvent.ADD_PATTERN + " " + pattern );

        return saveConfiguration();
    }

    public String getFileTypeId()
    {
        return fileTypeId;
    }

    public List<String> getFileTypeIds()
    {
        return fileTypeIds;
    }

    public Map<String, FileType> getFileTypeMap()
    {
        return fileTypeMap;
    }

    public List<AdminRepositoryConsumer> getInvalidContentConsumers()
    {
        return invalidContentConsumers;
    }

    public List<AdminRepositoryConsumer> getKnownContentConsumers()
    {
        return knownContentConsumers;
    }

    public String getPattern()
    {
        return pattern;
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public void prepare()
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();
        RepositoryScanningConfiguration reposcanning = config.getRepositoryScanning();

        FiletypeToMapClosure filetypeToMapClosure = new FiletypeToMapClosure();

        CollectionUtils.forAllDo( reposcanning.getFileTypes(), filetypeToMapClosure );
        fileTypeMap = filetypeToMapClosure.getMap();

        AddAdminRepoConsumerClosure addAdminRepoConsumer;

        addAdminRepoConsumer = new AddAdminRepoConsumerClosure( reposcanning.getKnownContentConsumers() );
        CollectionUtils.forAllDo( repoconsumerUtil.getAvailableKnownConsumers(), addAdminRepoConsumer );
        this.knownContentConsumers = addAdminRepoConsumer.getList();
        Collections.sort( knownContentConsumers, AdminRepositoryConsumerComparator.getInstance() );

        addAdminRepoConsumer = new AddAdminRepoConsumerClosure( reposcanning.getInvalidContentConsumers() );
        CollectionUtils.forAllDo( repoconsumerUtil.getAvailableInvalidConsumers(), addAdminRepoConsumer );
        this.invalidContentConsumers = addAdminRepoConsumer.getList();
        Collections.sort( invalidContentConsumers, AdminRepositoryConsumerComparator.getInstance() );

        fileTypeIds = new ArrayList<String>();
        fileTypeIds.addAll( fileTypeMap.keySet() );
        Collections.sort( fileTypeIds );
    }

    public String removeFiletypePattern()
    {
        log.info( "Remove File Type Pattern [" + getFileTypeId() + ":" + getPattern() + "]" );

        if ( !isValidFiletypeCommand() )
        {
            return INPUT;
        }

        FileType filetype = findFileType( getFileTypeId() );
        if ( filetype == null )
        {
            addActionError( "Pattern not removed, unable to find filetype " + getFileTypeId() );
            return INPUT;
        }

        filetype.removePattern( getPattern() );

        triggerAuditEvent( AuditEvent.REMOVE_PATTERN + " " + pattern );

        return saveConfiguration();
    }

    public void setFileTypeId( String fileTypeId )
    {
        this.fileTypeId = fileTypeId;
    }

    public void setPattern( String pattern )
    {
        this.pattern = pattern;
    }

    public String updateInvalidConsumers()
    {
        addActionMessage( "Update Invalid Consumers" );

        List<String> oldConsumers =
            archivaConfiguration.getConfiguration().getRepositoryScanning().getInvalidContentConsumers();

        archivaConfiguration.getConfiguration().getRepositoryScanning().setInvalidContentConsumers(
            enabledInvalidContentConsumers );

        if ( enabledInvalidContentConsumers != null )
        {
            filterAddedConsumers( oldConsumers, enabledInvalidContentConsumers );
            filterRemovedConsumers( oldConsumers, enabledInvalidContentConsumers );
        }
        else
        {
            disableAllEnabledConsumers( oldConsumers );
        }

        return saveConfiguration();
    }

    public String updateKnownConsumers()
    {
        addActionMessage( "Update Known Consumers" );

        List<String> oldConsumers =
            archivaConfiguration.getConfiguration().getRepositoryScanning().getKnownContentConsumers();

        archivaConfiguration.getConfiguration().getRepositoryScanning().setKnownContentConsumers(
            enabledKnownContentConsumers );

        if ( enabledKnownContentConsumers != null )
        {
            filterAddedConsumers( oldConsumers, enabledKnownContentConsumers );
            filterRemovedConsumers( oldConsumers, enabledKnownContentConsumers );
        }
        else
        {
            disableAllEnabledConsumers( oldConsumers );
        }

        return saveConfiguration();
    }

    private FileType findFileType( String id )
    {
        RepositoryScanningConfiguration scanning = archivaConfiguration.getConfiguration().getRepositoryScanning();
        return (FileType) CollectionUtils.find( scanning.getFileTypes(), new FiletypeSelectionPredicate( id ) );
    }

    private boolean isValidFiletypeCommand()
    {
        if ( StringUtils.isBlank( getFileTypeId() ) )
        {
            addActionError( "Unable to process blank filetype id." );
        }

        if ( StringUtils.isBlank( getPattern() ) )
        {
            addActionError( "Unable to process blank pattern." );
        }

        return !hasActionErrors();
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
            addActionError( "Unable to save configuration: " + e.getMessage() );
            return INPUT;
        }
        catch ( IndeterminateConfigurationException e )
        {
            addActionError( e.getMessage() );
            return INPUT;
        }

        return SUCCESS;
    }

    private void filterAddedConsumers( List<String> oldList, List<String> newList )
    {
        for ( String consumer : newList )
        {
            if ( !oldList.contains( consumer ) )
            {
                triggerAuditEvent( AuditEvent.ENABLE_REPO_CONSUMER + " " + consumer );
            }
        }
    }

    private void filterRemovedConsumers( List<String> oldList, List<String> newList )
    {
        for ( String consumer : oldList )
        {
            if ( !newList.contains( consumer ) )
            {
                triggerAuditEvent( AuditEvent.DISABLE_REPO_CONSUMER + " " + consumer );
            }
        }
    }

    private void disableAllEnabledConsumers( List<String> consumers )
    {
        for ( String consumer : consumers )
        {
            triggerAuditEvent( AuditEvent.DISABLE_REPO_CONSUMER + " " + consumer );
        }
    }

    public List<String> getEnabledInvalidContentConsumers()
    {
        return enabledInvalidContentConsumers;
    }

    public void setEnabledInvalidContentConsumers( List<String> enabledInvalidContentConsumers )
    {
        this.enabledInvalidContentConsumers = enabledInvalidContentConsumers;
    }

    public List<String> getEnabledKnownContentConsumers()
    {
        return enabledKnownContentConsumers;
    }

    public void setEnabledKnownContentConsumers( List<String> enabledKnownContentConsumers )
    {
        this.enabledKnownContentConsumers = enabledKnownContentConsumers;
    }

    public ArchivaConfiguration getArchivaConfiguration()
    {
        return archivaConfiguration;
    }

    public void setArchivaConfiguration( ArchivaConfiguration archivaConfiguration )
    {
        this.archivaConfiguration = archivaConfiguration;
    }
}
