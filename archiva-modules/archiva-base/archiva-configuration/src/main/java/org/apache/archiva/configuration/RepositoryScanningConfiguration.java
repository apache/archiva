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
 * Class RepositoryScanningConfiguration.
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class RepositoryScanningConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field fileTypes.
     */
    private java.util.List<FileType> fileTypes;

    /**
     * Field knownContentConsumers.
     */
    private java.util.List<String> knownContentConsumers;

    /**
     * Field invalidContentConsumers.
     */
    private java.util.List<String> invalidContentConsumers;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addFileType.
     * 
     * @param fileType
     */
    public void addFileType( FileType fileType )
    {
        getFileTypes().add( fileType );
    } //-- void addFileType( FileType )

    /**
     * Method addInvalidContentConsumer.
     * 
     * @param string
     */
    public void addInvalidContentConsumer( String string )
    {
        getInvalidContentConsumers().add( string );
    } //-- void addInvalidContentConsumer( String )

    /**
     * Method addKnownContentConsumer.
     * 
     * @param string
     */
    public void addKnownContentConsumer( String string )
    {
        getKnownContentConsumers().add( string );
    } //-- void addKnownContentConsumer( String )

    /**
     * Method getFileTypes.
     * 
     * @return List
     */
    public java.util.List<FileType> getFileTypes()
    {
        if ( this.fileTypes == null )
        {
            this.fileTypes = new java.util.ArrayList<FileType>();
        }

        return this.fileTypes;
    } //-- java.util.List<FileType> getFileTypes()

    /**
     * Method getInvalidContentConsumers.
     * 
     * @return List
     */
    public java.util.List<String> getInvalidContentConsumers()
    {
        if ( this.invalidContentConsumers == null )
        {
            this.invalidContentConsumers = new java.util.ArrayList<String>();
        }

        return this.invalidContentConsumers;
    } //-- java.util.List<String> getInvalidContentConsumers()

    /**
     * Method getKnownContentConsumers.
     * 
     * @return List
     */
    public java.util.List<String> getKnownContentConsumers()
    {
        if ( this.knownContentConsumers == null )
        {
            this.knownContentConsumers = new java.util.ArrayList<String>();
        }

        return this.knownContentConsumers;
    } //-- java.util.List<String> getKnownContentConsumers()

    /**
     * Method removeFileType.
     * 
     * @param fileType
     */
    public void removeFileType( FileType fileType )
    {
        getFileTypes().remove( fileType );
    } //-- void removeFileType( FileType )

    /**
     * Method removeInvalidContentConsumer.
     * 
     * @param string
     */
    public void removeInvalidContentConsumer( String string )
    {
        getInvalidContentConsumers().remove( string );
    } //-- void removeInvalidContentConsumer( String )

    /**
     * Method removeKnownContentConsumer.
     * 
     * @param string
     */
    public void removeKnownContentConsumer( String string )
    {
        getKnownContentConsumers().remove( string );
    } //-- void removeKnownContentConsumer( String )

    /**
     * Set the FileTypes for the repository scanning configuration.
     * 
     * @param fileTypes
     */
    public void setFileTypes( java.util.List<FileType> fileTypes )
    {
        this.fileTypes = fileTypes;
    } //-- void setFileTypes( java.util.List )

    /**
     * Set the list of active consumer IDs for invalid content.
     * 
     * @param invalidContentConsumers
     */
    public void setInvalidContentConsumers( java.util.List<String> invalidContentConsumers )
    {
        this.invalidContentConsumers = invalidContentConsumers;
    } //-- void setInvalidContentConsumers( java.util.List )

    /**
     * Set the list of active consumers IDs for known content.
     * 
     * @param knownContentConsumers
     */
    public void setKnownContentConsumers( java.util.List<String> knownContentConsumers )
    {
        this.knownContentConsumers = knownContentConsumers;
    } //-- void setKnownContentConsumers( java.util.List )

}
