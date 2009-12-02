package org.apache.maven.archiva.proxy.stubs;

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
import java.util.List;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.database.SimpleConstraint;

/**
 * Using a stub for faster tests! Not really used for the unit tests, just for dependency injection.
 */
public class ArchivaDAOStub
    implements ArchivaDAO
{

    public ArtifactDAO getArtifactDAO()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RepositoryContentStatisticsDAO getRepositoryContentStatisticsDAO()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public RepositoryProblemDAO getRepositoryProblemDAO()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public List<?> query( SimpleConstraint constraint )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public Object save( Serializable obj )
    {
        // TODO Auto-generated method stub
        return null;
    }

}
