package org.apache.archiva.repository.content;

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

import java.util.HashMap;
import java.util.Map;

/**
 * Maven 1.x request type to classifier mapping for translating to a Maven 2.x storage
 *
 * TODO reuse mappings for other repositories
 *
 * @since 1.1
 */
public class ArtifactClassifierMapping
{
    private static final Map<String, String> typeToClassifierMap;

    static
    {
        // note additional 's' on type as these are maven 1.x directory components
        typeToClassifierMap = new HashMap<>( 3 );
        typeToClassifierMap.put( "java-sources", "sources" );
        typeToClassifierMap.put( "javadoc.jars", "javadoc" );
        typeToClassifierMap.put( "javadocs", "javadoc" );
    }

    public static String getClassifier( String type )
    {
        // Try specialized types first.
        if ( typeToClassifierMap.containsKey( type ) )
        {
            return typeToClassifierMap.get( type );
        }

        // No classifier
        return null;
    }
}

