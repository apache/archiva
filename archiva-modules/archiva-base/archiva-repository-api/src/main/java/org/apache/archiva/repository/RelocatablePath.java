package org.apache.archiva.repository;
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
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class RelocatablePath
{

    private final String path;
    private final String originPath;
    private final boolean relocated;

    RelocatablePath(String path, String originPath) {
        this.path = path;
        this.originPath = originPath;
        this.relocated = !path.equals(originPath);
    }

    RelocatablePath(String path) {
        this.path = path;
        this.originPath = path;
        this.relocated = false;
    }

    public String getPath( )
    {
        return path;
    }

    public String getOriginPath( )
    {
        return originPath;
    }

    public boolean isRelocated( )
    {
        return relocated;
    }


}
