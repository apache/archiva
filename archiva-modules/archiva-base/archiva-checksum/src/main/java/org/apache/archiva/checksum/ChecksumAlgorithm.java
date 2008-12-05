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

import java.io.File;

import org.apache.commons.io.FilenameUtils;

/**
 * Enumeration of available ChecksumAlgorithm techniques.
 *
 * @version $Id$
 */
public enum ChecksumAlgorithm {
    SHA1("SHA-1", "sha1", "SHA1"),
    MD5("MD5", "md5", "MD5");

    public static ChecksumAlgorithm getByExtension( File file )
    {
        String ext = FilenameUtils.getExtension( file.getName() ).toLowerCase();
        if ( ChecksumAlgorithm.SHA1.getExt().equals( ext ) )
        {
            return ChecksumAlgorithm.SHA1;
        }
        else if ( ChecksumAlgorithm.MD5.getExt().equals( ext ) )
        {
            return ChecksumAlgorithm.MD5;
        }

        throw new IllegalArgumentException( "Filename " + file.getName() + " has no associated extension." );
    }

    /**
     * The MessageDigest algorithm for this hash.
     */
    private String algorithm;

    /**
     * The file extension for this ChecksumAlgorithm.
     */
    private String ext;

    /**
     * The checksum type, the key that you see in checksum files.
     */
    private String type;

    /**
     * Construct a ChecksumAlgorithm
     * 
     * @param algorithm the MessageDigest algorithm
     * @param ext the file extension.
     * @param type the checksum type.
     */
    private ChecksumAlgorithm( String algorithm, String ext, String type )
    {
        this.algorithm = algorithm;
        this.ext = ext;
        this.type = type;
    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public String getExt()
    {
        return ext;
    }

    public String getType()
    {
        return type;
    }
    
    
}
