package org.codehaus.plexus.redback.authorization;

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

/**
 * The exception thrown if an entity is unauthorized to access a resource.
 *
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Mar 1, 2003
 */
public class UnauthorizedException
    extends Exception
{
    /**
     * @param message
     */
    public UnauthorizedException( String message )
    {
        super( message );
    }
}
