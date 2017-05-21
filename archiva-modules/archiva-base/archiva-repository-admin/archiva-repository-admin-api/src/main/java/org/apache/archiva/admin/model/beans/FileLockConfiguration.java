package org.apache.archiva.admin.model.beans;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * File Locking configuration.
 *
 * @since 2.0.0
 */
@XmlRootElement( name = "fileLockConfiguration" )
public class FileLockConfiguration
    implements Serializable
{

    /**
     * skipping the locking mechanism.
     */
    private boolean skipLocking = true;

    /**
     * maximum time to wait to get the file lock (0 infinite).
     */
    private int lockingTimeout = 0;


    /**
     * Get maximum time to wait to get the file lock (0 infinite).
     *
     * @return int
     */
    public int getLockingTimeout()
    {
        return this.lockingTimeout;
    }

    /**
     * Get skipping the locking mechanism.
     *
     * @return boolean
     */
    public boolean isSkipLocking()
    {
        return this.skipLocking;
    }

    /**
     * Set maximum time to wait to get the file lock (0 infinite).
     *
     * @param lockingTimeout
     */
    public void setLockingTimeout( int lockingTimeout )
    {
        this.lockingTimeout = lockingTimeout;
    }

    /**
     * Set skipping the locking mechanism.
     *
     * @param skipLocking
     */
    public void setSkipLocking( boolean skipLocking )
    {
        this.skipLocking = skipLocking;
    }

}
