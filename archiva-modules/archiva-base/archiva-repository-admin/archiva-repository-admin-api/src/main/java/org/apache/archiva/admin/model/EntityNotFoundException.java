package org.apache.archiva.admin.model;/*
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
 * This exception is thrown, if a requested entity does not exist.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class EntityNotFoundException extends RepositoryAdminException
{
    public static final String KEY = "entity.not_found";

    public static EntityNotFoundException of(String... parameters) {
        String message = getMessage( KEY, parameters );
        return new EntityNotFoundException( message, parameters );
    }

    public static EntityNotFoundException ofMessage(String message, String... parameters) {
        return new EntityNotFoundException( message, parameters );
    }

    public EntityNotFoundException( String s, String... parameters )
    {
        super( s );
        setKey( KEY );
        setParameters( parameters );
    }

    public EntityNotFoundException( String s, String fieldName, String... parameters )
    {
        super( s, fieldName );
        setKey( KEY );
        setParameters( parameters );
    }

    public EntityNotFoundException( String message, Throwable cause, String... parameters )
    {
        super( message, cause );
        setKey( KEY );
        setParameters( parameters );
    }

    public EntityNotFoundException( String message, Throwable cause, String fieldName, String... parameters )
    {
        super( message, cause, fieldName );
        setKey( KEY );
        setParameters( parameters );
    }
}
