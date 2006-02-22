package org.apache.maven.repository.indexing;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * This class is the object type contained in the list returned by the DefaultRepositoryIndexSearcher
 */
public class RepositoryIndexSearchHit
{
    private Object obj;

    private boolean isHashMap = false;

    private boolean isMetadata = false;

    private boolean isModel = false;

    /**
     * Class constructor
     *
     * @param isHashMap  indicates whether the object is a HashMap object
     * @param isMetadata indicates whether the object is a RepositoryMetadata object
     * @param isModel    indicates whether the object is a Model object
     */
    public RepositoryIndexSearchHit( boolean isHashMap, boolean isMetadata, boolean isModel )
    {
        this.isHashMap = isHashMap;
        this.isMetadata = isMetadata;
        this.isModel = isModel;
    }

    /**
     * Getter method for obj variable
     *
     * @return the Object
     */
    public Object getObject()
    {
        return obj;
    }

    /**
     * Setter method for obj variable
     *
     * @param obj
     */
    public void setObject( Object obj )
    {
        this.obj = obj;
    }

    /**
     * Method that indicates if the object is a HashMap
     *
     * @return boolean
     */
    public boolean isHashMap()
    {
        return isHashMap;
    }

    /**
     * Method that indicates if the object is a RepositoryMetadata
     *
     * @return boolean
     */
    public boolean isMetadata()
    {
        return isMetadata;
    }

    /**
     * Method that indicates if the object is a Model
     *
     * @return boolean
     */
    public boolean isModel()
    {
        return isModel;
    }

}
