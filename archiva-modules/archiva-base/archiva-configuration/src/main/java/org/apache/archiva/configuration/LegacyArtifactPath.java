package org.apache.archiva.configuration;

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
 * Class LegacyArtifactPath.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class LegacyArtifactPath
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The legacy path.
     *           
     */
    private String path;

    /**
     * 
     *             The artifact reference, as " [groupId] :
     * [artifactId] : [version] : [classifier] : [type] ".
     *           
     */
    private String artifact;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the artifact reference, as " [groupId] : [artifactId] :
     * [version] : [classifier] : [type] ".
     * 
     * @return String
     */
    public String getArtifact()
    {
        return this.artifact;
    } //-- String getArtifact()

    /**
     * Get the legacy path.
     * 
     * @return String
     */
    public String getPath()
    {
        return this.path;
    } //-- String getPath()

    /**
     * Set the artifact reference, as " [groupId] : [artifactId] :
     * [version] : [classifier] : [type] ".
     * 
     * @param artifact
     */
    public void setArtifact( String artifact )
    {
        this.artifact = artifact;
    } //-- void setArtifact( String )

    /**
     * Set the legacy path.
     * 
     * @param path
     */
    public void setPath( String path )
    {
        this.path = path;
    } //-- void setPath( String )

    
    public boolean match( String path )
    {
        return path.equals( this.path );
    }

    public String getGroupId()
    {
        return artifact.split( ":" )[0];
    }

    public String getArtifactId()
    {
        return artifact.split( ":" )[1];
    }
        
    public String getVersion()
    {
        return artifact.split( ":" )[2];
    }
    
    public String getClassifier()
    {
        String classifier = artifact.split( ":" )[3];
        return classifier.length() > 0 ? classifier : null;
    }
    
    public String getType()
    {
        return artifact.split( ":" )[4];
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        LegacyArtifactPath that = (LegacyArtifactPath) o;

        if ( path != null ? !path.equals( that.path ) : that.path != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return path != null ? 37 + path.hashCode() : 0;
    }
       
}
