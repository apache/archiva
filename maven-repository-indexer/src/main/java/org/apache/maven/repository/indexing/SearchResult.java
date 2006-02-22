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

import org.apache.maven.artifact.Artifact;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the object type contained in the list that will be returned by the
 * RepositoryIndexSearchLayer to the action class
 */
public class SearchResult
{
    private Artifact artifact;

    private Map fieldMatches;

    /**
     * Class constructor
     */
    public SearchResult()
    {
        fieldMatches = new HashMap();
    }

    /**
     * Getter method for artifact
     *
     * @return Artifact
     */
    public Artifact getArtifact()
    {
        return artifact;
    }

    /**
     * Setter method for artifact
     *
     * @param artifact
     */
    public void setArtifact( Artifact artifact )
    {
        this.artifact = artifact;
    }

    /**
     * Getter method for fieldMatches
     *
     * @return Map
     */
    public Map getFieldMatches()
    {
        return fieldMatches;
    }

    /**
     * Setter method for fieldMatches
     *
     * @param fieldMatches
     */
    public void setFieldMatches( Map fieldMatches )
    {
        this.fieldMatches = fieldMatches;
    }
}
