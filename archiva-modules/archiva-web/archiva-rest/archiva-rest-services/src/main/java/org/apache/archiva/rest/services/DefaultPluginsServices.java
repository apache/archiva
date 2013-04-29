package org.apache.archiva.rest.services;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import org.apache.archiva.rest.api.services.PluginsService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

/**
 * @author Eric Barboni
 */
@Service( "pluginsService#rest" )
public class DefaultPluginsServices
    implements PluginsService
{

    private List<String> repositoryType = new ArrayList<String>();
    
    @Inject
    public DefaultPluginsServices( ApplicationContext applicationContext ) 
    {
        Resource[] xmlResources;
        try {
            xmlResources = applicationContext.getResources( "/**/repository/**/main.js" );
            for (Resource rc : xmlResources) 
            {
                String tmp =  rc.getURL().toString();
                tmp = tmp.substring( tmp.lastIndexOf("repository") + 11,  tmp.length() - 8 );
                repositoryType.add( tmp );
            }
        } catch (IOException ex) {
            
        }
        
    }
    
    @Override
    public String getAdminPlugins()
        throws ArchivaRestServiceException
    {
        // rebuild
        String baseRepo = "archiva/admin/repository/";
        StringBuilder sb = new StringBuilder();
        for (String repoType : repositoryType) 
        {
            sb.append( baseRepo ).append( repoType ).append( "/main" ).append( "|" );
        }
        
        return sb.substring( 0, sb.length() - 1);
        
    }

}
