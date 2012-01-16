package org.apache.archiva.admin.model;
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
public class RepositoryAdminException
    extends Exception
{

    /**
     * can return the field name of bean with issue
     * can be <code>null</code>
     * @since 1.4-M3
     */
    private String fieldName;

    public RepositoryAdminException( String s )
    {
        super( s );
    }

    public RepositoryAdminException( String s, String fieldName )
    {
        this( s );
        this.fieldName = fieldName;
    }

    public RepositoryAdminException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public RepositoryAdminException( String message, Throwable cause,  String fieldName )
    {
        super( message, cause );
        this.fieldName = fieldName;
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
