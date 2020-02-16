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

package org.apache.archiva.common.utils;

import java.io.IOException;
import java.nio.file.Path;

/**
 * File operation status for a given file.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class FileStatus
{
    final private Path path;
    final private StatusResult result;
    final private IOException exception;

    /**
     * Success status
     *
     * @param path the file path
     * @param statusResult the status of the file operation
     */
    FileStatus( Path path, StatusResult statusResult) {
        this.path = path;
        this.result = statusResult;
        this.exception = null;
    }

    /**
     * Error status
     * @param path  the file path
     * @param e the exception, that occured during the file operation
     */
    FileStatus( Path path, IOException e) {
        this.path = path;
        this.result = StatusResult.ERROR;
        this.exception = e;
    }

    public IOException getException( )
    {
        return exception;
    }

    public Path getPath( )
    {
        return path;
    }

    public StatusResult getResult( )
    {
        return result;
    }
}
