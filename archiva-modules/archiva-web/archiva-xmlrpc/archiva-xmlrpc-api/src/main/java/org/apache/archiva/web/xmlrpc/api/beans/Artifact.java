package org.apache.archiva.web.xmlrpc.api.beans;

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

import com.atlassian.xmlrpc.ServiceBean;
import com.atlassian.xmlrpc.ServiceBeanField;

@ServiceBean
public class Artifact
    implements Serializable
{
    private String repositoryId;
    
    private String groupId;

    private String artifactId;

    private String version;

    private String type;

    //private Date whenGathered;
    
    public Artifact()
    {

    }

    public Artifact( String repositoryId, String groupId, String artifactId, String version, String type )
//                     String type, Date whenGathered )
    {   
        this.repositoryId = repositoryId;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        //this.whenGathered = whenGathered;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getType()
    {
        return type;
    }
    
    public String getRepositoryId()
    {
        return repositoryId;
    }

    /*public Date getWhenGathered()
    {
        return whenGathered;
    }*/

    @ServiceBeanField( "groupId" )
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    @ServiceBeanField( "artifactId" )
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    @ServiceBeanField( "version" )
    public void setVersion( String version )
    {
        this.version = version;
    }

    @ServiceBeanField( "type" )
    public void setType( String type )
    {
        this.type = type;
    }
    
    @ServiceBeanField( "repositoryId" )
    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    /*@ServiceBeanField( "whenGathered" )
    public void setWhenGathered( Date whenGathered )
    {
        this.whenGathered = whenGathered;
    }*/    
}
