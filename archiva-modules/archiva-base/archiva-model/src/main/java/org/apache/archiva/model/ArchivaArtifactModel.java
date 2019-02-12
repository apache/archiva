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
 * Class ArchivaArtifactModel.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ArchivaArtifactModel
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The Group ID of the repository content.
     *           
     */
    private String groupId;

    /**
     * 
     *             The Artifact ID of the repository content.
     *           
     */
    private String artifactId;

    /**
     * 
     *             The version of the repository content.
     *           
     */
    private String version;

    /**
     * 
     *             The classifier for this artifact.
     *           
     */
    private String classifier;

    /**
     * 
     *             The type of artifact.
     *           
     */
    private String type;

    /**
     * 
     *             The repository associated with this content.
     *           
     */
    private String repositoryId;

    /**
     * 
     *             True if this is a snapshot.
     *           
     */
    private boolean snapshot = false;

    /**
     * 
     *             The MD5 checksum for the artifact file.
     *           
     */
    private String checksumMD5;

    /**
     * 
     *             The SHA1 checksum for the artifact file.
     *           
     */
    private String checksumSHA1;

    /**
     * 
     *             The Last Modified Timestamp of this artifact.
     *           
     */
    private java.util.Date lastModified;

    /**
     * 
     *             The size of the artifact on disk.
     *           
     */
    private long size = 0L;

    /**
     * 
     *             When this artifact was gathered or discovered
     * from the repository.
     *           
     */
    private java.util.Date whenGathered;


      //-----------/
     //- Methods -/
    //-----------/

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

        if ( !( other instanceof ArchivaArtifactModel ) )
        {
            return false;
        }

        ArchivaArtifactModel that = (ArchivaArtifactModel) other;
        boolean result = true;

        result = result && ( getGroupId() == null ? that.getGroupId() == null : getGroupId().equals( that.getGroupId() ) );
        result = result && ( getArtifactId() == null ? that.getArtifactId() == null : getArtifactId().equals( that.getArtifactId() ) );
        result = result && ( getVersion() == null ? that.getVersion() == null : getVersion().equals( that.getVersion() ) );
        result = result && ( getClassifier() == null ? that.getClassifier() == null : getClassifier().equals( that.getClassifier() ) );
        result = result && ( getType() == null ? that.getType() == null : getType().equals( that.getType() ) );
        result = result && ( getRepositoryId() == null ? that.getRepositoryId() == null : getRepositoryId().equals( that.getRepositoryId() ) );

        return result;
    } //-- boolean equals( Object )

    /**
     * Get the Artifact ID of the repository content.
     * 
     * @return String
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId()

    /**
     * Get the MD5 checksum for the artifact file.
     * 
     * @return String
     */
    public String getChecksumMD5()
    {
        return this.checksumMD5;
    } //-- String getChecksumMD5()

    /**
     * Get the SHA1 checksum for the artifact file.
     * 
     * @return String
     */
    public String getChecksumSHA1()
    {
        return this.checksumSHA1;
    } //-- String getChecksumSHA1()

    /**
     * Get the classifier for this artifact.
     * 
     * @return String
     */
    public String getClassifier()
    {
        return this.classifier;
    } //-- String getClassifier()

    /**
     * Get the Group ID of the repository content.
     * 
     * @return String
     */
    public String getGroupId()
    {
        return this.groupId;
    } //-- String getGroupId()

    /**
     * Get the Last Modified Timestamp of this artifact.
     * 
     * @return Date
     */
    public java.util.Date getLastModified()
    {
        return this.lastModified;
    } //-- java.util.Date getLastModified()

    /**
     * Get the repository associated with this content.
     * 
     * @return String
     */
    public String getRepositoryId()
    {
        return this.repositoryId;
    } //-- String getRepositoryId()

    /**
     * Get the size of the artifact on disk.
     * 
     * @return long
     */
    public long getSize()
    {
        return this.size;
    } //-- long getSize()

    /**
     * Get the type of artifact.
     * 
     * @return String
     */
    public String getType()
    {
        return this.type;
    } //-- String getType()

    /**
     * Get the version of the repository content.
     * 
     * @return String
     */
    public String getVersion()
    {
        return this.version;
    } //-- String getVersion()

    /**
     * Get when this artifact was gathered or discovered from the
     * repository.
     * 
     * @return Date
     */
    public java.util.Date getWhenGathered()
    {
        return this.whenGathered;
    } //-- java.util.Date getWhenGathered()

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
        result = 37 * result + ( classifier != null ? classifier.hashCode() : 0 );
        result = 37 * result + ( type != null ? type.hashCode() : 0 );
        result = 37 * result + ( repositoryId != null ? repositoryId.hashCode() : 0 );

        return result;
    } //-- int hashCode()

    /**
     * Get true if this is a snapshot.
     * 
     * @return boolean
     */
    public boolean isSnapshot()
    {
        return this.snapshot;
    } //-- boolean isSnapshot()

    /**
     * Set the Artifact ID of the repository content.
     * 
     * @param artifactId
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    } //-- void setArtifactId( String )

    /**
     * Set the MD5 checksum for the artifact file.
     * 
     * @param checksumMD5
     */
    public void setChecksumMD5( String checksumMD5 )
    {
        this.checksumMD5 = checksumMD5;
    } //-- void setChecksumMD5( String )

    /**
     * Set the SHA1 checksum for the artifact file.
     * 
     * @param checksumSHA1
     */
    public void setChecksumSHA1( String checksumSHA1 )
    {
        this.checksumSHA1 = checksumSHA1;
    } //-- void setChecksumSHA1( String )

    /**
     * Set the classifier for this artifact.
     * 
     * @param classifier
     */
    public void setClassifier( String classifier )
    {
        this.classifier = classifier;
    } //-- void setClassifier( String )

    /**
     * Set the Group ID of the repository content.
     * 
     * @param groupId
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    } //-- void setGroupId( String )

    /**
     * Set the Last Modified Timestamp of this artifact.
     * 
     * @param lastModified
     */
    public void setLastModified( java.util.Date lastModified )
    {
        this.lastModified = lastModified;
    } //-- void setLastModified( java.util.Date )

    /**
     * Set the repository associated with this content.
     * 
     * @param repositoryId
     */
    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    } //-- void setRepositoryId( String )

    /**
     * Set the size of the artifact on disk.
     * 
     * @param size
     */
    public void setSize( long size )
    {
        this.size = size;
    } //-- void setSize( long )

    /**
     * Set true if this is a snapshot.
     * 
     * @param snapshot
     */
    public void setSnapshot( boolean snapshot )
    {
        this.snapshot = snapshot;
    } //-- void setSnapshot( boolean )

    /**
     * Set the type of artifact.
     * 
     * @param type
     */
    public void setType( String type )
    {
        this.type = type;
    } //-- void setType( String )

    /**
     * Set the version of the repository content.
     * 
     * @param version
     */
    public void setVersion( String version )
    {
        this.version = version;
    } //-- void setVersion( String )

    /**
     * Set when this artifact was gathered or discovered from the
     * repository.
     * 
     * @param whenGathered
     */
    public void setWhenGathered( java.util.Date whenGathered )
    {
        this.whenGathered = whenGathered;
    } //-- void setWhenGathered( java.util.Date )

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
        buf.append( "\n" ); 
        buf.append( "classifier = '" );
        buf.append( getClassifier() );
        buf.append( "'" );
        buf.append( "\n" ); 
        buf.append( "type = '" );
        buf.append( getType() );
        buf.append( "'" );
        buf.append( "\n" ); 
        buf.append( "repositoryId = '" );
        buf.append( getRepositoryId() );
        buf.append( "'" );

        return buf.toString();
    } //-- java.lang.String toString()

    
    private static final long serialVersionUID = -6292417108113887384L;
          
}
