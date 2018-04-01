package org.apache.archiva.checksum;

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
 * Exception thrown by the ChecksumValidator
 *
 * Has an error type for different validation errors.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ChecksumValidationException extends RuntimeException
{

    public enum ValidationError {
        INVALID_FORMAT, DIGEST_ERROR, READ_ERROR, FILE_NOT_FOUND, BAD_CHECKSUM_FILE_REF, BAD_CHECKSUM_FILE
    };

    final private ValidationError errorType;

    public ChecksumValidationException( ValidationError errorType )
    {
        super( );
        this.errorType = errorType;
    }

    public ChecksumValidationException( ValidationError errorType,  String message )
    {
        super( message );
        this.errorType = errorType;
    }

    public ChecksumValidationException( ValidationError errorType, String message, Throwable cause )
    {
        super( message, cause );
        this.errorType = errorType;
    }

    public ChecksumValidationException( ValidationError errorType, Throwable cause )
    {
        super( cause );
        this.errorType = errorType;
    }

    public ValidationError getErrorType() {
        return errorType;
    }
}
