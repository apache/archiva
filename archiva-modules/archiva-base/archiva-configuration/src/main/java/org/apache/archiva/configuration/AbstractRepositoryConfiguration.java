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
 * Class AbstractRepositoryConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class AbstractRepositoryConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * 
     *             The repository identifier.
     *           
     */
    private String id;

    /**
     * 
     *             The repository type. Currently only MAVEN type
     * is known.
     *           
     */
    private String type = "MAVEN";

    /**
     * 
     *             The descriptive name of the repository.
     *           
     */
    private String name;

    /**
     * 
     *             The layout of the repository. Valid values are
     * "default" and "legacy".
     *           
     */
    private String layout = "default";

    /**
     * 
     *             The directory for the indexes of this
     * repository.
     *           
     */
    private String indexDir = "";

    /**
     * 
     *             The directory for the packed indexes of this
     * repository.
     *           
     */
    private String packedIndexDir = "";

    /**
     * 
     *             The description of this repository.
     *           
     */
    private String description = "";


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Get the description of this repository.
     * 
     * @return String
     */
    public String getDescription()
    {
        return this.description;
    } //-- String getDescription()

    /**
     * Get the repository identifier.
     * 
     * @return String
     */
    public String getId()
    {
        return this.id;
    } //-- String getId()

    /**
     * Get the directory for the indexes of this repository.
     * 
     * @return String
     */
    public String getIndexDir()
    {
        return this.indexDir;
    } //-- String getIndexDir()

    /**
     * Get the layout of the repository. Valid values are "default"
     * and "legacy".
     * 
     * @return String
     */
    public String getLayout()
    {
        return this.layout;
    } //-- String getLayout()

    /**
     * Get the descriptive name of the repository.
     * 
     * @return String
     */
    public String getName()
    {
        return this.name;
    } //-- String getName()

    /**
     * Get the directory for the packed indexes of this repository.
     * 
     * @return String
     */
    public String getPackedIndexDir()
    {
        return this.packedIndexDir;
    } //-- String getPackedIndexDir()

    /**
     * Get the repository type. Currently only MAVEN type is known.
     * 
     * @return String
     */
    public String getType()
    {
        return this.type;
    } //-- String getType()

    /**
     * Set the description of this repository.
     * 
     * @param description
     */
    public void setDescription( String description )
    {
        this.description = description;
    } //-- void setDescription( String )

    /**
     * Set the repository identifier.
     * 
     * @param id
     */
    public void setId( String id )
    {
        this.id = id;
    } //-- void setId( String )

    /**
     * Set the directory for the indexes of this repository.
     * 
     * @param indexDir
     */
    public void setIndexDir( String indexDir )
    {
        this.indexDir = indexDir;
    } //-- void setIndexDir( String )

    /**
     * Set the layout of the repository. Valid values are "default"
     * and "legacy".
     * 
     * @param layout
     */
    public void setLayout( String layout )
    {
        this.layout = layout;
    } //-- void setLayout( String )

    /**
     * Set the descriptive name of the repository.
     * 
     * @param name
     */
    public void setName( String name )
    {
        this.name = name;
    } //-- void setName( String )

    /**
     * Set the directory for the packed indexes of this repository.
     * 
     * @param packedIndexDir
     */
    public void setPackedIndexDir( String packedIndexDir )
    {
        this.packedIndexDir = packedIndexDir;
    } //-- void setPackedIndexDir( String )

    /**
     * Set the repository type. Currently only MAVEN type is known.
     * 
     * @param type
     */
    public void setType( String type )
    {
        this.type = type;
    } //-- void setType( String )

    
            public int hashCode()
            {
                int result = 17;
                result = 37 * result + ( id != null ? id.hashCode() : 0 );
                return result;
            }

            public boolean equals( Object other )
            {
                if ( this == other )
                {
                    return true;
                }

                if ( !( other instanceof AbstractRepositoryConfiguration ) )
                {
                    return false;
                }

                AbstractRepositoryConfiguration that = (AbstractRepositoryConfiguration) other;
                boolean result = true;
                result = result && ( getId() == null ? that.getId() == null : getId().equals( that.getId() ) );
                return result;
            }
       
}
