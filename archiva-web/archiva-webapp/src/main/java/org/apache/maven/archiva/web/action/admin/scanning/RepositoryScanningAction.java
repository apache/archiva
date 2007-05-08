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

import com.opensymphony.xwork.Preparable;
import com.opensymphony.xwork.Validateable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.functors.FiletypeToMapClosure;
import org.apache.maven.archiva.security.ArchivaRoleConstants;
import org.codehaus.plexus.security.rbac.Resource;
import org.codehaus.plexus.security.ui.web.interceptor.SecureAction;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionBundle;
import org.codehaus.plexus.security.ui.web.interceptor.SecureActionException;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RepositoryScanningAction 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 * 
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="repositoryScanningAction"
 */
public class RepositoryScanningAction
    extends PlexusActionSupport
    implements Preparable, Validateable, SecureAction
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    private Map fileTypeMap;

    private List goodConsumers = new ArrayList();

    private List badConsumers = new ArrayList();

    public void prepare()
        throws Exception
    {
        Configuration config = archivaConfiguration.getConfiguration();
        FiletypeToMapClosure filetypeToMapClosure = new FiletypeToMapClosure();

        CollectionUtils.forAllDo( config.getRepositoryScanning().getFileTypes(), filetypeToMapClosure );
        fileTypeMap = filetypeToMapClosure.getMap();

        goodConsumers.clear();
        goodConsumers.addAll( config.getRepositoryScanning().getGoodConsumers() );

        badConsumers.clear();
        badConsumers.addAll( config.getRepositoryScanning().getBadConsumers() );
    }

    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {
        SecureActionBundle bundle = new SecureActionBundle();

        bundle.setRequiresAuthentication( true );
        bundle.addRequiredAuthorization( ArchivaRoleConstants.OPERATION_MANAGE_CONFIGURATION, Resource.GLOBAL );

        return bundle;
    }

    public List getBadConsumers()
    {
        return badConsumers;
    }

    public void setBadConsumers( List badConsumers )
    {
        this.badConsumers = badConsumers;
    }

    public Map getFileTypeMap()
    {
        return fileTypeMap;
    }

    public void setFileTypeMap( Map fileTypeMap )
    {
        this.fileTypeMap = fileTypeMap;
    }

    public List getGoodConsumers()
    {
        return goodConsumers;
    }

    public void setGoodConsumers( List goodConsumers )
    {
        this.goodConsumers = goodConsumers;
    }
}
