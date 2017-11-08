package org.apache.archiva.repository;

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
 * This is a fatal exception and should not happen. It is thrown if the implementation
 * does not have certain classes to support this repository type.
 */
public class UnsupportedRepositoryTypeException extends RuntimeException {

    private static final String MESSAGE = "The repository type is not supported: ";

    public UnsupportedRepositoryTypeException(RepositoryType type) {
        super(MESSAGE+type.name());
    }

    public UnsupportedRepositoryTypeException(RepositoryType type, Throwable cause) {
        super(MESSAGE+type.name(), cause);
    }

    public UnsupportedRepositoryTypeException(Throwable cause) {
        super(cause);
    }

    protected UnsupportedRepositoryTypeException(RepositoryType type, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(MESSAGE+type.name(), cause, enableSuppression, writableStackTrace);
    }
}
