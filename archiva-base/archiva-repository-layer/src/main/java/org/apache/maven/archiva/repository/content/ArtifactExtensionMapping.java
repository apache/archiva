package org.apache.maven.archiva.repository.content;

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

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ArtifactExtensionMapping
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArtifactExtensionMapping
{
    public static final String MAVEN_ARCHETYPE = "maven-archetype";

    public static final String MAVEN_PLUGIN = "maven-plugin";

    private static final Map<String, String> typeToExtensionMap;

    private static final Pattern mavenPluginPattern = Pattern.compile( "^(maven-.*-plugin)|(.*-maven-plugin)$" );

    static
    {
        typeToExtensionMap = new HashMap<String, String>();
        typeToExtensionMap.put( "ejb-client", "jar" );
        typeToExtensionMap.put( "ejb", "jar" );
        typeToExtensionMap.put( "distribution-tgz", "tar.gz" );
        typeToExtensionMap.put( "distribution-zip", "zip" );
        typeToExtensionMap.put( "java-source", "jar" );
        typeToExtensionMap.put( "javadoc.jar", "jar" );
        typeToExtensionMap.put( "javadoc", "jar" );
        typeToExtensionMap.put( "aspect", "jar" );
        typeToExtensionMap.put( "uberjar", "jar" );
        typeToExtensionMap.put( MAVEN_PLUGIN, "jar" );
        typeToExtensionMap.put( MAVEN_ARCHETYPE, "jar" );
    }

    public static String getExtension( String type )
    {
        // Try specialized types first.
        if ( typeToExtensionMap.containsKey( type ) )
        {
            return (String) typeToExtensionMap.get( type );
        }

        // Return type
        return type.replace( '-', '.' );
    }

    public static String guessTypeFromFilename( File file )
    {
        return guessTypeFromFilename( file.getName() );
    }

    public static String guessTypeFromFilename( String filename )
    {
        if ( StringUtils.isBlank( filename ) )
        {
            return null;
        }

        String normalizedName = filename.toLowerCase().trim();
        int idx = normalizedName.lastIndexOf( '.' );

        if ( idx == ( -1 ) )
        {
            return null;
        }

        if ( normalizedName.endsWith( ".tar.gz" ) )
        {
            return "distribution-tgz";
        }
        if ( normalizedName.endsWith( ".tar.bz2" ) )
        {
            return "distribution-bzip";
        }
        else if ( normalizedName.endsWith( ".zip" ) )
        {
            return "distribution-zip";
        }
        else if ( normalizedName.endsWith( "-sources.jar" ) )
        {
            return "java-source";
        }
        else if ( normalizedName.endsWith( "-javadoc.jar" ) )
        {
            return "javadoc";
        }
        else
        {
            return normalizedName.substring( idx + 1 );
        }
    }

    /**
     * Determine if a given artifact Id conforms to the naming scheme for a maven plugin.
     *
     * @param artifactId the artifactId to test.
     * @return true if this artifactId conforms to the naming scheme for a maven plugin.
     */
    public static boolean isMavenPlugin( String artifactId )
    {
        return mavenPluginPattern.matcher( artifactId ).matches();
    }
}
