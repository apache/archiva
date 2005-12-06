package org.apache.maven.repository.reporting;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:jtolentino@mergere.com">John Tolentino</a>
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
        if ( queryConditions.isEmpty() )
        {
            return false;
        }
        else
        {
            boolean temp = ( (Boolean) iterator.next() ).booleanValue();
            return temp;
        }
    }

    public void addReturnValue( boolean queryCondition )
    {
        queryConditions.add( new Boolean( queryCondition ) );
    }

    public void clearList()
    {
        queryConditions.clear();
    }

    public boolean containsArtifact( Artifact artifact, Snapshot snapshot )
    {
        // TODO
        return containsArtifact( artifact );
    }
}
