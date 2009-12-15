package org.apache.maven.archiva.web.action.admin.repositories;

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

import java.util.List;

import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.SimpleConstraint;

/**
 * Stub class for Archiva DAO to avoid having to set up a database for tests.
 *
 * @todo a mock would be better, but that won't play nicely with Plexus injection.
 */
public class ArchivaDAOStub
    implements ArchivaDAO
{

    private ArtifactDAO artifactDao;

    public List<?> query( SimpleConstraint constraint )
    {
        throw new UnsupportedOperationException();
    }

    public ArtifactDAO getArtifactDAO()
    {
        return artifactDao;
    }

    public void setArtifactDao( ArtifactDAO artifactDao )
    {
        this.artifactDao = artifactDao;
    }

}
