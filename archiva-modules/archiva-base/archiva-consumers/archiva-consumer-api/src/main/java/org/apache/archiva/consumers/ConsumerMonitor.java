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
 * ConsumerMonitor - a monitor for consumers. 
 *
 * @version $Id$
 */
public interface ConsumerMonitor
{
    /**
     * A consumer error event.
     * 
     * @param consumer the consumer that caused the error.
     * @param type the type of error.
     * @param message the message about the error.
     */
    public void consumerError( Consumer consumer, String type, String message );
    
    /**
     * A consumer warning event.
     * 
     * @param consumer the consumer that caused the warning.
     * @param type the type of warning.
     * @param message the message about the warning.
     */
    public void consumerWarning( Consumer consumer, String type, String message );

    /**
     * A consumer informational event.
     * 
     * @param consumer the consumer that caused the informational message.
     * @param message the message.
     */
    public void consumerInfo( Consumer consumer, String message );
}
