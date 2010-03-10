package org.apache.archiva.metadata.repository.storage.maven2;

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

import java.util.HashMap;
import java.util.Map;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.storage.maven2.ArtifactMappingProvider" role-hint="default"
 */
public class DefaultArtifactMappingProvider
    implements ArtifactMappingProvider
{
    private final Map<String,String> classifierAndExtensionToTypeMap;

    public DefaultArtifactMappingProvider()
    {
        classifierAndExtensionToTypeMap = new HashMap<String,String>();

        // Maven 2.2.1 supplied types (excluding defaults where extension == type and no classifier)
        classifierAndExtensionToTypeMap.put( "client:jar", "ejb-client" );
        classifierAndExtensionToTypeMap.put( "sources:jar", "java-source" );
        classifierAndExtensionToTypeMap.put( "javadoc:jar", "javadoc" );
        classifierAndExtensionToTypeMap.put( "tests:jar", "test-jar" );
    }

    public String mapClassifierAndExtensionToType( String classifier, String ext )
    {
        classifier = classifier != null ? classifier : "";
        ext = ext != null ? ext : "";
        return classifierAndExtensionToTypeMap.get( classifier + ":" + ext );
    }
}
