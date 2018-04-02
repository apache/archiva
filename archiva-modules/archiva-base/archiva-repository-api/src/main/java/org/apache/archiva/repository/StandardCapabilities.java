package org.apache.archiva.repository;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Capability implementation.
 */
public class StandardCapabilities implements RepositoryCapabilities
{
    private final Set<ReleaseScheme> supportedReleaseSchemes;
    private final Set<ReleaseScheme> uSupportedReleaseSchemes;
    private final Set<String> supportedLayouts;
    private final Set<String> uSupportedLayouts;
    private final Set<String> customCapabilities;
    private final Set<String> uCustomCapabilities;
    private final Set<String> supportedFeatures;
    private final Set<String> uSupportedFeatures;
    private final boolean indexable;
    private final boolean fileBased;
    private final boolean canBlockRedeployments;
    private final boolean scannable;
    private final boolean allowsFailover;


    public StandardCapabilities( ReleaseScheme[] supportedReleaseSchemes, String[] supportedLayouts,
                                 String[] customCapabilities, String[] supportedFeatures,
                                 boolean indexable, boolean fileBased,
                                 boolean canBlockRedeployments, boolean scannable, boolean allowsFailover )
    {
        this.supportedReleaseSchemes = new HashSet<>();
        for (ReleaseScheme scheme : supportedReleaseSchemes) {
            this.supportedReleaseSchemes.add(scheme);
        }
        this.uSupportedReleaseSchemes = Collections.unmodifiableSet( this.supportedReleaseSchemes);
        this.supportedLayouts = new HashSet<>(  );
        for (String layout : supportedLayouts) {
            this.supportedLayouts.add(layout);
        }
        this.uSupportedLayouts = Collections.unmodifiableSet( this.supportedLayouts );
        this.customCapabilities = new HashSet<>(  );
        for (String cap : customCapabilities) {
            this.customCapabilities.add(cap);
        }
        this.uCustomCapabilities = Collections.unmodifiableSet( this.customCapabilities );
        this.supportedFeatures = new HashSet<>(  );
        for (String feature : supportedFeatures) {
            this.supportedFeatures.add(feature);
        }
        this.uSupportedFeatures = Collections.unmodifiableSet( this.supportedFeatures );
        this.indexable = indexable;
        this.fileBased = fileBased;
        this.canBlockRedeployments = canBlockRedeployments;
        this.scannable = scannable;
        this.allowsFailover = allowsFailover;
    }

    @Override
    public Set<ReleaseScheme> supportedReleaseSchemes( )
    {
        return uSupportedReleaseSchemes;
    }

    @Override
    public Set<String> supportedLayouts( )
    {
        return uSupportedLayouts;
    }

    @Override
    public Set<String> customCapabilities( )
    {
        return uCustomCapabilities;
    }

    @Override
    public Set<String> supportedFeatures( )
    {
        return uSupportedFeatures;
    }

    @Override
    public boolean isIndexable( )
    {
        return indexable;
    }

    @Override
    public boolean isFileBased( )
    {
        return fileBased;
    }

    @Override
    public boolean canBlockRedeployments( )
    {
        return canBlockRedeployments;
    }

    @Override
    public boolean isScannable( )
    {
        return scannable;
    }

    @Override
    public boolean allowsFailover( )
    {
        return allowsFailover;
    }
}
