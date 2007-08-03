package org.apache.maven.archiva.repository.content;

import org.apache.commons.lang.StringUtils;

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

/**
 * LegacyArtifactExtensionMapping 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class LegacyArtifactExtensionMapping
    extends AbstractArtifactExtensionMapping
{
    public LegacyArtifactExtensionMapping()
    {
        super();
    }

    public String getType( String pathType, String filename )
    {
        if ( StringUtils.isBlank( filename ) )
        {
            return pathType;
        }

        String normalizedName = filename.toLowerCase().trim();

        if ( normalizedName.endsWith( ".tar.gz" ) )
        {
            return "distribution-tgz";
        }
        else if ( normalizedName.endsWith( ".zip" ) )
        {
            return "distribution-zip";
        }
        else if ( normalizedName.endsWith( "-sources.jar" ) )
        {
            return "jar";
        }
        else if ( normalizedName.endsWith( "-javadoc.jar" ) )
        {
            return "jar";
        }
        else
        {
            if ( pathType.endsWith( "s" ) )
            {
                return pathType.substring( 0, pathType.length() - 1 );
            }
            else
            {
                return pathType;
            }
        }
    }
}
