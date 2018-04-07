package org.apache.archiva.rest.api.model;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */




import org.apache.archiva.admin.model.beans.ManagedRepository;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * @author Olivier Lamy
 */
@XmlRootElement( name = "repositoryScannerStatistics" )
public class RepositoryScannerStatistics
    implements Serializable
{
    private ManagedRepository managedRepository;

    private List<ConsumerScanningStatistics> consumerScanningStatistics;

    private long totalFileCount = 0;

    private long newFileCount = 0;

    public RepositoryScannerStatistics()
    {
        // no op
    }

    public ManagedRepository getManagedRepository()
    {
        return managedRepository;
    }

    public void setManagedRepository( ManagedRepository managedRepository )
    {
        this.managedRepository = managedRepository;
    }

    public List<ConsumerScanningStatistics> getConsumerScanningStatistics()
    {
        return consumerScanningStatistics;
    }

    public void setConsumerScanningStatistics( List<ConsumerScanningStatistics> consumerScanningStatistics )
    {
        this.consumerScanningStatistics = consumerScanningStatistics;
    }

    public long getTotalFileCount()
    {
        return totalFileCount;
    }

    public void setTotalFileCount( long totalFileCount )
    {
        this.totalFileCount = totalFileCount;
    }

    public long getNewFileCount()
    {
        return newFileCount;
    }

    public void setNewFileCount( long newFileCount )
    {
        this.newFileCount = newFileCount;
    }
}
