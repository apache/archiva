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

import org.apache.archiva.repository.ManagedRepository;
import org.apache.archiva.repository.ManagedRepositoryContent;
import org.apache.archiva.repository.RemoteRepository;
import org.apache.archiva.repository.RemoteRepositoryContent;
import org.apache.archiva.repository.Repository;
import org.apache.archiva.repository.RepositoryContent;
import org.apache.archiva.repository.RepositoryContentProvider;
import org.apache.archiva.repository.RepositoryException;
import org.apache.archiva.repository.RepositoryType;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service("repositoryContentProvider#mock")
public class RepositoryContentProviderMock implements RepositoryContentProvider {

    private static final Set<RepositoryType> REPOSITORY_TYPES = new HashSet<>();
    static {
        REPOSITORY_TYPES.add(RepositoryType.MAVEN);
        REPOSITORY_TYPES.add(RepositoryType.NPM);
    }

    @Override
    public boolean supportsLayout(String layout) {
        return true;
    }

    @Override
    public Set<RepositoryType> getSupportedRepositoryTypes() {
        return REPOSITORY_TYPES;
    }

    @Override
    public boolean supports(RepositoryType type) {
        return true;
    }

    @Override
    public RemoteRepositoryContent createRemoteContent(RemoteRepository repository) throws RepositoryException {
        return new RemoteRepositoryContentMock();
    }

    @Override
    public ManagedRepositoryContent createManagedContent(ManagedRepository repository) throws RepositoryException {
        return new ManagedRepositoryContentMock();
    }

    @Override
    public <T extends RepositoryContent, V extends Repository> T createContent(Class<T> clazz, V repository) throws RepositoryException {
        return null;
    }
}
