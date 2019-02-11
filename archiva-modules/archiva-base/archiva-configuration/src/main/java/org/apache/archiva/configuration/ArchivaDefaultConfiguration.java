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
 * 
 *       Archiva default settings.
 *     
 * 
 * @version $Revision$ $Date$
 */
@SuppressWarnings( "all" )
public class ArchivaDefaultConfiguration
    implements java.io.Serializable
{

      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    /**
     * Field defaultCheckPaths.
     */
    private java.util.List<RepositoryCheckPath> defaultCheckPaths;


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Method addDefaultCheckPath.
     * 
     * @param repositoryCheckPath
     */
    public void addDefaultCheckPath( RepositoryCheckPath repositoryCheckPath )
    {
        getDefaultCheckPaths().add( repositoryCheckPath );
    } //-- void addDefaultCheckPath( RepositoryCheckPath )

    /**
     * Method getDefaultCheckPaths.
     * 
     * @return List
     */
    public java.util.List<RepositoryCheckPath> getDefaultCheckPaths()
    {
        if ( this.defaultCheckPaths == null )
        {
            this.defaultCheckPaths = new java.util.ArrayList<RepositoryCheckPath>();
        }

        return this.defaultCheckPaths;
    } //-- java.util.List<RepositoryCheckPath> getDefaultCheckPaths()

    /**
     * Method removeDefaultCheckPath.
     * 
     * @param repositoryCheckPath
     */
    public void removeDefaultCheckPath( RepositoryCheckPath repositoryCheckPath )
    {
        getDefaultCheckPaths().remove( repositoryCheckPath );
    } //-- void removeDefaultCheckPath( RepositoryCheckPath )

    /**
     * Set the default check paths for certain remote repositories.
     * 
     * @param defaultCheckPaths
     */
    public void setDefaultCheckPaths( java.util.List<RepositoryCheckPath> defaultCheckPaths )
    {
        this.defaultCheckPaths = defaultCheckPaths;
    } //-- void setDefaultCheckPaths( java.util.List )

}
