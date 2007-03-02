package org.apache.maven.archiva.model.health;

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

import org.apache.maven.archiva.model.ArchivaArtifact;

import java.util.ArrayList;
import java.util.List;

/**
 * ArtifactHealth 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class ArtifactHealth
{
    private ArchivaArtifact artifact;
    
    private List problems = new ArrayList();

    public void addProblem( HealthProblem problem )
    {
        this.problems.add( problem );
    }

    public ArchivaArtifact getArtifact()
    {
        return artifact;
    }

    public List getProblems()
    {
        return problems;
    }

    public void setArtifact( ArchivaArtifact artifact )
    {
        this.artifact = artifact;
    }

    public void setProblems( List problems )
    {
        this.problems = problems;
    }
}
