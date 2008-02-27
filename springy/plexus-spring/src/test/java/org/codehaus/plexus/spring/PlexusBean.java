package org.codehaus.plexus.spring;

import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.logging.Logger;

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
 * A plexus component interface
 *
 * @author <a href="mailto:nicolas@apache.org">Nicolas De Loof</a>
 */
public interface PlexusBean
{
    public static final String DISPOSED = "disposed";

    public static final String INITIALIZED = "initialized";

    String ROLE = PlexusBean.class.getName();

    String describe();

    String getState();

    Context getContext();

    Logger getLogger();
}