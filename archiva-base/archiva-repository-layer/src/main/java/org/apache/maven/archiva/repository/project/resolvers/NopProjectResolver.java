package org.apache.maven.archiva.repository.project.resolvers;

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
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.repository.project.ProjectModelException;
import org.apache.maven.archiva.repository.project.ProjectModelResolver;

/**
 * A No-op Project Resolver, perform no lookup, just returns the requested
 * information in the form of a simple ArchviaProjectModel.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class NopProjectResolver
    implements ProjectModelResolver
{
    private static NopProjectResolver INSTANCE = new NopProjectResolver();

    public static NopProjectResolver getInstance()
    {
        return INSTANCE;
    }

    public ArchivaProjectModel resolveProjectModel( VersionedReference reference )
        throws ProjectModelException
    {
        ArchivaProjectModel model = new ArchivaProjectModel();

        model.setGroupId( reference.getGroupId() );
        model.setArtifactId( reference.getArtifactId() );
        model.setVersion( reference.getVersion() );
        model.setPackaging( "pom" );

        model.setOrigin( "nop" );

        return model;
    }

}
