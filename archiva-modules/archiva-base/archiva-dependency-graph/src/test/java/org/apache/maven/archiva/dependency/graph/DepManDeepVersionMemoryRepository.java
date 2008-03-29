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

import org.apache.maven.archiva.model.ArchivaProjectModel;

/**
 * DepManDeepVersionMemoryRepository
 * 
 * MemoryRepository for testing <code>net.example.depman.deepversion:A:1.0</code>
 *
 * @version $Id$
 */
public class DepManDeepVersionMemoryRepository
    extends AbstractMemoryRepository
{
    public void initialize()
    {
        ArchivaProjectModel model;

        model = toModel( "net.example.depman.deepversion:A:1.0" );
        model.addDependency( toDependency( "net.example.depman.deepversion:B:1.0::jar" ) );
        model.addDependency( toDependency( "net.example.depman.deepversion:C:1.0::jar" ) );
        model.addDependencyManagement( toDependency( "net.example.depman.deepversion:D:2.0::jar" ) );
        addModel( model );
        
        /* Having a depman in A for D:2.0 will cause an orphaned E:2.0 during the depman
         * application phase.
         * 
         * This is intentional, to test out the depman application and recovery.
         */

        model = toModel( "net.example.depman.deepversion:B:1.0" );
        model.addDependency( toDependency( "net.example.depman.deepversion:D:1.0::jar" ) );
        addModel( model );

        model = toModel( "net.example.depman.deepversion:E:2.0" );
        addModel( model );
        
        model = toModel( "net.example.depman.deepversion:E:3.0" );
        model.addDependency( toDependency( "net.example.depman.deepversion:F:1.0::jar" ) );
        addModel( model );
        
        model = toModel( "net.example.depman.deepversion:F:1.0" );
        addModel( model );

        model = toModel( "net.example.depman.deepversion:C:1.0" );
        model.addDependency( toDependency( "net.example.depman.deepversion:D:1.0::jar" ) );
        addModel( model );

        model = toModel( "net.example.depman.deepversion:D:1.0" );
        model.addDependency( toDependency( "net.example.depman.deepversion:E:2.0::jar" ) );
        addModel( model );

        model = toModel( "net.example.depman.deepversion:D:2.0" );
        model.addDependency( toDependency( "net.example.depman.deepversion:E:3.0::jar" ) );
        addModel( model );

    }
}
