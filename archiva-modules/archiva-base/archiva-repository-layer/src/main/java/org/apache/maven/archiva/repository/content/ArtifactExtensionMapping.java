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

import org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider;
import org.apache.archiva.metadata.repository.storage.maven2.DefaultArtifactMappingProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ArtifactExtensionMapping
 *
 * @version $Id$
 */
public class ArtifactExtensionMapping
{
    public static final String MAVEN_ARCHETYPE = "maven-archetype";

    public static final String MAVEN_PLUGIN = "maven-plugin";
	
	public static final String MAVEN_ONE_PLUGIN = "maven-one-plugin";

    private static final Map<String, String> typeToExtensionMap;

    private static final Pattern mavenPluginPattern = Pattern.compile( "^(maven-.*-plugin)|(.*-maven-plugin)$" );

    // TODO: won't support extensions - need to refactor away this class
    private static final ArtifactMappingProvider mapping = new DefaultArtifactMappingProvider();

    static
    {
        typeToExtensionMap = new HashMap<String, String>();
        typeToExtensionMap.put( "ejb-client", "jar" );
        typeToExtensionMap.put( "ejb", "jar" );
        typeToExtensionMap.put( "java-source", "jar" );
        typeToExtensionMap.put( "javadoc", "jar" );
        typeToExtensionMap.put( "test-jar", "jar" );
        typeToExtensionMap.put( MAVEN_PLUGIN, "jar" );

        typeToExtensionMap.put( MAVEN_ARCHETYPE, "jar" );

        // TODO: move to maven 1 plugin
        typeToExtensionMap.put( MAVEN_ONE_PLUGIN, "jar" );
        typeToExtensionMap.put( "javadoc.jar", "jar" );
        typeToExtensionMap.put( "uberjar", "jar" );
        typeToExtensionMap.put( "distribution-tgz", "tar.gz" );
        typeToExtensionMap.put( "distribution-zip", "zip" );
        typeToExtensionMap.put( "aspect", "jar" );
    }

    public static String getExtension( String type )
    {
        // Try specialized types first.
        if ( typeToExtensionMap.containsKey( type ) )
        {
            return typeToExtensionMap.get( type );
        }

        // Return type
        return type;
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

    public static String mapExtensionAndClassifierToType( String classifier, String extension )
    {
        return mapExtensionAndClassifierToType( classifier, extension, extension );
    }

    public static String mapExtensionAndClassifierToType( String classifier, String extension,
                                                           String defaultExtension )
    {
        String value = mapping.mapClassifierAndExtensionToType( classifier, extension );
        if ( value == null )
        {
            value = mapToMaven1Type( extension );
        }
        return value != null ? value : defaultExtension;
    }

    public static String mapExtensionToType( String extension )
    {
        String value = mapToMaven1Type( extension );

        return value != null ? value : extension;
    }

    private static String mapToMaven1Type( String extension )
    {
        // TODO: Maven 1 plugin
        String value = null;
        if ( "tar.gz".equals( extension ) )
        {
            value = "distribution-tgz";
        }
        else  if ( "tar.bz2".equals( extension ) )
        {
            value = "distribution-bzip";
        }
        else  if ( "zip".equals( extension ) )
        {
            value = "distribution-zip";
        }
        return value;
    }
}
