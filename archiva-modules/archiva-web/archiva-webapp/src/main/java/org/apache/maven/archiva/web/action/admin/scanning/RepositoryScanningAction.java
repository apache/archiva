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
import org.apache.archiva.admin.repository.RepositoryAdminException;
import org.apache.archiva.admin.repository.admin.ArchivaAdministration;
import org.apache.archiva.admin.repository.admin.FileType;
import org.apache.archiva.admin.repository.admin.FiletypeToMapClosure;
import org.apache.archiva.admin.repository.admin.RepositoryScanning;
import org.apache.archiva.audit.Auditable;
import org.apache.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.codehaus.plexus.redback.rbac.Resource;
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
 */
@Controller( "repositoryScanningAction" )
@Scope( "prototype" )
public class RepositoryScanningAction
    extends AbstractActionSupport
    implements Preparable, Validateable, SecureAction, Auditable
{

    @Inject
    private RepositoryContentConsumers repoconsumerUtil;

    @Inject
    private ArchivaAdministration archivaAdministration;

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
        log.info( "Add New File Type Pattern [{}:{}]", getFileTypeId(), getPattern() );

        if ( !isValidFiletypeCommand() )
        {
            return INPUT;
        }

        try
        {
            getArchivaAdministration().addFileTypePattern( getFileTypeId(), getPattern(), getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "error adding file type pattern " + e.getMessage() );
            return INPUT;
        }
        return SUCCESS;
    }

    public String removeFiletypePattern()
        throws RepositoryAdminException
    {
        log.info( "Remove File Type Pattern [{}:{}]", getFileTypeId(), getPattern() );

        if ( !isValidFiletypeCommand() )
        {
            return INPUT;
        }

        try
        {
            getArchivaAdministration().removeFileTypePattern( getFileTypeId(), getPattern(), getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            addActionError( "error adding file type pattern " + e.getMessage() );
            return INPUT;
        }

        return SUCCESS;
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
        RepositoryScanning reposcanning = archivaAdministration.getRepositoryScanning();

        FiletypeToMapClosure filetypeToMapClosure = new FiletypeToMapClosure();

        CollectionUtils.forAllDo( reposcanning.getFileTypes(), filetypeToMapClosure );
        fileTypeMap = filetypeToMapClosure.getMap();

        AddAdminRepoConsumerClosure addAdminRepoConsumer =
            new AddAdminRepoConsumerClosure( reposcanning.getKnownContentConsumers() );
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

        try
        {
            List<String> oldConsumers = getArchivaAdministration().getRepositoryScanning().getInvalidContentConsumers();

            if ( enabledInvalidContentConsumers != null )
            {
                for ( String oldConsumer : oldConsumers )
                {
                    if ( !enabledInvalidContentConsumers.contains( oldConsumer ) )
                    {
                        getArchivaAdministration().removeInvalidContentConsumer( oldConsumer, getAuditInformation() );
                    }
                }
                for ( String enabledKnowContentConsumer : enabledInvalidContentConsumers )
                {
                    getArchivaAdministration().addInvalidContentConsumer( enabledKnowContentConsumer,
                                                                          getAuditInformation() );
                }
            }
            else
            {
                for ( String oldConsumer : oldConsumers )
                {
                    getArchivaAdministration().removeInvalidContentConsumer( oldConsumer, getAuditInformation() );
                }
            }
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            addActionError( "Error update invalidContentConsumers " + e.getMessage() );
            return INPUT;
        }
        addActionMessage( "Update Invalid Consumers" );

        return SUCCESS;
    }

    public String updateKnownConsumers()
    {

        try
        {
            List<String> oldConsumers = getArchivaAdministration().getRepositoryScanning().getKnownContentConsumers();

            if ( enabledKnownContentConsumers != null )
            {
                for ( String oldConsumer : oldConsumers )
                {
                    if ( !enabledKnownContentConsumers.contains( oldConsumer ) )
                    {
                        getArchivaAdministration().removeKnownContentConsumer( oldConsumer, getAuditInformation() );
                    }
                }
                for ( String enabledKnowContentConsumer : enabledKnownContentConsumers )
                {
                    getArchivaAdministration().addKnownContentConsumer( enabledKnowContentConsumer,
                                                                        getAuditInformation() );
                }
            }
            else
            {
                for ( String oldConsumer : oldConsumers )
                {
                    getArchivaAdministration().removeKnownContentConsumer( oldConsumer, getAuditInformation() );
                }
            }
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            addActionError( "Error update knowContentConsumers " + e.getMessage() );
            return INPUT;
        }
        addActionMessage( "Update Known Consumers" );

        return SUCCESS;
    }

    private FileType findFileType( String id )
        throws RepositoryAdminException
    {
        return getArchivaAdministration().getFileType( id );
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

    public ArchivaAdministration getArchivaAdministration()
    {
        return archivaAdministration;
    }

    public void setArchivaAdministration( ArchivaAdministration archivaAdministration )
    {
        this.archivaAdministration = archivaAdministration;
    }
}
