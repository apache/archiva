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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 */
@Service( "artifactMappingProvider#npanday" )
public class NPandayArtifactMappingProvider
    implements ArtifactMappingProvider
{
    private final Map<String, String> extensionToTypeMap;

    private final Map<String, String> typeToExtensionMap;

    public NPandayArtifactMappingProvider()
    {
        extensionToTypeMap = new HashMap<String, String>();

        // TODO: this could be one of many - we need to look up the artifact metadata from the POM instead
        //       should do this anyway so that plugins don't compete for providing an extension
        extensionToTypeMap.put( "dll", "dotnet-library" );

        extensionToTypeMap.put( "netmodule", "dotnet-module" );
        extensionToTypeMap.put( "exe", "dotnet-executable" );

        typeToExtensionMap = new HashMap<String, String>();
        typeToExtensionMap.put( "dotnet-library", "dll" );
        typeToExtensionMap.put( "dotnet-library-config", "dll.config" );
        typeToExtensionMap.put( "dotnet-executable", "exe" );
        typeToExtensionMap.put( "dotnet-executable-config", "exe.config" );
        typeToExtensionMap.put( "dotnet-module", "netmodule" );
        typeToExtensionMap.put( "dotnet-maven-plugin", "dll" );
        typeToExtensionMap.put( "asp", "dll" );
        typeToExtensionMap.put( "visual-studio-addin", "dll" );
        typeToExtensionMap.put( "sharp-develop-addin", "dll" );
        typeToExtensionMap.put( "nar", "nar" );
        typeToExtensionMap.put( "dotnet-symbols", "pdb" );
        typeToExtensionMap.put( "ole-type-library", "tlb" );
        typeToExtensionMap.put( "dotnet-vsdocs", "xml" );
        typeToExtensionMap.put( "dotnet-archive", "zip" );
        typeToExtensionMap.put( "dotnet-gac", "dll" );
        typeToExtensionMap.put( "gac", "dll" );
        typeToExtensionMap.put( "gac_msil", "dll" );
        typeToExtensionMap.put( "gac_msil4", "dll" );
        typeToExtensionMap.put( "gac_32", "dll" );
        typeToExtensionMap.put( "gac_32_4", "dll" );
        typeToExtensionMap.put( "gac_64", "dll" );
        typeToExtensionMap.put( "gac_64_4", "dll" );
        typeToExtensionMap.put( "com_reference", "dll" );

        // Legacy types
        typeToExtensionMap.put( "library", "dll" );
        typeToExtensionMap.put( "gac_generic", "dll" );
        typeToExtensionMap.put( "netplugin", "dll" );
        typeToExtensionMap.put( "module", "netmodule" );
        typeToExtensionMap.put( "exe.config", "exe.config" );
        typeToExtensionMap.put( "winexe", "exe" );
        typeToExtensionMap.put( "exe", "exe" );
    }

    public String mapClassifierAndExtensionToType( String classifier, String ext )
    {
        // we don't need classifier
        return extensionToTypeMap.get( ext );
    }

    public String mapTypeToExtension( String type )
    {
        return typeToExtensionMap.get( type );
    }
}
