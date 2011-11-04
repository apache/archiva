package org.apache.archiva.metadata.repository.jcr;
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

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class ArchivaJcrRepositoryConfig
{
    public static RepositoryConfig create( String file, String home )
        throws ConfigurationException
    {
        File homeFile = new File( home );
        if ( !homeFile.exists( ) )
        {
            homeFile.mkdirs( );
        }

        File configurationFile = new File( file );
        if ( !configurationFile.exists( ) )
        {
            String resourcePath = "org/apache/archiva/metadata/repository/jcr/repository.xml";
            LoggerFactory.getLogger( ArchivaJcrRepositoryConfig.class ).info(
                "no repository.xml file in path {} so use default from resources path {}", file, resourcePath );
            // use bundled repository.xml
            return RepositoryConfig.create(
                Thread.currentThread( ).getContextClassLoader( ).getResourceAsStream( resourcePath ), home );
        }

        return RepositoryConfig.create( file, home );
    }
}
