package org.codehaus.redback.rest.api.services;
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

import org.codehaus.redback.rest.api.model.ErrorMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
public class RedbackServiceException
    extends Exception
{
    private int httpErrorCode = 500;

    private List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>(0);

    public RedbackServiceException( String s )
    {
        super( s );
    }

    public RedbackServiceException( String s, int httpErrorCode )
    {
        super( s );
        this.httpErrorCode = httpErrorCode;
    }

    public RedbackServiceException( ErrorMessage errorMessage )
    {
        errorMessages.add( errorMessage );
    }

    public RedbackServiceException( ErrorMessage errorMessage, int httpErrorCode )
    {
        this.httpErrorCode = httpErrorCode;
        errorMessages.add( errorMessage );
    }

    public RedbackServiceException( List<ErrorMessage> errorMessage )
    {
        errorMessages.addAll( errorMessage );
    }

    public int getHttpErrorCode()
    {
        return httpErrorCode;
    }

    public void setHttpErrorCode( int httpErrorCode )
    {
        this.httpErrorCode = httpErrorCode;
    }

    public List<ErrorMessage> getErrorMessages()
    {
        if ( errorMessages == null )
        {
            this.errorMessages = new ArrayList<ErrorMessage>();
        }
        return errorMessages;
    }

    public void setErrorMessages( List<ErrorMessage> errorMessages )
    {
        this.errorMessages = errorMessages;
    }

    public void addErrorMessage( ErrorMessage errorMessage )
    {
        this.errorMessages.add( errorMessage );
    }
}
