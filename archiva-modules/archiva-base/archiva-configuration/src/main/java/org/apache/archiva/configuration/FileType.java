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
 * The FileType object.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class FileType
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field id.
     */
    private String id;

    /**
     * Field patterns.
     */
    private java.util.List<String> patterns;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addPattern.
     * 
     * @param string
     */
    public void addPattern( String string )
    {
        getPatterns().add( string );
    } //-- void addPattern( String )

    /**
     * Get the id field.
     * 
     * @return String
     */
    public String getId()
    {
        return this.id;
    } //-- String getId()

    /**
     * Method getPatterns.
     * 
     * @return List
     */
    public java.util.List<String> getPatterns()
    {
        if ( this.patterns == null )
        {
            this.patterns = new java.util.ArrayList<String>();
        }

        return this.patterns;
    } //-- java.util.List<String> getPatterns()

    /**
     * Method removePattern.
     * 
     * @param string
     */
    public void removePattern( String string )
    {
        getPatterns().remove( string );
    } //-- void removePattern( String )

    /**
     * Set the id field.
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId( String )

    /**
     * Set the patterns field.
     * 
     * @param patterns
     */
    public void setPatterns( java.util.List<String> patterns )
    {
        this.patterns = patterns;
    } //-- void setPatterns( java.util.List )

    

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

                FileType fileType = (FileType) o;

                if ( id != null ? !id.equals( fileType.id ) : fileType.id != null )
                {
                    return false;
                }

                return true;
            }

            @Override
            public int hashCode()
            {
                return id != null ? 37 + id.hashCode() : 0;
            }
          
}
