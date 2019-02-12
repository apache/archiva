package org.apache.archiva.model;

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
 * Class ArchivaRepositoryMetadata.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ArchivaRepositoryMetadata
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The Group ID of the metadata.
     *           
     */
    private String groupId;

    /**
     * 
     *             The Artifact ID of the metadata.
     *           
     */
    private String artifactId;

    /**
     * 
     *             The Version of the metadata.
     *           
     */
    private String version;

    /**
     * 
     *             The latest version id.
     *           
     */
    private String latestVersion;

    /**
     * 
     *             The released version id.
     *           
     */
    private String releasedVersion;

    /**
     * 
     *             The snapshot version id.
     *           
     */
    private SnapshotVersion snapshotVersion;

    /**
     * Field plugins.
     */
    private java.util.List<Plugin> plugins;

    /**
     * Field availableVersions.
     */
    private java.util.List<String> availableVersions;

    /**
     * 
     *             When the metadata was last updated.
     *           
     */
    private String lastUpdated;

    /**
     * 
     *             The Last Modified Timestamp of this file.
     *           
     */
    private java.util.Date fileLastModified;

    /**
     * 
     *             The size of the artifact on disk.
     *           
     */
    private long fileSize = 0L;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addAvailableVersion.
     * 
     * @param string
     */
    public void addAvailableVersion( String string )
    {
        getAvailableVersions().add( string );
    } //-- void addAvailableVersion( String )

    /**
     * Method addPlugin.
     * 
     * @param plugin
     */
    public void addPlugin( Plugin plugin )
    {
        getPlugins().add( plugin );
    } //-- void addPlugin( Plugin )

    /**
     * Method equals.
     * 
     * @param other
     * @return boolean
     */
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof ArchivaRepositoryMetadata ) )
        {
            return false;
        }

        ArchivaRepositoryMetadata that = (ArchivaRepositoryMetadata) other;
        boolean result = true;

        result = result && ( getGroupId() == null ? that.getGroupId() == null : getGroupId().equals( that.getGroupId() ) );
        result = result && ( getArtifactId() == null ? that.getArtifactId() == null : getArtifactId().equals( that.getArtifactId() ) );
        result = result && ( getVersion() == null ? that.getVersion() == null : getVersion().equals( that.getVersion() ) );

        return result;
    } //-- boolean equals( Object )

    /**
     * Get the Artifact ID of the metadata.
     * 
     * @return String
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId()

    /**
     * Method getAvailableVersions.
     * 
     * @return List
     */
    public java.util.List<String> getAvailableVersions()
    {
        if ( this.availableVersions == null )
        {
            this.availableVersions = new java.util.ArrayList<String>();
        }

        return this.availableVersions;
    } //-- java.util.List<String> getAvailableVersions()

    /**
     * Get the Last Modified Timestamp of this file.
     * 
     * @return Date
     */
    public java.util.Date getFileLastModified()
    {
        return this.fileLastModified;
    } //-- java.util.Date getFileLastModified()

    /**
     * Get the size of the artifact on disk.
     * 
     * @return long
     */
    public long getFileSize()
    {
        return this.fileSize;
    } //-- long getFileSize()

    /**
     * Get the Group ID of the metadata.
     * 
     * @return String
     */
    public String getGroupId()
    {
        return this.groupId;
    } //-- String getGroupId()

    /**
     * Get when the metadata was last updated.
     * 
     * @return String
     */
    public String getLastUpdated()
    {
        return this.lastUpdated;
    } //-- String getLastUpdated()

    /**
     * Get the latest version id.
     * 
     * @return String
     */
    public String getLatestVersion()
    {
        return this.latestVersion;
    } //-- String getLatestVersion()

    /**
     * Method getPlugins.
     * 
     * @return List
     */
    public java.util.List<Plugin> getPlugins()
    {
        if ( this.plugins == null )
        {
            this.plugins = new java.util.ArrayList<Plugin>();
        }

        return this.plugins;
    } //-- java.util.List<Plugin> getPlugins()

    /**
     * Get the released version id.
     * 
     * @return String
     */
    public String getReleasedVersion()
    {
        return this.releasedVersion;
    } //-- String getReleasedVersion()

    /**
     * Get the snapshot version id.
     * 
     * @return SnapshotVersion
     */
    public SnapshotVersion getSnapshotVersion()
    {
        return this.snapshotVersion;
    } //-- SnapshotVersion getSnapshotVersion()

    /**
     * Get the Version of the metadata.
     * 
     * @return String
     */
    public String getVersion()
    {
        return this.version;
    } //-- String getVersion()

    /**
     * Method hashCode.
     * 
     * @return int
     */
    public int hashCode()
    {
        int result = 17;

        result = 37 * result + ( groupId != null ? groupId.hashCode() : 0 );
        result = 37 * result + ( artifactId != null ? artifactId.hashCode() : 0 );
        result = 37 * result + ( version != null ? version.hashCode() : 0 );

        return result;
    } //-- int hashCode()

    /**
     * Method removeAvailableVersion.
     * 
     * @param string
     */
    public void removeAvailableVersion( String string )
    {
        getAvailableVersions().remove( string );
    } //-- void removeAvailableVersion( String )

    /**
     * Method removePlugin.
     * 
     * @param plugin
     */
    public void removePlugin( Plugin plugin )
    {
        getPlugins().remove( plugin );
    } //-- void removePlugin( Plugin )

    /**
     * Set the Artifact ID of the metadata.
     * 
     * @param artifactId
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    } //-- void setArtifactId( String )

    /**
     * Set the list of available version ids.
     * 
     * @param availableVersions
     */
    public void setAvailableVersions( java.util.List<String> availableVersions )
    {
        this.availableVersions = availableVersions;
    } //-- void setAvailableVersions( java.util.List )

    /**
     * Set the Last Modified Timestamp of this file.
     * 
     * @param fileLastModified
     */
    public void setFileLastModified( java.util.Date fileLastModified )
    {
        this.fileLastModified = fileLastModified;
    } //-- void setFileLastModified( java.util.Date )

    /**
     * Set the size of the artifact on disk.
     * 
     * @param fileSize
     */
    public void setFileSize( long fileSize )
    {
        this.fileSize = fileSize;
    } //-- void setFileSize( long )

    /**
     * Set the Group ID of the metadata.
     * 
     * @param groupId
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    } //-- void setGroupId( String )

    /**
     * Set when the metadata was last updated.
     * 
     * @param lastUpdated
     */
    public void setLastUpdated( String lastUpdated )
    {
        this.lastUpdated = lastUpdated;
    } //-- void setLastUpdated( String )

    /**
     * Set the latest version id.
     * 
     * @param latestVersion
     */
    public void setLatestVersion( String latestVersion )
    {
        this.latestVersion = latestVersion;
    } //-- void setLatestVersion( String )

    /**
     * Set the available plugins.
     * 
     * @param plugins
     */
    public void setPlugins( java.util.List<Plugin> plugins )
    {
        this.plugins = plugins;
    } //-- void setPlugins( java.util.List )

    /**
     * Set the released version id.
     * 
     * @param releasedVersion
     */
    public void setReleasedVersion( String releasedVersion )
    {
        this.releasedVersion = releasedVersion;
    } //-- void setReleasedVersion( String )

    /**
     * Set the snapshot version id.
     * 
     * @param snapshotVersion
     */
    public void setSnapshotVersion( SnapshotVersion snapshotVersion )
    {
        this.snapshotVersion = snapshotVersion;
    } //-- void setSnapshotVersion( SnapshotVersion )

    /**
     * Set the Version of the metadata.
     * 
     * @param version
     */
    public void setVersion( String version )
    {
        this.version = version;
    } //-- void setVersion( String )

    /**
     * Method toString.
     * 
     * @return String
     */
    public java.lang.String toString()
    {
        StringBuilder buf = new StringBuilder( 128 );

        buf.append( "groupId = '" );
        buf.append( getGroupId() );
        buf.append( "'" );
        buf.append( "\n" ); 
        buf.append( "artifactId = '" );
        buf.append( getArtifactId() );
        buf.append( "'" );
        buf.append( "\n" ); 
        buf.append( "version = '" );
        buf.append( getVersion() );
        buf.append( "'" );

        return buf.toString();
    } //-- java.lang.String toString()

    
    private static final long serialVersionUID = 914715358219606100L;
          
    
    public void updateTimestamp()
    {
        setLastUpdatedTimestamp( new java.util.Date() );
    }

    public void setLastUpdatedTimestamp( java.util.Date date )
    {
        java.util.TimeZone timezone = java.util.TimeZone.getTimeZone( "UTC" );
        java.text.DateFormat fmt = new java.text.SimpleDateFormat( "yyyyMMddHHmmss" );
        fmt.setTimeZone( timezone );
        setLastUpdated( fmt.format( date ) );
    }
          
}
