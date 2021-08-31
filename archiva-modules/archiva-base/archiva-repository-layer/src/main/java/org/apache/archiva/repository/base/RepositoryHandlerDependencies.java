package org.apache.archiva.repository.base;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.repository.base.group.RepositoryGroupHandler;
import org.apache.archiva.repository.base.managed.ManagedRepositoryHandler;
import org.apache.archiva.repository.base.remote.RemoteRepositoryHandler;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * This is just a class that pulls the handler dependencies. It is used by test classes.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service()
public class RepositoryHandlerDependencies
{
    @Inject
    ManagedRepositoryHandler managedRepositoryHandler;

    @Inject
    RemoteRepositoryHandler remoteRepositoryHandler;

    @Inject
    RepositoryGroupHandler repositoryGroupHandler;
}
