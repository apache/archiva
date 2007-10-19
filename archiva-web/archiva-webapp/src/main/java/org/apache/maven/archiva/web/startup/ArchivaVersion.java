package org.apache.maven.archiva.web.startup;

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

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * ArchivaVersion 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArchivaVersion
{
    public static String VERSION = "Unknown";
    
    public static String determineVersion( ClassLoader cloader )
    {
        if ( VERSION != null )
        {
            return VERSION;
        }
        
        /* This is the search order of modules to find the version.
         */
        String modules[] = new String[] {
            "archiva-common",
            "archiva-configuration",
            "archiva-database",
            "archiva-consumer-api",
            "archiva-core-consumers",
            "archiva-signature-consumers",
            "archiva-database-consumers",
            "archiva-lucene-consumers",
            "archiva-indexer",
            "archiva-model",
            "archiva-policies",
            "archiva-proxy",
            "archiva-report-manager",
            "archiva-artifact-reports",
            "archiva-project-reports",
            "archiva-metadata-reports",
            "archiva-repository-layer",
            "archiva-scheduled",
            "archiva-webapp",
            "archiva-security",
            "archiva-applet",
            "archiva-xml-tools" };

        for ( int i = 0; i < modules.length; i++ )
        {
            String module = modules[i];
            URL pomurl = findModulePom( cloader, module );
            if ( pomurl != null )
            {
                try
                {
                    Properties props = new Properties();
                    InputStream is = pomurl.openStream();
                    props.load( is );
                    String version = props.getProperty( "version" );
                    if ( StringUtils.isNotBlank( version ) )
                    {
                        VERSION = version;
                        return VERSION;
                    }
                }
                catch ( IOException e )
                {
                    /* do nothing */
                }
            }
        }

        return VERSION;
    }

    private static URL findModulePom( ClassLoader cloader, String module )
    {
        URL ret = cloader.getResource( "/META-INF/maven/org.apache.maven.archiva/" + module + "/pom.properties" );
        return ret;
    }
}
