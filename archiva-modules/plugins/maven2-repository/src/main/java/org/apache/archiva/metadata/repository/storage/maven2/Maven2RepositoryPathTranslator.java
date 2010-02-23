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

    public File toFile( File basedir, String namespace, String projectId, String projectVersion )
    {
        return new File( basedir, toPath( namespace, projectId, projectVersion ) );
    }

    public String toPath( String namespace, String projectId, String projectVersion, String filename )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceToProjectVersion( path, namespace, projectId, projectVersion );
        path.append( PATH_SEPARATOR );
        path.append( filename );

        return path.toString();
    }

    private void appendNamespaceToProjectVersion( StringBuilder path, String namespace, String projectId,
                                                  String projectVersion )
    {
        appendNamespaceAndProject( path, namespace, projectId );
        path.append( projectVersion );
    }

    public String toPath( String namespace, String projectId, String projectVersion )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceToProjectVersion( path, namespace, projectId, projectVersion );

        return path.toString();
    }

    public String toPath( String namespace )
    {
        StringBuilder path = new StringBuilder();

        appendNamespace( path, namespace );

        return path.toString();
    }

    public String toPath( String namespace, String projectId )
    {
        StringBuilder path = new StringBuilder();

        appendNamespaceAndProject( path, namespace, projectId );

        return path.toString();
    }

    private void appendNamespaceAndProject( StringBuilder path, String namespace, String projectId )
    {
        appendNamespace( path, namespace );
        path.append( projectId ).append( PATH_SEPARATOR );
    }

    private void appendNamespace( StringBuilder path, String namespace )
    {
        path.append( formatAsDirectory( namespace ) ).append( PATH_SEPARATOR );
    }

    public File toFile( File basedir, String namespace, String projectId )
    {
        return new File( basedir, toPath( namespace, projectId ) );
    }

    public File toFile( File basedir, String namespace )
    {
        return new File( basedir, toPath( namespace ) );
    }

    private String formatAsDirectory( String directory )
    {
        return directory.replace( GROUP_SEPARATOR, PATH_SEPARATOR );
    }
}
