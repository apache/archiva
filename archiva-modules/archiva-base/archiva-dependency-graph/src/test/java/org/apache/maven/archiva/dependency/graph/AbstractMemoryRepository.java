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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.model.ArchivaProjectModel;
import org.apache.maven.archiva.model.Dependency;
import org.apache.maven.archiva.model.Exclusion;
import org.apache.maven.archiva.model.Keys;
import org.apache.maven.archiva.model.VersionedReference;

/**
 * AbstractMemoryRepository 
 *
 * @version $Id$
 */
public abstract class AbstractMemoryRepository
    implements MemoryRepository
{
    private Map<String,ArchivaProjectModel> modelMap = new HashMap<String, ArchivaProjectModel>();

    public AbstractMemoryRepository()
    {
        initialize();
    }

    public void addModel( ArchivaProjectModel model )
    {
        String key = Keys.toKey( model );
        modelMap.put( key, model );
    }

    public ArchivaProjectModel getProjectModel( String groupId, String artifactId, String version )
    {
        String key = Keys.toKey( groupId, artifactId, version );

        return (ArchivaProjectModel) modelMap.get( key );
    }

    public abstract void initialize();

    protected void addExclusion( Dependency dependency, String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );

        if ( parts.length != 2 )
        {
            throw new IllegalArgumentException( "Exclusion key [" + key + "] should be 2 parts. (detected "
                + parts.length + " instead)" );
        }

        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId( parts[0] );
        exclusion.setArtifactId( parts[1] );

        dependency.addExclusion( exclusion );
    }

    protected Dependency toDependency( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );

        if ( parts.length != 5 )
        {
            throw new IllegalArgumentException( "Dependency key [" + key + "] should be 5 parts. (detected "
                + parts.length + " instead)" );
        }

        Dependency dep = new Dependency();

        dep.setGroupId( parts[0] );
        dep.setArtifactId( parts[1] );
        dep.setVersion( parts[2] );
        dep.setClassifier( parts[3] );
        dep.setType( parts[4] );

        return dep;
    }

    protected Dependency toDependency( String key, String scope )
    {
        Dependency dependency = toDependency( key );
        dependency.setScope( scope );

        return dependency;
    }

    protected ArchivaProjectModel toModel( String key )
    {
        return toModel( key, Collections.<Dependency>emptyList() );
    }

    protected ArchivaProjectModel toModel( String key, Dependency[] deps )
    {
        List<Dependency> depList = new ArrayList<Dependency>();

        if ( deps != null )
        {
            depList.addAll( Arrays.asList( deps ) );
        }

        return toModel( key, depList );
    }

    protected ArchivaProjectModel toModel( String key, List<Dependency> deps )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );

        if ( parts.length != 3 )
        {
            throw new IllegalArgumentException( "Project/Model key [" + key + "] should be 3 parts. (detected "
                + parts.length + " instead)" );
        }

        ArchivaProjectModel model = new ArchivaProjectModel();
        model.setGroupId( parts[0] );
        model.setArtifactId( parts[1] );
        model.setVersion( parts[2] );
        model.setOrigin( "testcase" );
        model.setPackaging( "jar" );

        for ( Dependency dep : deps )
        {
            model.addDependency( dep );
        }

        return model;
    }

    protected VersionedReference toParent( String key )
    {
        String parts[] = StringUtils.splitPreserveAllTokens( key, ':' );

        if ( parts.length != 3 )
        {
            throw new IllegalArgumentException( "Parent key [" + key + "] should be 3 parts. (detected " + parts.length
                + " instead)" );
        }

        VersionedReference ref = new VersionedReference();
        ref.setGroupId( parts[0] );
        ref.setArtifactId( parts[1] );
        ref.setVersion( parts[2] );

        return ref;
    }

}
