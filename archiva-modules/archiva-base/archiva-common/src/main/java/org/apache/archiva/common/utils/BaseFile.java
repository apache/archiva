package org.apache.archiva.common.utils;

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

import java.io.File;
import java.net.URI;

/**
 * BaseFile - convenient File object that tracks the Base Directory and can provide relative path values
 * for the file object based on that Base Directory value.
 *
 *
 */
public class BaseFile
    extends File
{
    private File baseDir;

    public BaseFile( File pathFile )
    {
        this( pathFile.getAbsolutePath() );
    }

    public BaseFile( File repoDir, File pathFile )
    {
        this( repoDir, PathUtil.getRelative(repoDir.getAbsolutePath(), pathFile.toPath() ) );
    }

    public BaseFile( File parent, String child )
    {
        super( parent, child );
        this.baseDir = parent;
    }

    public BaseFile( String pathname )
    {
        super( pathname );

        // Calculate the top level directory.

        File parent = this;
        while ( parent.getParentFile() != null )
        {
            parent = parent.getParentFile();
        }

        this.baseDir = parent;
    }

    public BaseFile( String repoDir, File pathFile )
    {
        this( new File( repoDir ), pathFile );
    }

    public BaseFile( String parent, String child )
    {
        super( parent, child );
        this.baseDir = new File( parent );
    }

    public BaseFile( URI uri )
    {
        super( uri ); // only to satisfy java compiler.
        throw new IllegalStateException(
            "The " + BaseFile.class.getName() + " object does not support URI construction." );
    }

    public File getBaseDir()
    {
        return baseDir;
    }

    public String getRelativePath()
    {
        return PathUtil.getRelative( this.baseDir.getAbsolutePath(), this.toPath() );
    }

    public void setBaseDir( File baseDir )
    {
        this.baseDir = baseDir;
    }

    public void setBaseDir( String repoDir )
    {
        setBaseDir( new File( repoDir ) );
    }
}
