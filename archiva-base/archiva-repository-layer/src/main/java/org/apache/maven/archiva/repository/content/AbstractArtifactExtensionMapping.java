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

import java.util.HashMap;
import java.util.Map;

/**
 * AbstractArtifactExtensionMapping 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public abstract class AbstractArtifactExtensionMapping
{
    protected final Map typeToExtensionMap;

    public AbstractArtifactExtensionMapping()
    {
        typeToExtensionMap = new HashMap();
        typeToExtensionMap.put( "ejb-client", "jar" );
        typeToExtensionMap.put( "ejb", "jar" );
        typeToExtensionMap.put( "distribution-tgz", "tar.gz" );
        typeToExtensionMap.put( "distribution-zip", "zip" );
        typeToExtensionMap.put( "java-source", "jar" );
        typeToExtensionMap.put( "javadoc.jar", "jar" );
        typeToExtensionMap.put( "javadoc", "jar" );
        typeToExtensionMap.put( "aspect", "jar" );
        typeToExtensionMap.put( "uberjar", "jar" );
        typeToExtensionMap.put( "maven-plugin", "jar" );
        typeToExtensionMap.put( "maven-archetype", "jar" );
    }

    public String getExtension( String type )
    {
        // Try specialized types first.
        if ( typeToExtensionMap.containsKey( type ) )
        {
            return (String) typeToExtensionMap.get( type );
        }

        // Return type
        return type;
    }
}
