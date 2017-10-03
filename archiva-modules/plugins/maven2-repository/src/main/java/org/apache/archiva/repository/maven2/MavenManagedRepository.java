package org.apache.archiva.repository.maven2;

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

import org.apache.archiva.repository.AbstractManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.ReleaseScheme;
import org.apache.archiva.repository.RepositoryCapabilities;
import org.apache.archiva.repository.RepositoryType;
import org.apache.archiva.repository.StandardCapabilities;
import org.apache.archiva.repository.UnsupportedFeatureException;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.repository.features.IndexCreationFeature;
import org.apache.archiva.repository.features.RepositoryFeature;
import org.apache.archiva.repository.features.StagingRepositoryFeature;

import java.util.Locale;

/**
 * Maven2 managed repository implementation.
 */
public class MavenManagedRepository extends AbstractManagedRepository
{
    public static final String DEFAULT_LAYOUT = "default";
    public static final String LEGACY_LAYOUT = "legacy";
    private ManagedRepositoryContent content;
    private ArtifactCleanupFeature artifactCleanupFeature = new ArtifactCleanupFeature( );
    private IndexCreationFeature indexCreationFeature = new IndexCreationFeature(  );
    private StagingRepositoryFeature stagingRepositoryFeature = new StagingRepositoryFeature(  );

    private static final RepositoryCapabilities CAPABILITIES = new StandardCapabilities(
        new ReleaseScheme[] { ReleaseScheme.RELEASE, ReleaseScheme.SNAPSHOT },
        new String[] { DEFAULT_LAYOUT, LEGACY_LAYOUT},
        new String[] {},
        new String[] {ArtifactCleanupFeature.class.getName(), IndexCreationFeature.class.getName(),
            StagingRepositoryFeature.class.getName()},
        true,
        true,
        true,
        true,
        false
    );

    public MavenManagedRepository( String id, String name )
    {
        super( RepositoryType.MAVEN, id, name );
    }

    public MavenManagedRepository( Locale primaryLocale, String id, String name )
    {
        super( primaryLocale, RepositoryType.MAVEN, id, name );
    }

    protected void setContent(ManagedRepositoryContent content) {
        this.content = content;
    }

    @Override
    public ManagedRepositoryContent getContent( )
    {
        return content;
    }

    @Override
    public RepositoryCapabilities getCapabilities( )
    {
        return CAPABILITIES;
    }

    @Override
    public <T extends RepositoryFeature<T>> RepositoryFeature<T> getFeature( Class<T> clazz ) throws UnsupportedFeatureException
    {
        if (ArtifactCleanupFeature.class.equals( clazz ))
        {
            return (RepositoryFeature<T>) artifactCleanupFeature;
        } else if (IndexCreationFeature.class.equals(clazz)) {
            return (RepositoryFeature<T>) indexCreationFeature;
        } else if (StagingRepositoryFeature.class.equals(clazz)) {
            return (RepositoryFeature<T>) stagingRepositoryFeature;
        } else {
            throw new UnsupportedFeatureException(  );
        }
    }

    @Override
    public <T extends RepositoryFeature<T>> boolean supportsFeature( Class<T> clazz )
    {
        if (ArtifactCleanupFeature.class.equals(clazz) ||
            IndexCreationFeature.class.equals(clazz) ||
            StagingRepositoryFeature.class.equals(clazz)) {
            return true;
        }
        return false;
    }
}
