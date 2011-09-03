package org.apache.archiva.web.startup;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;

/**
 * ArchivaVersion 
 *
 * @version $Id$
 */
public class ArchivaVersion
{
    private static String version = null;

    private ArchivaVersion()
    {
    }

    public static String determineVersion(  )
    {
        if ( version != null )
        {
            return version;
        }
        
        InputStream is = ArchivaConfiguration.class.getResourceAsStream( "/META-INF/maven/org.apache.archiva/archiva-configuration/pom.properties" );
        
        if ( is != null )
        {
            try
            {
                Properties props = new Properties();
                props.load( is );
                String version = props.getProperty( "version" );
                if ( StringUtils.isNotBlank( version ) )
                {
                    ArchivaVersion.version = version;
                    return version;
                }
            }
            catch ( IOException e )
            {
                /* do nothing */
            }
            finally
            {
                IOUtils.closeQuietly( is );
            }
        }

        ArchivaVersion.version = "";
        return version;
    }

    public static String getVersion()
    {
        return version;
    }
}
