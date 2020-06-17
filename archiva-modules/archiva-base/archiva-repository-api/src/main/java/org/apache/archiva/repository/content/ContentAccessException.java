package org.apache.archiva.repository.content;

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

import org.apache.archiva.repository.RepositoryRuntimeException;

import java.util.List;

/**
 * This exception is thrown, during errors while accessing the repository data
 * E.g. the cause may be IO errors while accessing the filesystem, permission problems
 * on the filesystem or other backend related problems.
 */
public class ContentAccessException extends RepositoryRuntimeException
{
    private static final long serialVersionUID = 3811491193671356230L;

    List<Exception> errorList;

    public ContentAccessException( )
    {
    }

    public ContentAccessException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ContentAccessException( String message )
    {
        super( message );
    }

    public ContentAccessException( Throwable cause )
    {
        super( cause );
    }
}
