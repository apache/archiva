package org.apache.maven.archiva.dependency.graph;

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

/**
 * DependencyResolutionEvent 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class DependencyResolutionEvent
{
    public static final int ADDING_MODEL = 1;

    public static final int DEP_CONFLICT_OMIT_FOR_NEARER = 2;

    public static final int CYCLE_BROKEN = 3;

    public static final int APPLYING_DEPENDENCY_MANAGEMENT = 4;

    private int type;

    private DependencyGraph graph;

    public DependencyResolutionEvent( int type, DependencyGraph graph )
    {
        this.type = type;
        this.graph = graph;
    }

    public DependencyGraph getGraph()
    {
        return graph;
    }

    public int getType()
    {
        return type;
    }
}
