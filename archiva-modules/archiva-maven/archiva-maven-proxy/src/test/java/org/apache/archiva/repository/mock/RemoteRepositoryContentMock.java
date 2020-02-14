package org.apache.archiva.repository.mock;

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

import org.apache.archiva.common.utils.VersionUtil;
import org.apache.archiva.model.ArtifactReference;
import org.apache.archiva.model.RepositoryURL;
import org.apache.archiva.repository.LayoutException;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.content.ItemSelector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service("remoteRepositoryContent#mock")
public class RemoteRepositoryContentMock implements RemoteRepositoryContent
{
    RemoteRepository repository;

    RemoteRepositoryContentMock(RemoteRepository repo) {
        this.repository = repo;
    }

    @Override
    public String getId( )
    {
        return repository.getId();
    }

    @Override
    public RemoteRepository getRepository( )
    {
        return repository;
    }

    @Override
    public RepositoryURL getURL( )
    {
        return new RepositoryURL(repository.getLocation().toString());
    }

    @Override
    public void setRepository( RemoteRepository repo )
    {
        this.repository = repo;
    }

    @Override
    public ArtifactReference toArtifactReference( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public String toPath( ArtifactReference reference )
    {
        String baseVersion;
        if (VersionUtil.isSnapshot(reference.getVersion())) {
            baseVersion=VersionUtil.getBaseVersion(reference.getVersion());
        } else {
            baseVersion=reference.getVersion();
        }
        return reference.getGroupId().replaceAll("\\.", "/")+"/"+reference.getArtifactId()+"/"+baseVersion+"/"
                +reference.getArtifactId()+"-"+reference.getVersion()+(
                StringUtils.isNotEmpty(reference.getClassifier()) ? "-"+reference.getClassifier() : "")+"."+reference.getType();
    }

    @Override
    public String toPath( ItemSelector selector )
    {
        String baseVersion;
        if (!selector.hasVersion() && VersionUtil.isSnapshot(selector.getArtifactVersion())) {
            baseVersion=VersionUtil.getBaseVersion(selector.getArtifactVersion());
        } else {
            baseVersion=selector.getVersion();
        }
        return selector.getNamespace().replaceAll("\\.", "/")+"/"+selector.getArtifactId()+"/"+baseVersion+"/"
            +selector.getArtifactId()+"-"+selector.getVersion()+(
            StringUtils.isNotEmpty(selector.getClassifier()) ? "-"+selector.getClassifier() : "")+"."+selector.getType();
    }

    @Override
    public ItemSelector toItemSelector( String path ) throws LayoutException
    {
        return null;
    }

    @Override
    public RepositoryURL toURL( ArtifactReference reference )
    {
        return null;
    }
}
