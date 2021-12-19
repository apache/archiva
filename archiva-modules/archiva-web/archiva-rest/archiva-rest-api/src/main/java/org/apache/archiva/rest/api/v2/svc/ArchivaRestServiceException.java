package org.apache.archiva.rest.api.v2.svc;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Generic REST Service Exception that contains error information.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class ArchivaRestServiceException extends Exception
{
    private int httpErrorCode = 500;

    private List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>(0);

    public ArchivaRestServiceException( String s )
    {
        super( s );
    }

    public ArchivaRestServiceException( String s, int httpErrorCode )
    {
        super( s );
        this.httpErrorCode = httpErrorCode;
    }

    public ArchivaRestServiceException( ErrorMessage errorMessage )
    {
        errorMessages.add( errorMessage );
    }

    public ArchivaRestServiceException( ErrorMessage errorMessage, int httpResponseCode )
    {
        this.httpErrorCode = httpResponseCode;
        errorMessages.add( errorMessage );
    }

    public ArchivaRestServiceException( List<ErrorMessage> errorMessage )
    {
        errorMessages.addAll( errorMessage );
    }

    public ArchivaRestServiceException( List<ErrorMessage> errorMessage, int httpResponseCode )
    {
        this.httpErrorCode = httpResponseCode;
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
