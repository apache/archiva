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

/**
 * This module provides an event mechanism for all archiva subsystems.
 *
 * The events are hierarchical organized. That means each subsystem has its own event manager that collects events
 * and processes and forwards them to the parent event manager (normally the central event manager).
 * Each event manager clones the event and stores the origin event in the chain before forwarding them to the parent manager.
 *
 * Event Types are also hierarchical. There is one special type {@link org.apache.archiva.event.EventType#ROOT} that is the
 * root type and has no parent type. All other types must be descendants of the ROOT type.
 *
 * Event types may have certain methods to access context information. But context information can also be accessed in a
 * subsystem independent way using the event context data. Event contexts provide access to data without using the
 * subsystem API and classes.
 * Event types may be used for filtering events.
 *
 * @since 3.0
 * @author Martin Schreier <martin_s@apache.org>
 */
package org.apache.archiva.event;