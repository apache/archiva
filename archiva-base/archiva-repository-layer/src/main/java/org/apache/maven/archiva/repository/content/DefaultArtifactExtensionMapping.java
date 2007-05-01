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

/**
 * DefaultArtifactExtensionMapping - extension mapping for Maven 2.x projects. 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DefaultArtifactExtensionMapping extends AbstractArtifactExtensionMapping
{
    public DefaultArtifactExtensionMapping()
    {
        super();
    }

    public String getType( String filename )
    {
        if ( StringUtils.isBlank( filename ) )
        {
            return null;
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
            return "java-source";
        }
        // TODO: handle type for -javadoc.jar ?
        else
        {
            int index = normalizedName.lastIndexOf( '.' );
            if ( index >= 0 )
            {
                return normalizedName.substring( index + 1 );
            }
            else
            {
                throw new IllegalArgumentException( "Filename " + filename + " does not have an extension." );
            }
        }
    }
}
