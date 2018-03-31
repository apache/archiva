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


import org.apache.commons.io.FilenameUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enumeration of available ChecksumAlgorithm techniques.
 *
 *
 */
public enum ChecksumAlgorithm {
    MD5("MD5", "MD5", "md5"),
    SHA1("SHA-1", "SHA1", "sha1", "sha128", "sha-128"),
    SHA256("SHA-256", "SHA2", "sha2", "sha256", "sha-256"),
    SHA384("SHA-384", "SHA3", "sha3", "sha384", "sha-384"),
    SHA512("SHA-512", "SHA5", "sha5", "sha512", "sha-512");

    public static ChecksumAlgorithm getByExtension( Path file )
    {
        String ext = FilenameUtils.getExtension( file.getFileName().toString() ).toLowerCase();
        if (extensionMap.containsKey(ext)) {
            return extensionMap.get(ext);
        }
        throw new IllegalArgumentException( "Filename " + file.getFileName() + " has no valid extension." );
    }

    private static final Map<String, ChecksumAlgorithm> extensionMap = new HashMap<>(  );

    static {
        for (ChecksumAlgorithm alg : ChecksumAlgorithm.values()) {
            for (String extString : alg.getExt())
            {
                extensionMap.put( extString, alg );
            }
        }
    }

    public static Set<String> getExtensions() {
        return extensionMap.keySet();
    }

    /**
     * The MessageDigest algorithm for this hash.
     */
    private final String algorithm;

    /**
     * The file extension for this ChecksumAlgorithm.
     */
    private final List<String> ext;

    /**
     * The checksum type, the key that you see in checksum files.
     */
    private final String type;

    /**
     * Construct a ChecksumAlgorithm
     * 
     * @param algorithm the MessageDigest algorithm
     * @param ext the file extension.
     * @param type the checksum type.
     */
    private ChecksumAlgorithm( String algorithm, String type, String... ext )
    {
        this.algorithm = algorithm;
        this.ext = Arrays.asList( ext );
        this.type = type;

    }

    public String getAlgorithm()
    {
        return algorithm;
    }

    public List<String> getExt()
    {
        return ext;
    }

    public String getType()
    {
        return type;
    }
    
    
}
