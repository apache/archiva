package org.apache.maven.archiva.web.action.admin.legacy;

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

import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.IndeterminateConfigurationException;
import org.apache.maven.archiva.configuration.LegacyArtifactPath;
import org.apache.maven.archiva.web.action.AbstractActionSupport;
import org.codehaus.plexus.registry.RegistryException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import javax.inject.Inject;
import java.util.Iterator;

/**
 * Delete a LegacyArtifactPath to archiva configuration
 *
 * @since 1.1
 */
@Controller( "deleteLegacyArtifactPathAction" )
@Scope( "prototype" )
public class DeleteLegacyArtifactPathAction
    extends AbstractActionSupport
{

    @Inject
    private ArchivaConfiguration archivaConfiguration;

    private String path;

    public String delete()
    {
        log.info( "remove [" + path + "] from legacy artifact path resolution" );
        Configuration configuration = archivaConfiguration.getConfiguration();
        for ( Iterator<LegacyArtifactPath> iterator = configuration.getLegacyArtifactPaths().iterator();
              iterator.hasNext(); )
        {
            LegacyArtifactPath legacyArtifactPath = (LegacyArtifactPath) iterator.next();
            if ( legacyArtifactPath.match( path ) )
            {
                iterator.remove();
            }
        }
        return saveConfiguration( configuration );
    }

    protected String saveConfiguration( Configuration configuration )
    {
        try
        {
            archivaConfiguration.save( configuration );
            addActionMessage( "Successfully saved configuration" );
        }
        catch ( IndeterminateConfigurationException e )
        {
            addActionError( e.getMessage() );
            return INPUT;
        }
        catch ( RegistryException e )
        {
            addActionError( "Configuration Registry Exception: " + e.getMessage() );
            return INPUT;
        }

        return SUCCESS;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }
}
