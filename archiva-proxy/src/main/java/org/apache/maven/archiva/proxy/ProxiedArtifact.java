package org.apache.maven.archiva.proxy;

import java.io.File;

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
 * Composite object for describing an artifact as result of a proxy-request.
 *
 * @author @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ProxiedArtifact
{
    /**
     * The artifact as a file in the managed repository
     */
    private File file;

    /**
     * The artifact Path in the managed repository
     */
    private String path;


    /**
     * Constructor
     */
    public ProxiedArtifact( File file, String path )
    {
        super();
        this.file = file;
        this.path = path;
    }

    /**
     * @return the file
     */
    public File getFile()
    {
        return file;
    }

    /**
     * @return the path
     */
    public String getPath()
    {
        return path;
    }
}
