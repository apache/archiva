package org.apache.archiva.metadata.repository.storage.maven2;

import java.io.File;

import org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator;

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

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.storage.RepositoryPathTranslator" role-hint="maven2"
 */
public class Maven2RepositoryPathTranslator
    implements RepositoryPathTranslator
{
    private static final char PATH_SEPARATOR = '/';

    private static final char GROUP_SEPARATOR = '.';

    public File toFile( File basedir, String namespace, String projectId, String projectVersion, String filename )
    {
        return new File( basedir, toPath( namespace, projectId, projectVersion, filename ) );
    }

    public String toPath( String namespace, String projectId, String projectVersion, String filename )
    {
        StringBuilder path = new StringBuilder();

        path.append( formatAsDirectory( namespace ) ).append( PATH_SEPARATOR );
        path.append( projectId ).append( PATH_SEPARATOR );
        path.append( projectVersion ).append( PATH_SEPARATOR );
        path.append( filename );

        return path.toString();
    }

    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }
}
