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

import org.apache.commons.collections.Predicate;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.StringUtils;

/**
 * MatchingDependencyPredicate 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class MatchingDependencyPredicate
    implements Predicate
{
    private Dependency selectedDep;
    
    public MatchingDependencyPredicate( Dependency selectedDep )
    {
        this.selectedDep = selectedDep;
    }

    public boolean evaluate( Object input )
    {
        boolean satisfies = false;

        if ( input instanceof Dependency )
        {
            Dependency dep = (Dependency) input;
            if ( StringUtils.equals( dep.getArtifactId(), selectedDep.getArtifactId() )
                 && StringUtils.equals( dep.getGroupId(), selectedDep.getGroupId() )
                 && StringUtils.equals( dep.getType(), selectedDep.getType() ))
            {
                // So far, so good. groupId/artifactId/type match.
                satisfies = true;
                
                // Test classifier (if defined)
                if( StringUtils.isNotEmpty( selectedDep.getClassifier() ) )
                {
                    satisfies = StringUtils.equals( dep.getClassifier(), selectedDep.getClassifier() );
                }
            }
        }

        return satisfies;
    }
}
