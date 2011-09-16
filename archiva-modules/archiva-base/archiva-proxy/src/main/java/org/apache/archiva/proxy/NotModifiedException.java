package org.apache.archiva.proxy;

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
 * <p>
 * NotModifiedException - thrown when the resource requested was found on the remote repository, but
 * the remote repository reported that the copy we have in our managed repository is newer than
 * the one present on the remote repository.
 * </p>
 * <p>
 * Similar in scope to the <code>HTTP 304 Not Modified</code> response code.
 * </p> 
 *
 * @version $Id$
 */
public class NotModifiedException
    extends ProxyException
{

    public NotModifiedException( String message )
    {
        super( message );
    }

    public NotModifiedException( String message, Throwable t )
    {
        super( message, t );
    }
}
