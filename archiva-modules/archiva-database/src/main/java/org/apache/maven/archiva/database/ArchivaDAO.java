package org.apache.maven.archiva.database;

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

import java.io.Serializable;
import java.util.List;

/**
 * ArchivaDAO - The interface for all content within the database.
 *
 * @version $Id$
 */
public interface ArchivaDAO
{
    public static final String ROLE = ArchivaDAO.class.getName();

    /**
     * Perform a simple query against the database.
     * 
     * @param constraint the constraint to use.
     * @return the List of results.
     */
    List<?> query( SimpleConstraint constraint );

    /**
     * Perform a simple save of a peristable object to the database.
     * 
     * @param o the serializable (persistable) object to save.
     * @return the post-serialized object.
     */
    Object save( Serializable obj );
    
    ArtifactDAO getArtifactDAO();

    ProjectModelDAO getProjectModelDAO();

    RepositoryProblemDAO getRepositoryProblemDAO();
    
    RepositoryContentStatisticsDAO getRepositoryContentStatisticsDAO();
}
