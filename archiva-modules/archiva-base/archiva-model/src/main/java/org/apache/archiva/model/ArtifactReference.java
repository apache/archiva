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
 * Class ArtifactReference.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ArtifactReference
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

    public ArtifactReference artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    /**
     * Get the classifier for this artifact.
     * 
     * @return String
     */
    public String getClassifier()
    {
        return this.classifier;
    } //-- String getClassifier()

    public ArtifactReference classifier(String classifier) {
        this.classifier = classifier;
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

    public ArtifactReference groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    /**
     * Get the type of artifact.
     * 
     * @return String
     */
    public String getType()
    {
        return this.type;
    } //-- String getType()

    public ArtifactReference type(String type) {
        this.type = type;
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

    public ArtifactReference version(String version) {
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

    
    private static final long serialVersionUID = -6116764846682178732L;
          
    
    private static String defaultString( String value )
    {
        if ( value == null )
        {
            return "";
        }
        
        return value.trim();
    }
          
    public static String toKey( ArtifactReference artifactReference )
    {
        StringBuilder key = new StringBuilder();

        key.append( defaultString( artifactReference.getGroupId() ) ).append( ":" );
        key.append( defaultString( artifactReference.getArtifactId() ) ).append( ":" );
        key.append( defaultString( artifactReference.getVersion() ) ).append( ":" );
        key.append( defaultString( artifactReference.getClassifier() ) ).append( ":" );
        key.append( defaultString( artifactReference.getType() ) );

        return key.toString();
    }

    public static String toVersionlessKey( ArtifactReference artifactReference )
    {
        StringBuilder key = new StringBuilder();

        key.append( defaultString( artifactReference.getGroupId() ) ).append( ":" );
        key.append( defaultString( artifactReference.getArtifactId() ) ).append( ":" );
        key.append( defaultString( artifactReference.getClassifier() ) ).append( ":" );
        key.append( defaultString( artifactReference.getType() ) );

        return key.toString();
    }
          
    
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = PRIME * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = PRIME * result + ( ( version == null ) ? 0 : version.hashCode() );
        result = PRIME * result + ( ( classifier == null ) ? 0 : classifier.hashCode() );
        result = PRIME * result + ( ( type == null ) ? 0 : type.hashCode() );
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        
        if ( obj == null )
        {
            return false;
        }
        
        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        final ArtifactReference other = (ArtifactReference) obj;

        if ( groupId == null )
        {
            if ( other.groupId != null )
            {
                return false;
            }
        }
        else if ( !groupId.equals( other.groupId ) )
        {
            return false;
        }

        if ( artifactId == null )
        {
            if ( other.artifactId != null )
            {
                return false;
            }
        }
        else if ( !artifactId.equals( other.artifactId ) )
        {
            return false;
        }

        if ( version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !version.equals( other.version ) )
        {
            return false;
        }

        if ( classifier == null )
        {
            if ( other.classifier != null )
            {
                return false;
            }
        }
        else if ( !classifier.equals( other.classifier ) )
        {
            return false;
        }
        
        if ( type == null )
        {
            if ( other.type != null )
            {
                return false;
            }
        }
        else if ( !type.equals( other.type ) )
        {
            return false;
        }
        
        return true;
    }          
          
}
