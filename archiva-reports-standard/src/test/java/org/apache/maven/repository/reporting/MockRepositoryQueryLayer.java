package org.apache.maven.repository.reporting;

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
import org.apache.maven.artifact.repository.metadata.Snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 
 */
public class MockRepositoryQueryLayer
    implements RepositoryQueryLayer
{
    private List queryConditions;

    private Iterator iterator;

    public MockRepositoryQueryLayer()
    {
        queryConditions = new ArrayList();
    }

    public boolean containsArtifact( Artifact artifact )
    {
        if ( iterator == null || !iterator.hasNext() ) // not initialized or reached end of the list. start again
        {
            iterator = queryConditions.iterator();
        }
        boolean b;
        if ( queryConditions.isEmpty() )
        {
            b = false;
        }
        else
        {
            b = ( (Boolean) iterator.next() ).booleanValue();
        }
        return b;
    }

    public void addReturnValue( boolean queryCondition )
    {
        queryConditions.add( Boolean.valueOf( queryCondition ) );
    }

    public void clearList()
    {
        queryConditions.clear();
    }

    public boolean containsArtifact( Artifact artifact, Snapshot snapshot )
    {
        return containsArtifact( artifact );
    }

    public List getVersions( Artifact artifact )
    {
        return Collections.EMPTY_LIST;
    }
}
