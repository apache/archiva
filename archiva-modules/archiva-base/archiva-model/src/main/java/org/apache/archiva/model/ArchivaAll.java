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
 * 
 *         This object is only used for the XML backup / restore
 * features of Archiva.
 *         This object is not serialized to the Database.
 *       
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ArchivaAll
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field artifacts.
     */
    private java.util.List<ArchivaArtifactModel> artifacts;

    /**
     * Field repositoryMetadata.
     */
    private java.util.List<ArchivaRepositoryMetadata> repositoryMetadata;

    /**
     * Field modelEncoding.
     */
    private String modelEncoding = "UTF-8";


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addArtifact.
     * 
     * @param archivaArtifactModel
     */
    public void addArtifact( ArchivaArtifactModel archivaArtifactModel )
    {
        getArtifacts().add( archivaArtifactModel );
    } //-- void addArtifact( ArchivaArtifactModel )

    /**
     * Method addRepositoryMetadata.
     * 
     * @param archivaRepositoryMetadata
     */
    public void addRepositoryMetadata( ArchivaRepositoryMetadata archivaRepositoryMetadata )
    {
        getRepositoryMetadata().add( archivaRepositoryMetadata );
    } //-- void addRepositoryMetadata( ArchivaRepositoryMetadata )

    /**
     * Method getArtifacts.
     * 
     * @return List
     */
    public java.util.List<ArchivaArtifactModel> getArtifacts()
    {
        if ( this.artifacts == null )
        {
            this.artifacts = new java.util.ArrayList<ArchivaArtifactModel>();
        }

        return this.artifacts;
    } //-- java.util.List<ArchivaArtifactModel> getArtifacts()

    /**
     * Get the modelEncoding field.
     * 
     * @return String
     */
    public String getModelEncoding()
    {
        return this.modelEncoding;
    } //-- String getModelEncoding()

    /**
     * Method getRepositoryMetadata.
     * 
     * @return List
     */
    public java.util.List<ArchivaRepositoryMetadata> getRepositoryMetadata()
    {
        if ( this.repositoryMetadata == null )
        {
            this.repositoryMetadata = new java.util.ArrayList<ArchivaRepositoryMetadata>();
        }

        return this.repositoryMetadata;
    } //-- java.util.List<ArchivaRepositoryMetadata> getRepositoryMetadata()

    /**
     * Method removeArtifact.
     * 
     * @param archivaArtifactModel
     */
    public void removeArtifact( ArchivaArtifactModel archivaArtifactModel )
    {
        getArtifacts().remove( archivaArtifactModel );
    } //-- void removeArtifact( ArchivaArtifactModel )

    /**
     * Method removeRepositoryMetadata.
     * 
     * @param archivaRepositoryMetadata
     */
    public void removeRepositoryMetadata( ArchivaRepositoryMetadata archivaRepositoryMetadata )
    {
        getRepositoryMetadata().remove( archivaRepositoryMetadata );
    } //-- void removeRepositoryMetadata( ArchivaRepositoryMetadata )

    /**
     * Set the artifacts field.
     * 
     * @param artifacts
     */
    public void setArtifacts( java.util.List<ArchivaArtifactModel> artifacts )
    {
        this.artifacts = artifacts;
    } //-- void setArtifacts( java.util.List )

    /**
     * Set the modelEncoding field.
     * 
     * @param modelEncoding
     */
    public void setModelEncoding( String modelEncoding )
    {
        this.modelEncoding = modelEncoding;
    } //-- void setModelEncoding( String )

    /**
     * Set the repositoryMetadata field.
     * 
     * @param repositoryMetadata
     */
    public void setRepositoryMetadata( java.util.List<ArchivaRepositoryMetadata> repositoryMetadata )
    {
        this.repositoryMetadata = repositoryMetadata;
    } //-- void setRepositoryMetadata( java.util.List )

    
    private static final long serialVersionUID = 3259707008803111764L;
          
}
