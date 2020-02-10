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
 * A reference to another Versioned Project.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class VersionedReference
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


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the Artifact ID of the repository content.
     * 
     * @return String
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId()

    public VersionedReference artifactId(String artifactId) {
        this.artifactId  = artifactId;
        return this;
    }

    /**
     * Get the Group ID of the repository content.
     * 
     * @return String
     */
    public String getGroupId()
    {
        return this.groupId;
    } //-- String getGroupId()

    public VersionedReference groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Get the version of the repository content.
     * 
     * @return String
     */
    public String getVersion()
    {
        return this.version;
    } //-- String getVersion()

    public VersionedReference version(String version) {
        this.version = version;
        return this;
    }

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
     * Set the Group ID of the repository content.
     * 
     * @param groupId
     */
    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    } //-- void setGroupId( String )

    /**
     * Set the version of the repository content.
     * 
     * @param version
     */
    public void setVersion( String version )
    {
        this.version = version;
    } //-- void setVersion( String )

    
    private static final long serialVersionUID = -6990353165677563113L;
          
    
    private static String defaultString( String value )
    {
        if ( value == null )
        {
            return "";
        }
        
        return value.trim();
    }
          
    public static String toKey( VersionedReference reference )
    {
        StringBuilder key = new StringBuilder();

        key.append( defaultString( reference.getGroupId() ) ).append( ":" );
        key.append( defaultString( reference.getArtifactId() ) ).append( ":" );
        key.append( defaultString( reference.getVersion() ) );

        return key.toString();
    }
          
}
