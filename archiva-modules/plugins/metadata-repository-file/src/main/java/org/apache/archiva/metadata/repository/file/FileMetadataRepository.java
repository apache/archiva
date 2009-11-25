package org.apache.archiva.metadata.repository.file;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectBuildMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.commons.io.IOUtils;

/**
 * @plexus.component role="org.apache.archiva.metadata.repository.MetadataRepository"
 */
public class FileMetadataRepository
    implements MetadataRepository
{
    /**
     * TODO: this isn't suitable for production use
     * @plexus.configuration
     */
    private File directory = new File( System.getProperty( "user.home" ), ".archiva-metadata" );

    public void updateProject( String repoId, ProjectMetadata project )
    {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        try
        {
            File projectDirectory =
                new File( this.directory, repoId + "/" + project.getNamespace() + "/" + project.getId() );
            Properties properties = new Properties();
            properties.setProperty( "namespace", project.getNamespace() );
            properties.setProperty( "id", project.getId() );
            writeProperties( properties, projectDirectory );
        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();
        }
    }

    public void updateBuild( String repoId, String namespace, String projectId, ProjectBuildMetadata build )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId );

        Properties properties = new Properties();
        properties.setProperty( "id", build.getId() );

        try
        {
            writeProperties( properties, new File( directory, build.getId() ) );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void updateArtifact( String repoId, String namespace, String projectId, String buildId,
                                ArtifactMetadata artifact )
    {
        File directory = new File( this.directory, repoId + "/" + namespace + "/" + projectId + "/" + buildId );

        Properties properties = readProperties( directory );

        properties.setProperty( "updated:" + artifact.getId(), Long.toString( artifact.getUpdated().getTime() ) );
        properties.setProperty( "size:" + artifact.getId(), Long.toString( artifact.getSize() ) );
        properties.setProperty( "version:" + artifact.getId(), artifact.getVersion() );

        try
        {
            writeProperties( properties, directory );
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private Properties readProperties( File directory )
    {
        Properties properties = new Properties();
        FileInputStream in = null;
        try
        {
            in = new FileInputStream( new File( directory, "metadata.xml" ) );
            properties.load( in );
        }
        catch ( FileNotFoundException e )
        {
            // skip - use blank properties
        }
        catch ( IOException e )
        {
            // TODO
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }
        return properties;
    }

    public ProjectMetadata getProject( String repoId, String groupId, String projectId )
    {
        File directory = new File( this.directory, repoId + "/" + projectId );

        Properties properties = readProperties( directory );

        ProjectMetadata project = new ProjectMetadata();
        project.setNamespace( properties.getProperty( "namespace" ) );
        project.setId( properties.getProperty( "id" ) );
        return project;
    }

    public ProjectBuildMetadata getProjectBuild( String repoId, String groupId, String projectId, String buildId )
    {
        File directory = new File( this.directory, repoId + "/" + projectId + "/" + buildId );

        Properties properties = readProperties( directory );

        ProjectBuildMetadata build = new ProjectBuildMetadata();
        build.setId( properties.getProperty( "id" ) );
        return build;
    }

    public Collection<String> getArtifactVersions( String repoId, String namespace, String projectId, String buildId )
    {
        File directory = new File( this.directory, repoId + "/" + projectId + "/" + buildId );

        Properties properties = readProperties( directory );

        List<String> versions = new ArrayList<String>();
        for ( Map.Entry entry : properties.entrySet() )
        {
            String name = (String) entry.getKey();
            if ( name.startsWith( "version:" ) )
            {
                versions.add( (String) entry.getValue() );
            }
        }
        return versions;
    }

    private void writeProperties( Properties properties, File directory )
        throws IOException
    {
        directory.mkdirs();
        FileOutputStream os = new FileOutputStream( new File( directory, "metadata.xml" ) );
        try
        {
            properties.storeToXML( os, null );
        }
        finally
        {
            IOUtils.closeQuietly( os );
        }
    }

}
