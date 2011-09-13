package org.apache.archiva.rest.api.services;

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


import org.apache.archiva.rest.api.model.Artifact;
import org.apache.archiva.rest.api.model.Dependency;

import javax.ws.rs.Path;
import java.util.List;

@Path( "/searchService/" )
public interface SearchService
{
    /*
    * quick/general text search which returns a list of artifacts
    * query for an artifact based on a checksum
    * query for all available versions of an artifact, sorted in version significance order
    * query for an artifact's direct dependencies
    * TODO query for an artifact's dependency tree (as with mvn dependency:tree - no duplicates should be included)
    * TODO query for all artifacts that depend on a given artifact
    */

    List<Artifact> quickSearch( String queryString )
        throws Exception;

    List<Artifact> getArtifactByChecksum( String checksum )
        throws Exception;

    List<Artifact> getArtifactVersions( String groupId, String artifactId )
        throws Exception;

    List<Dependency> getDependencies( String groupId, String artifactId, String version )
        throws Exception;

}
