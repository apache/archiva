package org.apache.archiva.web.xmlrpc.api;

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

import java.util.Date;
import java.util.List;

import org.apache.archiva.web.xmlrpc.api.beans.Artifact;
import org.apache.archiva.web.xmlrpc.api.beans.Dependency;

import com.atlassian.xmlrpc.ServiceObject;

@ServiceObject("Search")
public interface SearchService
{
   /*
    * quick/general text search which returns a list of artifacts
    * query for an artifact based on a checksum
    * query for all available versions of an artifact, sorted in version significance order
    * query for all available versions of an artifact since a given date
    * query for an artifact's direct dependencies
    * query for an artifact's dependency tree (as with mvn dependency:tree - no duplicates should be included)
    * query for all artifacts that depend on a given artifact
    */

    public List<Artifact> quickSearch( String queryString ) 
            throws Exception;

    public List<Artifact> getArtifactByChecksum( String checksum) throws Exception;
    
    public List<Artifact> getArtifactVersions( String groupId, String artifactId ) throws Exception;
    
    public List<Artifact> getArtifactVersionsByDate( String groupId, String artifactId, String version, Date whenGathered )
            throws Exception;

    public List<Dependency> getDependencies( String groupId, String artifactId, String version ) 
            throws Exception;
    
    public List<Artifact> getDependencyTree( String groupId, String artifactId, String version ) throws Exception;
    
    public List<Artifact> getDependees( String groupId, String artifactId, String version )
            throws Exception;    
}
