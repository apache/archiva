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
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * Collects information about file system operational status, e.g. if a file could be deleted,
 * or IOException was thrown.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class IOStatus
{

    Map<Path,IOException> errorList;
    Map<Path, StatusResult> okList = new TreeMap<>(  );

    /**
     * Returns <code>true</code>, if no error was recorded.
     * @return
     */
    boolean isOk() {
        return !hasErrors( );
    }

    /**
     * Returns <code>true</code>, if at least one error was recorded
     * @return
     */
    boolean hasErrors() {
        if (errorList==null || errorList.size()==0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Accumulator method used for stream collecting
     *
     * @param ioStatus
     * @param fileStatus
     * @return
     */
    public static IOStatus accumulate(IOStatus ioStatus, FileStatus fileStatus) {
        ioStatus.addStatus( fileStatus );
        return ioStatus;
    }

    /**
     * Combiner used for stream collecting
     * @param ioStatus1
     * @param ioStatus2
     * @return
     */
    public static IOStatus combine(IOStatus ioStatus1, IOStatus ioStatus2) {
        IOStatus status = new IOStatus( );
        status.addAllSuccess( ioStatus1.getSuccessFiles() );
        status.addAllSuccess( ioStatus2.getSuccessFiles( ) );
        status.addAllErrors( ioStatus1.getErrorFiles( ) );
        status.addAllErrors( ioStatus2.getErrorFiles( ) );
        return status;
    }

    /**
     * Add the status for a specific file to this status collection.
     *
     * @param status the status for a given file
     * @return the status object itself
     */
    public IOStatus addStatus(FileStatus status) {
        if (status.getResult()== StatusResult.ERROR) {
            addError( status.getPath( ), status.getException( ) );
        } else {
            addSuccess( status.getPath( ), status.getResult( ) );
        }
        return this;
    }

    /**
     * Adds an error to the status collection.
     *
     * @param path the file path
     * @param e the exception thrown during the file operation
     */
    public void addError( Path path, IOException e) {
        if (errorList==null) {
            errorList = new TreeMap<>( );
        }
        errorList.put( path, e );
    }

    /**
     * Adds multiple errors to the collection.
     *
     * @param errors the map of file, error pairs
     */
    public void addAllErrors(Map<Path, IOException> errors) {
        if (errorList == null) {
            errorList = new TreeMap<>( );
        }
        errorList.putAll( errors );
    }

    /**
     * Adds all successful states to the collection.
     *
     * @param success a map of file, StatusResult pairs
     */
    public void addAllSuccess( Map<Path, StatusResult> success) {
        okList.putAll( success );
    }

    /**
     * Add success status for a given file to the collection.
     *
     * @param path the file path
     * @param status the status of the file operation, e.g. DELETED
     */
    public void addSuccess( Path path, StatusResult status) {
        okList.put( path, status );
    }

    /**
     * Returns all the recorded errors as map of path, exception pairs.
     * @return the map of path, exception pairs.
     */
    public Map<Path, IOException> getErrorFiles() {
        if (errorList==null) {
            return Collections.emptyMap( );
        }
        return errorList;
    }

    /**
     * Returns all the recorded successful operations.
     *
     * @return the map of path, StatusResult pairs
     */
    public Map<Path, StatusResult> getSuccessFiles() {
        if (okList==null) {
            return Collections.emptyMap( );
        }
        return okList;
    }
}
