package org.apache.archiva.repository.api;

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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junit.framework.TestCase;

public class RepositoryWeightComparitorTest extends TestCase
{
    public void testRepositoryManagerWeights() throws Exception
    {
        RepoManager2 manager2 = new RepoManager2();
        RepoManager1 manager1 = new RepoManager1();

        List<RepositoryManager> managers = new ArrayList<RepositoryManager>();
        managers.add(manager2);
        managers.add(manager1);

        Collections.sort(managers, new RepositoryWeightComparitor());

        assertEquals(manager1, managers.get(0));
        assertEquals(manager2, managers.get(1));
    }

    @RepositoryManagerWeight(2)
    class RepoManager2 implements RepositoryManager
    {
        public ResourceContext handles(ResourceContext context)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        public boolean exists(String repositoryId)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean read(ResourceContext context, OutputStream os)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public List<Status> stat(ResourceContext context)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean write(ResourceContext context, InputStream is)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    @RepositoryManagerWeight(1)
    class RepoManager1 implements RepositoryManager
    {
        public ResourceContext handles(ResourceContext context)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean exists(String repositoryId)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean read(ResourceContext context, OutputStream os)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public List<Status> stat(ResourceContext context)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean write(ResourceContext context, InputStream is)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
       
    }
}
