package org.apache.maven.archiva.plugins.dev.functors;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;

import java.util.Comparator;

/**
 * ArtifactComparator 
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArtifactComparator
    implements Comparator
{

    public int compare( Object arg0, Object arg1 )
    {
        if( arg0 == null || arg1 == null )
        {
            return -1;
        }
        
        Artifact artifact1 = (Artifact) arg0;
        Artifact artifact2 = (Artifact) arg1;
        
        int diff;
        
        diff = artifact1.getGroupId().compareTo( artifact2.getGroupId() );
        if( diff != 0 )
        {
            return diff;
        }
        
        diff = artifact1.getArtifactId().compareTo( artifact2.getArtifactId() );
        if( diff != 0 )
        {
            return diff;
        }
        
        return artifact1.getVersion().compareTo( artifact2.getVersion() );
    }

}
