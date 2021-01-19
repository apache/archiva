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


import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * Base exception class for the admin interfaces. Exceptions should set keys that allows identifying and classifying the error.
 *
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public class RepositoryAdminException
    extends Exception
{

    private static final ResourceBundle bundle = ResourceBundle.getBundle( "org.apache.archiva.admin.model.error.AdminErrors", Locale.ROOT );

    /**
     * can return the field name of bean with issue
     * can be <code>null</code>
     * @since 1.4-M3
     */
    private String fieldName;

    /**
     * A unique identifier of this error
     * @since 3.0
     */
    private String key;
    private boolean keyExists = false;

    /**
     * Message parameters
     */
    String[] parameters = new String[0];

    protected static String getMessage( String key, String[] params )
    {
        return MessageFormat.format( bundle.getString( key ), params );
    }

    /**
     * Tries to retrieve a message from the bundle for the given key and returns the
     * exception.
     * @param key the identifier of the error
     * @param params parameters for translating the message
     * @return the exception
     */
    public static RepositoryAdminException ofKey(String key, String... params) {
        String message = getMessage( key, params );
        RepositoryAdminException ex = new RepositoryAdminException( message );
        ex.setKey( key );
        ex.setParameters( params );
        return ex;
    }

    /**
     * Tries to retrieve a message from the bundle for the given key and returns the
     * exception.
     * @param key the identifier of the error
     * @param cause the exception that caused the error
     * @param params parameters for translating the message
     * @return the exception
     */
    public static RepositoryAdminException ofKey(String key, Throwable cause, String... params) {
        String message = getMessage( key, params );
        RepositoryAdminException ex = new RepositoryAdminException( message, cause );
        ex.setKey( key );
        ex.setParameters( params );
        return ex;
    }


    /**
     * Tries to retrieve a message from the bundle for the given key and the given field and returns the
     * exception.
     * @param key the identifier of the error
     * @param fieldName the field this exception is for
     * @param params parameters for translating the message
     * @return the exception
     */
    public static RepositoryAdminException ofKeyAndField(String key, String fieldName, String... params) {
        String message = getMessage( key, params );
        RepositoryAdminException ex = new RepositoryAdminException( message, fieldName );
        ex.setKey( key );
        ex.setParameters( params );
        return ex;
    }

    /**
     * Tries to retrieve a message from the bundle for the given key and the given field and returns the
     * exception.
     * @param key the identifier of the error
     * @param fieldName the field this exception is for
     * @param cause the exception that caused this error
     * @param params parameters for translating the message
     * @return the exception
     */
    public static RepositoryAdminException ofKeyAndField(String key, Throwable cause, String fieldName, String... params) {
        String message = getMessage( key, params );
        RepositoryAdminException ex = new RepositoryAdminException( message, cause, fieldName );
        ex.setKey( key );
        ex.setFieldName( fieldName );
        ex.setParameters( params );
        return ex;
    }

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

    public String getKey( )
    {
        return key;
    }

    public void setKey( String key )
    {
        this.keyExists=!StringUtils.isEmpty( key );
        this.key = key;
    }

    public boolean keyExists() {
        return this.keyExists;
    }

    public String[] getParameters( )
    {
        return parameters;
    }

    public void setParameters( String[] parameters )
    {
        if (parameters==null) {
            this.parameters = new String[0];
        }
        this.parameters = parameters;
    }
}
