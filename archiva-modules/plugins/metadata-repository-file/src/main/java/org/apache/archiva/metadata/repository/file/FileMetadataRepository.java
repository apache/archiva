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

    public void update( ProjectMetadata project )
    {
        // TODO: this is a more braindead implementation than we would normally expect, for prototyping purposes
        try
        {
            File projectDirectory = new File( this.directory, project.getId() );
            store( project, projectDirectory );

            for ( ProjectBuildMetadata build : project.getBuilds() )
            {
                store( build, projectDirectory );
            }
        }
        catch ( IOException e )
        {
            // TODO!
            e.printStackTrace();
        }
    }

    private void store( ProjectBuildMetadata build, File directory )
        throws FileNotFoundException, IOException
    {
        Properties properties = new Properties();
        properties.setProperty( "id", build.getId() );
        
        for ( ArtifactMetadata artifact : build.getArtifacts() )
        {
            properties.setProperty( artifact.getId() + ".updated", Long.toString( artifact.getUpdated().getTime() ) );
            properties.setProperty( artifact.getId() + ".size", Long.toString( artifact.getSize() ) );
        }
        
        writeProperties( properties, new File( directory, build.getId() ) );
    }

    private void store( ProjectMetadata project, File directory )
        throws FileNotFoundException, IOException
    {

        Properties properties = new Properties();
        properties.setProperty( "id", project.getId() );
        writeProperties( properties, directory );
    }

    private void writeProperties( Properties properties, File directory )
        throws FileNotFoundException, IOException
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
