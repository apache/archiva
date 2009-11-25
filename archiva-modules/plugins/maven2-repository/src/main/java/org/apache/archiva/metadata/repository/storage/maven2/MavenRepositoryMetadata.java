package org.apache.archiva.metadata.repository.storage.maven2;

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

import java.util.ArrayList;
import java.util.List;

public class MavenRepositoryMetadata
{
    private String groupId;

    private String artifactId;

    private String version;

    private String lastUpdated;

    private String latestVersion;

    private String releasedVersion;

    private List<String> availableVersions;

    private Snapshot snapshotVersion;

    private List<Plugin> plugins = new ArrayList<Plugin>();

    public List<Plugin> getPlugins()
    {
        return plugins;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public void setLastUpdated( String lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    }

    public void setLatestVersion( String latestVersion )
    {
        this.latestVersion = latestVersion;
    }

    public void setReleasedVersion( String releasedVersion )
    {
        this.releasedVersion = releasedVersion;
    }

    public void setAvailableVersions( List<String> availableVersions )
    {
        this.availableVersions = availableVersions;
    }

    public void setSnapshotVersion( Snapshot snapshotVersion )
    {
        this.snapshotVersion = snapshotVersion;
    }

    public void addPlugin( Plugin plugin )
    {
        this.plugins.add( plugin );
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

    public String getLastUpdated()
    {
        return lastUpdated;
    }

    public String getLatestVersion()
    {
        return latestVersion;
    }

    public String getReleasedVersion()
    {
        return releasedVersion;
    }

    public List<String> getAvailableVersions()
    {
        return availableVersions;
    }

    public Snapshot getSnapshotVersion()
    {
        return snapshotVersion;
    }

    public static class Snapshot
    {
        private String timestamp;

        private int buildNumber;

        public void setTimestamp( String timestamp )
        {
            this.timestamp = timestamp;
        }

        public void setBuildNumber( int buildNumber )
        {
            this.buildNumber = buildNumber;
        }

        public int getBuildNumber()
        {
            return buildNumber;
        }

        public String getTimestamp()
        {
            return timestamp;
        }
    }

    public static class Plugin
    {
        private String prefix;

        private String artifactId;

        private String name;

        public void setPrefix( String prefix )
        {
            this.prefix = prefix;
        }

        public void setArtifactId( String artifactId )
        {
            this.artifactId = artifactId;
        }

        public void setName( String name )
        {
            this.name = name;
        }

        public String getPrefix()
        {
            return prefix;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public String getName()
        {
            return name;
        }
    }
}
