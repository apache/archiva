package org.apache.archiva.rest.services;
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

import org.apache.archiva.rest.api.services.ArchivaRestServiceException;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Olivier Lamy
 * @since 1.4-M2
 */
@XmlRootElement( name = "archivaRestError" )
public class ArchivaRestError
{
    
    private int httpErrorCode;
    
    private String errorKey;
    
    private String errorMessage;
    
    public ArchivaRestError()
    {
        // no op
    }
    
    public ArchivaRestError( ArchivaRestServiceException e )
    {
        httpErrorCode = e.getHttpErrorCode();
        errorKey = e.getErrorKey();
        errorMessage = e.getMessage();
    }

    public int getHttpErrorCode()
    {
        return httpErrorCode;
    }

    public void setHttpErrorCode( int httpErrorCode )
    {
        this.httpErrorCode = httpErrorCode;
    }

    public String getErrorKey()
    {
        return errorKey;
    }

    public void setErrorKey( String errorKey )
    {
        this.errorKey = errorKey;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }
}
