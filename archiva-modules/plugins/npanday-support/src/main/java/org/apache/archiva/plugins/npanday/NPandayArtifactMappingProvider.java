package org.apache.archiva.plugins.npanday;

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

import org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider" role-hint="npanday"
 */
public class NPandayArtifactMappingProvider
    implements ArtifactMappingProvider
{
    private final Map<String, String> classifierAndExtensionToTypeMap;

    private final Map<String, String> typeToExtensionMap;

    public NPandayArtifactMappingProvider()
    {
        classifierAndExtensionToTypeMap = new HashMap<String, String>();

        // TODO: this could be one of many - we need to look up the artifact metadata from the POM instead
        //       should do this anyway so that plugins don't compete for providing an extension
        classifierAndExtensionToTypeMap.put( "dll", "library" );

        classifierAndExtensionToTypeMap.put( "netmodule", "module" );
        classifierAndExtensionToTypeMap.put( "exe", "winexe" );
        classifierAndExtensionToTypeMap.put( "tests:jar", "test-jar" );

        typeToExtensionMap = new HashMap<String, String>();
        typeToExtensionMap.put( "library", "dll" );
        typeToExtensionMap.put( "asp", "dll" );
        typeToExtensionMap.put( "gac", "dll" );
        typeToExtensionMap.put( "gac_generic", "dll" );
        typeToExtensionMap.put( "gac_msil", "dll" );
        typeToExtensionMap.put( "gac_32", "dll" );
        typeToExtensionMap.put( "netplugin", "dll" );
        typeToExtensionMap.put( "visual-studio-addin", "dll" );
        typeToExtensionMap.put( "module", "netmodule" );
        typeToExtensionMap.put( "exe.config", "exe.config" );
        typeToExtensionMap.put( "winexe", "exe" );
        typeToExtensionMap.put( "nar", "nar" );
    }

    public String mapClassifierAndExtensionToType( String classifier, String ext )
    {
        // we don't need classifier
        return classifierAndExtensionToTypeMap.get( ext );
    }

    public String mapTypeToExtension( String type )
    {
        return typeToExtensionMap.get( type );
    }
}
