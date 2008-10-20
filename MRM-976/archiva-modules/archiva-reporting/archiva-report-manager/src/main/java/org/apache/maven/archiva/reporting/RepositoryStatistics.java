package org.apache.maven.archiva.reporting;

import java.util.Date;

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
 * RepositoryStatistics
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class RepositoryStatistics
{
    private String repositoryId;
    
    private long fileCount = 0;
    
    private long totalSize = 0;
    
    private long projectCount = 0;
    
    private long groupCount = 0;
    
    private long artifactCount = 0;
    
    private long pluginCount = 0;
    
    private long archetypeCount = 0;
    
    private long jarCount = 0;
    
    private long warCount = 0;
    
    private long earCount = 0;
    
    private long dllCount = 0;
    
    private long exeCount = 0;
    
    private long pomCount = 0;
    
    private long deploymentCount = 0;
    
    private long downloadCount = 0;
    
    private Date dateOfScan;

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public long getFileCount()
    {
        return fileCount;
    }

    public void setFileCount( long fileCount )
    {
        this.fileCount = fileCount;
    }

    public long getTotalSize()
    {
        return totalSize;
    }

    public void setTotalSize( long totalSize )
    {
        this.totalSize = totalSize;
    }

    public long getProjectCount()
    {
        return projectCount;
    }

    public void setProjectCount( long projectCount )
    {
        this.projectCount = projectCount;
    }

    public long getGroupCount()
    {
        return groupCount;
    }

    public void setGroupCount( long groupCount )
    {
        this.groupCount = groupCount;
    }

    public long getArtifactCount()
    {
        return artifactCount;
    }

    public void setArtifactCount( long artifactCount )
    {
        this.artifactCount = artifactCount;
    }

    public long getPluginCount()
    {
        return pluginCount;
    }

    public void setPluginCount( long pluginCount )
    {
        this.pluginCount = pluginCount;
    }

    public long getArchetypeCount()
    {
        return archetypeCount;
    }

    public void setArchetypeCount( long archetypeCount )
    {
        this.archetypeCount = archetypeCount;
    }

    public long getJarCount()
    {
        return jarCount;
    }

    public void setJarCount( long jarCount )
    {
        this.jarCount = jarCount;
    }

    public long getWarCount()
    {
        return warCount;
    }

    public void setWarCount( long warCount )
    {
        this.warCount = warCount;
    }

    public long getEarCount()
    {
        return earCount;
    }

    public void setEarCount( long earCount )
    {
        this.earCount = earCount;
    }

    public long getDllCount()
    {
        return dllCount;
    }

    public void setDllCount( long dllCount )
    {
        this.dllCount = dllCount;
    }

    public long getExeCount()
    {
        return exeCount;
    }

    public void setExeCount( long exeCount )
    {
        this.exeCount = exeCount;
    }

    public long getPomCount()
    {
        return pomCount;
    }

    public void setPomCount( long pomCount )
    {
        this.pomCount = pomCount;
    }

    public long getDeploymentCount()
    {
        return deploymentCount;
    }

    public void setDeploymentCount( long deploymentCount )
    {
        this.deploymentCount = deploymentCount;
    }

    public long getDownloadCount()
    {
        return downloadCount;
    }

    public void setDownloadCount( long downloadCount )
    {
        this.downloadCount = downloadCount;
    }

    public Date getDateOfScan()
    {
        return dateOfScan;
    }

    public void setDateOfScan( Date dateOfScan )
    {
        this.dateOfScan = dateOfScan;
    }
}
