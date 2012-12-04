package org.apache.archiva.redback.rest.api.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "errorMessage" )
public class ErrorMessage
    implements Serializable
{
    private String errorKey;

    private String[] args;

    /**
     * @since 2.1 for message without any key
     */
    private String message;

    private static final String[] EMPTY = new String[0];

    public ErrorMessage()
    {
        // no op
    }

    public ErrorMessage( String errorKey )
    {
        this.errorKey = errorKey;
        this.args = EMPTY;
    }

    public ErrorMessage( String errorKey, String[] args )
    {
        this.errorKey = errorKey;
        this.args = args;
    }

    public String getErrorKey()
    {
        return errorKey;
    }

    public void setErrorKey( String errorKey )
    {
        this.errorKey = errorKey;
    }

    public String[] getArgs()
    {
        return args;
    }

    public void setArgs( String[] args )
    {
        this.args = args;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    public ErrorMessage message( String message )
    {
        this.message = message;
        return this;
    }
}
