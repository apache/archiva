package org.apache.maven.archiva.dependency.graph.functors;

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

import org.apache.commons.collections.Transformer;
import org.apache.maven.archiva.dependency.graph.DependencyGraphEdge;
import org.apache.maven.archiva.dependency.graph.DependencyGraphKeys;
import org.apache.maven.archiva.dependency.graph.DependencyGraphNode;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.Keys;

/**
 * ToKeyTransformer 
 *
 * @version $Id$
 */
public class ToKeyTransformer
    implements Transformer
{

    public Object transform( Object input )
    {
        if ( input instanceof ArchivaProjectModel )
        {
            return Keys.toKey( (ArchivaProjectModel) input );
        }

        if ( input instanceof DependencyGraphNode )
        {
            return DependencyGraphKeys.toKey( ((DependencyGraphNode) input).getArtifact() );
        }

        if ( input instanceof DependencyGraphEdge )
        {
            DependencyGraphEdge edge = (DependencyGraphEdge) input;
            // Potentially Confusing, but this is called "To"KeyTransformer after all.
            return DependencyGraphKeys.toKey( edge.getNodeTo() );
        }

        if ( input instanceof ArtifactReference )
        {
            return DependencyGraphKeys.toKey( ((ArtifactReference) input) );
        }

        return input;
    }

}
