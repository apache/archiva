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
import java.util.Properties;

import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.model.ProjectBuildMetadata;
import org.apache.archiva.metadata.model.ProjectMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.commons.io.IOUtils;

public class FileMetadataRepository
    implements MetadataRepository
{
    private File directory;

    public FileMetadataRepository( File directory )
    {
        this.directory = directory;
    }

    public void updateProject( ProjectMetadata project )
    {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        try
        {
            File projectDirectory = new File( this.directory, project.getId() );
            Properties properties = new Properties();
            properties.setProperty( "id", project.getId() );
            writeProperties( properties, projectDirectory );
        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();
        }
    }

    public void updateBuild( String projectId, ProjectBuildMetadata build )
    {
        File directory = new File( this.directory, projectId );

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

    public void updateArtifact( String projectId, String buildId, ArtifactMetadata artifact )
    {
        File directory = new File( this.directory, projectId + "/" + buildId );

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

        properties.setProperty( artifact.getId() + ".updated", Long.toString( artifact.getUpdated().getTime() ) );
        properties.setProperty( artifact.getId() + ".size", Long.toString( artifact.getSize() ) );

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
