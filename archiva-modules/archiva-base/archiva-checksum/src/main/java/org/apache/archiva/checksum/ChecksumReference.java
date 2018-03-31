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

import java.nio.file.Path;

/**
 *
 * Simple POJO used to represent a one-to-one relationship between a reference file and
 * a checksum file. The checksum file represents a certain algorithm.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class ChecksumReference
{
    private ChecksummedFile file;
    private Path checksumFile;
    private ChecksumAlgorithm algorithm;


    ChecksumReference( ChecksummedFile file, ChecksumAlgorithm algo, Path checksumFile )
    {
        this.file = file;
        this.algorithm = algo;
    }

    public ChecksummedFile getFile( )
    {
        return file;
    }

    public void setFile( ChecksummedFile file )
    {
        this.file = file;
    }

    public ChecksumAlgorithm getAlgorithm( )
    {
        return algorithm;
    }

    public void setAlgorithm( ChecksumAlgorithm algorithm )
    {
        this.algorithm = algorithm;
    }

    public Path getChecksumFile( )
    {
        return checksumFile;
    }

    public void setChecksumFile( Path checksumFile )
    {
        this.checksumFile = checksumFile;
    }
}
