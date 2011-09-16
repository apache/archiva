package org.apache.archiva.consumers;

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

/**
 * Consumer - the base set of methods for a consumer.
 *
 * @version $Id$
 */
public abstract interface Consumer
{
    /**
     * This is the id for the consumer.
     * 
     * @return the consumer id.
     */
    public String getId();
    
    /**
     * The human readable description for this consumer.
     * 
     * @return the human readable description for this consumer.
     */
    public String getDescription();
    
    /**
     * Flag indicating permanance of consumer. (if it can be disabled or not)
     * 
     * @return true indicating that consumer is permanent and cannot be disabled. 
     */
    public boolean isPermanent();

    /**
     * Add a consumer monitor to the consumer.
     * 
     * @param monitor the monitor to add.
     */
    public void addConsumerMonitor( ConsumerMonitor monitor );
    
    /**
     * Remove a consumer monitor.
     * 
     * @param monitor the monitor to remove.
     */
    public void removeConsumerMonitor( ConsumerMonitor monitor );
}
