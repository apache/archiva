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
 * @since 1.1
 */
public class ArtifactClassifierMapping
{
    private static final Map<String, String> typeToClassifierMap;

    static
    {
        typeToClassifierMap = new HashMap<String, String>();
        typeToClassifierMap.put( "java-source", "sources" );
        typeToClassifierMap.put( "javadoc.jar", "javadoc" );
        typeToClassifierMap.put( "javadoc", "javadoc" );
    }

    public static String getClassifier( String type )
    {
        // Try specialized types first.
        if ( typeToClassifierMap.containsKey( type ) )
        {
            return (String) typeToClassifierMap.get( type );
        }

        // No classifier
        return null;
    }
}

