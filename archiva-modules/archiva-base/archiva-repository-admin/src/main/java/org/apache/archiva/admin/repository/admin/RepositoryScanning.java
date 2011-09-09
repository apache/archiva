package org.apache.archiva.admin.repository.admin;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class RepositoryScanning
    implements Serializable
{
    /**
     * Field fileTypes.
     */
    private List<FileType> fileTypes;

    /**
     * Field knownContentConsumers.
     */
    private List<String> knownContentConsumers;

    /**
     * Field invalidContentConsumers.
     */
    private List<String> invalidContentConsumers;

    public RepositoryScanning()
    {
        // no op
    }

    public RepositoryScanning( List<FileType> fileTypes, List<String> knownContentConsumers,
                               List<String> invalidContentConsumers )
    {
        this.fileTypes = fileTypes;
        this.knownContentConsumers = knownContentConsumers;
        this.invalidContentConsumers = invalidContentConsumers;
    }

    public List<FileType> getFileTypes()
    {
        if ( this.fileTypes == null )
        {
            this.fileTypes = new ArrayList<FileType>();
        }
        return fileTypes;
    }

    public void setFileTypes( List<FileType> fileTypes )
    {
        this.fileTypes = fileTypes;
    }

    public List<String> getKnownContentConsumers()
    {
        if ( this.knownContentConsumers == null )
        {
            this.knownContentConsumers = new ArrayList<String>();
        }
        return knownContentConsumers;
    }

    public void setKnownContentConsumers( List<String> knownContentConsumers )
    {
        this.knownContentConsumers = knownContentConsumers;
    }

    public List<String> getInvalidContentConsumers()
    {
        if ( this.invalidContentConsumers == null )
        {
            this.invalidContentConsumers = new ArrayList<String>();
        }
        return invalidContentConsumers;
    }

    public void setInvalidContentConsumers( List<String> invalidContentConsumers )
    {
        this.invalidContentConsumers = invalidContentConsumers;
    }
}
