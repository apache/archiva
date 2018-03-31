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
 * Simple POJO for storing the data parsed from a checksum file.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ChecksumFileContent
{
    String checksum;
    String fileReference;
    boolean formatMatch = false;

    public ChecksumFileContent() {
    }

    public ChecksumFileContent(String checksum, String fileReference, boolean formatMatch) {
        this.checksum = checksum;
        this.fileReference = fileReference;
        this.formatMatch = formatMatch;
    }

    /**
     * The checksum as hex string.
     *
     * @return
     */
    public String getChecksum( )
    {
        return checksum;
    }

    public void setChecksum( String checksum )
    {
        this.checksum = checksum;
    }

    /**
     * The name of the reference file as stored in the checksum file.
     * @return
     */
    public String getFileReference( )
    {
        return fileReference;
    }

    public void setFileReference( String fileReference )
    {
        this.fileReference = fileReference;
    }

    /**
     * True, if the file content matches a known format
     * @return
     */
    public boolean isFormatMatch( )
    {
        return formatMatch;
    }

    public void setFormatMatch( boolean formatMatch )
    {
        this.formatMatch = formatMatch;
    }
}
