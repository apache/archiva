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

/**
 * ArtifactExtensionMapping
 *
 * @version $Id$
 */
public class ArtifactExtensionMapping
{
    public static final String MAVEN_ONE_PLUGIN = "maven-one-plugin";

    // TODO: now only used in Maven 1, we should be using M1 specific mappings
    private static final ArtifactMappingProvider mapping = new DefaultArtifactMappingProvider();

    public static String getExtension( String type )
    {
        String ext = mapping.mapTypeToExtension( type );

        if ( ext == null )
        {
            ext = type;
        }

        return ext;
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
            // TODO: Maven 1 plugin
            String value1 = null;
            if ( "tar.gz".equals( extension ) )
            {
                value1 = "distribution-tgz";
            }
            else  if ( "tar.bz2".equals( extension ) )
            {
                value1 = "distribution-bzip";
            }
            else  if ( "zip".equals( extension ) )
            {
                value1 = "distribution-zip";
            }
            value = value1;
        }
        return value != null ? value : defaultExtension;
    }
}
