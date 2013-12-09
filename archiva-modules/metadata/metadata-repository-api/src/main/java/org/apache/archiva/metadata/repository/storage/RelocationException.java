package org.apache.archiva.metadata.repository.storage;

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
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class RelocationException
    extends Exception
{
    public enum RelocationType {
        TEMPORARY,PERMANENT;
    }

    private String path;

    private RelocationType relocationType;

    public RelocationException( String path, RelocationType relocationType )
    {
        this.path = path;
        this.relocationType = relocationType;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath( String path )
    {
        this.path = path;
    }

    public RelocationType getRelocationType()
    {
        return relocationType;
    }

    public void setRelocationType( RelocationType relocationType )
    {
        this.relocationType = relocationType;
    }
}
