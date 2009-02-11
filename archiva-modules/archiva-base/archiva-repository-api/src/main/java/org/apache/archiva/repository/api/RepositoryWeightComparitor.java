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

import org.apache.archiva.repository.api.RepositoryManager;
import org.apache.archiva.repository.api.RepositoryManagerWeight;
import java.util.Comparator;

public class RepositoryWeightComparitor implements Comparator<RepositoryManager>
{
    public int compare(RepositoryManager repo1, RepositoryManager repo2)
    {
        RepositoryManagerWeight repo1Weight = getRepositoryManagerWeight(repo1);
        RepositoryManagerWeight repo2Weight = getRepositoryManagerWeight(repo2);
        if (repo1Weight == null)
        {
            return -1;
        }
        if (repo2Weight == null)
        {
            return 1;
        }
        if (repo1Weight.value() > repo2Weight.value())
        {
            return 1;
        }
        if (repo1Weight.value() < repo2Weight.value())
        {
            return -1;
        }
        return 0;
    }

    private RepositoryManagerWeight getRepositoryManagerWeight(RepositoryManager manager)
    {
        return manager.getClass().getAnnotation(RepositoryManagerWeight.class);
    }
}
