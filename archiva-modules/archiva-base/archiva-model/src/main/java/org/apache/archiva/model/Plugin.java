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
 * The Plugin.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class Plugin
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The prefix for a plugin
     *           .
     */
    private String prefix;

    /**
     * 
     *             The artifactId for a plugin
     *           .
     */
    private String artifactId;

    /**
     * 
     *             The name for a plugin
     *           .
     */
    private String name;


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

        if ( !( other instanceof Plugin ) )
        {
            return false;
        }

        Plugin that = (Plugin) other;
        boolean result = true;

        result = result && ( getArtifactId() == null ? that.getArtifactId() == null : getArtifactId().equals( that.getArtifactId() ) );

        return result;
    } //-- boolean equals( Object )

    /**
     * Get the artifactId for a plugin.
     * 
     * @return String
     */
    public String getArtifactId()
    {
        return this.artifactId;
    } //-- String getArtifactId()

    /**
     * Get the name for a plugin.
     * 
     * @return String
     */
    public String getName()
    {
        return this.name;
    } //-- String getName()

    /**
     * Get the prefix for a plugin.
     * 
     * @return String
     */
    public String getPrefix()
    {
        return this.prefix;
    } //-- String getPrefix()

    /**
     * Method hashCode.
     * 
     * @return int
     */
    public int hashCode()
    {
        int result = 17;

        result = 37 * result + ( artifactId != null ? artifactId.hashCode() : 0 );

        return result;
    } //-- int hashCode()

    /**
     * Set the artifactId for a plugin.
     * 
     * @param artifactId
     */
    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    } //-- void setArtifactId( String )

    /**
     * Set the name for a plugin.
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    } //-- void setName( String )

    /**
     * Set the prefix for a plugin.
     * 
     * @param prefix
     */
    public void setPrefix( String prefix )
    {
        this.prefix = prefix;
    } //-- void setPrefix( String )

    /**
     * Method toString.
     * 
     * @return String
     */
    public java.lang.String toString()
    {
        StringBuilder buf = new StringBuilder( 128 );

        buf.append( "artifactId = '" );
        buf.append( getArtifactId() );
        buf.append( "'" );

        return buf.toString();
    } //-- java.lang.String toString()

}
