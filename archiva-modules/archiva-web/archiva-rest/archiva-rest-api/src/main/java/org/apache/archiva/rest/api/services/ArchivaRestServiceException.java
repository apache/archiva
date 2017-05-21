package org.apache.archiva.rest.api.services;
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

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class ArchivaRestServiceException
    extends Exception
{

    private int httpErrorCode = 500;

    private String errorKey;

    /**
     * can return the field name of bean with issue
     * can be <code>null</code>
     *
     * @since 1.4-M3
     */
    private String fieldName;


    public ArchivaRestServiceException( String message, Throwable t )
    {
        super( message, t );
    }

    public ArchivaRestServiceException( String message, String fieldName, Throwable t )
    {
        this( message, t );
        this.fieldName = fieldName;
    }

    public ArchivaRestServiceException( String s, int httpErrorCode, Throwable t )
    {
        super( s, t );
        this.httpErrorCode = httpErrorCode;
    }

    public ArchivaRestServiceException( String s, int httpErrorCode, String fieldName, Throwable t )
    {
        this( s, httpErrorCode, t );
        this.fieldName = fieldName;
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

    public String getFieldName()
    {
        return fieldName;
    }

    public void setFieldName( String fieldName )
    {
        this.fieldName = fieldName;
    }

}
